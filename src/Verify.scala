package perfectcuboid

import cats.effect.IO
import cats.syntax.all.*
import fs2.io.file.{Files, Path}

/** Checking a hunt's results file, without trusting the code that wrote it.
  *
  * Every find is checked again with BigInt arithmetic (`Quarry.check`), and the file's
  * structure is checked: checkpoints going up, each find's key after the checkpoint before it
  * and no later than the one after it, finds in order. Then a few blocks, spread evenly
  * through the file, are searched again, to confirm the file holds everything there is.
  */
object Verify:

  /** What reading the file has shown so far. `problems` keeps the first few, and counts all. */
  final case class State(
      lines: Long,
      finds: Long,
      checkpoints: Vector[Long],
      lastKey: Long,
      sinceCheckpoint: Long,
      problems: Vector[String],
      problemCount: Long,
      start: Long = 1
  ):
    def lastCheckpoint: Long = checkpoints.lastOption.getOrElse(start - 1)

    def problem(text: String): State =
      copy(problems = if problems.size < 10 then problems :+ text else problems, problemCount = problemCount + 1)

  object State:
    val Start: State = State(0, 0, Vector.empty, 0, 0, Vector.empty, 0)

  /** Takes one line into account, like `HuntFile.step`. An unfinished last line is left alone:
    * a resumed hunt would throw it away.
    */
  def step[A](quarry: Quarry[A])(state: State, line: String, complete: Boolean): State =
    val next = state.copy(lines = state.lines + 1)
    val prefix = HuntFile.checkpointPrefix(quarry)
    if !complete || line.isEmpty then next
    else if line.startsWith(HuntFile.startPrefix(quarry)) then
      line.stripPrefix(HuntFile.startPrefix(quarry)).toLongOption match
        case Some(n) if n >= 1 && state.lines == 0 => next.copy(start = n)
        case _ => next.problem(s"line ${next.lines}: a start can only be the first line, and at least 1")
    else if line.startsWith(prefix) then
      line.stripPrefix(prefix).toLongOption match
        case Some(n) if n > state.lastCheckpoint && state.lastKey <= n =>
          next.copy(checkpoints = state.checkpoints :+ n, sinceCheckpoint = 0)
        case Some(n) => next.problem(s"line ${next.lines}: checkpoint $n does not cover what came before it")
        case None    => next.problem(s"line ${next.lines}: unreadable checkpoint")
    else
      quarry.parse(line) match
        case None       => next.problem(s"line ${next.lines}: not a ${quarry.name} line: $line")
        case Some(find) =>
          val key = quarry.keyOf(find)
          val counted = next.copy(finds = state.finds + 1, lastKey = key, sinceCheckpoint = state.sinceCheckpoint + 1)
          if !quarry.check(find) then counted.problem(s"line ${next.lines}: not one of the ${quarry.name}: $line")
          else if key <= state.lastCheckpoint then
            counted.problem(s"line ${next.lines}: ${quarry.key} $key is before the checkpoint ${state.lastCheckpoint}")
          else if key < state.lastKey then counted.problem(s"line ${next.lines}: out of order")
          else counted

  /** Up to `count` blocks between consecutive checkpoints, spread evenly through the file, which
    * starts at `start`.
    */
  def blocksToSearch(checkpoints: Vector[Long], count: Int, start: Long = 1): List[(Long, Long)] =
    val blocks = ((start - 1) +: checkpoints).zip(checkpoints).map((before, at) => (before + 1, at))
    if blocks.isEmpty || count <= 0 then Nil
    else (0 until count).map(i => blocks(i * blocks.size / count)).distinct.toList

  private def read[A](quarry: Quarry[A], path: Path): IO[State] =
    Files[IO]
      .readUtf8Lines(path)
      .zipWithNext
      .fold(State.Start) { case (state, (line, next)) => step(quarry)(state, line, next.isDefined) }
      .compile
      .lastOrError

  /** The finds the file records with keys from `first` to `last`. */
  private def recorded[A](quarry: Quarry[A], path: Path, first: Long, last: Long): IO[List[A]] =
    Files[IO]
      .readUtf8Lines(path)
      .map(quarry.parse)
      .unNone
      .filter(find => quarry.keyOf(find) >= first && quarry.keyOf(find) <= last)
      .compile
      .toList

  /** The whole check, as lines to print. */
  def run[A](quarry: Quarry[A], path: Path, blocks: Int): IO[List[String]] =
    for
      state <- read(quarry, path)
      spotChecks <- blocksToSearch(state.checkpoints, blocks, state.start).traverse: (first, last) =>
        recorded(quarry, path, first, last).map(found =>
          (first = first, last = last, same = found == quarry.search(first, last).toList)
        )
    yield
      val missing = spotChecks.filterNot(_.same)
      List(
        s"Verifying $path as ${quarry.name}",
        f"  ${state.lines}%,d lines: ${state.finds}%,d ${quarry.name}, ${state.checkpoints.size}%,d checkpoints, " +
          (if state.start > 1 then f"${quarry.key}s from ${state.start}%,d to ${state.lastCheckpoint}%,d"
           else f"up to ${quarry.key} ${state.lastCheckpoint}%,d"),
        s"  every one re-checked with BigInt, and the checkpoints checked: ${state.problemCount} problems",
        s"  ${spotChecks.size} blocks searched again: " +
          (if missing.isEmpty then "the file has everything they hold" else s"${missing.size} differ")
      ) ++
        Option
          .when(state.sinceCheckpoint > 0)(
            s"  ${state.sinceCheckpoint} lines after the last checkpoint, from an unfinished block"
          )
          .toList ++
        state.problems.map("  " + _) ++
        missing.map((first, last, _) => s"  the block $first to $last differs from a new search") :+
        (if state.problemCount == 0 && missing.isEmpty then "OK" else "PROBLEMS FOUND")
