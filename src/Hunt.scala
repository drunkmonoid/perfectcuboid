package perfectcuboid

import cats.effect.{IO, Outcome, Ref}
import cats.syntax.all.*
import fs2.{Chunk, Stream}
import fs2.io.file.{Files, Flag, Flags, Path}
import scala.concurrent.duration.*

/** The format of a hunt's results file, as pure functions: no files are touched here.
  *
  * Each block of smallest edges is written as its bricks, one per line, followed by a
  * checkpoint line saying how far the search has got:
  *
  *   44 117 240
  *   85 132 720
  *   # searched smallest edges up to 100
  */
object HuntFile:

  val CheckpointPrefix = "# searched smallest edges up to "

  def blockText(bricks: List[Cuboid], searchedUpTo: Long): String =
    val brickLines = bricks.map(c => s"${c.a} ${c.b} ${c.c}" + (if c.isPerfect then "  PERFECT" else ""))
    (brickLines :+ s"$CheckpointPrefix$searchedUpTo").map(_ + "\n").mkString

  /** What reading a results file tells us. `validBytes` is where the last complete checkpoint
    * ends: anything after it belongs to a block that was not finished, and is thrown away.
    */
  final case class Resume(
      searchedUpTo: Long,
      bricks: Long,
      validBytes: Long,
      bricksSinceCheckpoint: Long,
      bytesRead: Long
  )

  object Resume:
    val Fresh: Resume = Resume(0, 0, 0, 0, 0)

  /** Takes one line of the file into account. A line only counts once it is known to be
    * complete, i.e. followed by a newline: a checkpoint cut off half-way would give the wrong number.
    */
  def step(state: Resume, line: String, complete: Boolean): Resume =
    val bytesRead = state.bytesRead + line.length + (if complete then 1 else 0)
    if !complete then state.copy(bytesRead = bytesRead)
    else if line.startsWith(CheckpointPrefix) then
      line.stripPrefix(CheckpointPrefix).toLongOption match
        case Some(n) =>
          Resume(n, state.bricks + state.bricksSinceCheckpoint, bytesRead, 0, bytesRead)
        case None => state.copy(bytesRead = bytesRead)
    else if line.nonEmpty then
      state.copy(bricksSinceCheckpoint = state.bricksSinceCheckpoint + 1, bytesRead = bytesRead)
    else state.copy(bytesRead = bytesRead)

  /** The bricks recorded in a results file, for tests and later analysis. */
  def parseBricks(lines: List[String]): List[(Long, Long, Long)] =
    lines
      .filterNot(_.startsWith("#"))
      .map(_.split(" +").toList)
      .collect:
        case a :: b :: c :: _ => (a.toLong, b.toLong, c.toLong)

/** A long Euler brick search that writes to a file, can be stopped at any time with Ctrl+C,
  * and resumes where it left off when run again.
  *
  * Everything here is a description of a program, an `IO` value: building it does nothing.
  * Only the runtime in `Main` actually runs it. That is what keeps it pure.
  */
object Hunt:

  final case class Progress(searchedUpTo: Long, bricks: Long)

  /** Reads an existing results file, line by line in constant memory, to find where to resume. */
  def readResume(path: Path): IO[HuntFile.Resume] =
    Files[IO]
      .exists(path)
      .flatMap: exists =>
        if !exists then IO.pure(HuntFile.Resume.Fresh)
        else
          Files[IO]
            .readUtf8Lines(path)
            .zipWithNext
            // A line followed by another was ended by a newline; the very last one was not.
            .fold(HuntFile.Resume.Fresh)((state, pair) => HuntFile.step(state, pair._1, pair._2.isDefined))
            .compile
            .lastOrError

  /** Cuts off anything after the last complete checkpoint. (Careful: `Flags.Write` would
    * also empty the file on opening, so only the bare `Write` flag is used.)
    */
  def dropUnfinishedBlock(path: Path, resume: HuntFile.Resume): IO[Unit] =
    Files[IO].open(path, Flags(Flag.Write)).use(handle => handle.truncate(resume.validBytes))

  def run(
      path: Path,
      upTo: Long,
      blockSize: Long = 10_000,
      cores: Int = Runtime.getRuntime.availableProcessors
  ): IO[Unit] =
    for
      resume <- readResume(path)
      _ <- dropUnfinishedBlock(path, resume).whenA(resume.bytesRead > resume.validBytes)
      _ <- IO.println(
        if resume.searchedUpTo == 0 then s"Hunting for Euler bricks, writing to $path"
        else s"Resuming after smallest edge ${resume.searchedUpTo} (${resume.bricks} bricks so far)"
      )
      progress <- Ref.of[IO, Progress](Progress(resume.searchedUpTo, resume.bricks))
      started <- IO.monotonic
      _ <- search(path, resume.searchedUpTo + 1, upTo, blockSize, cores, progress)
        .concurrently(report(progress, resume.searchedUpTo, started))
        .compile
        .drain
        .guaranteeCase(outcome => progress.get.flatMap(p => IO.println(summary(outcome, p))))
    yield ()

  /** Blocks of edges are searched on every core at once by `parEvalMap`, which hands back
    * the results in their original order, so each block is written after the one before it.
    */
  private def search(
      path: Path,
      from: Long,
      upTo: Long,
      blockSize: Long,
      cores: Int,
      progress: Ref[IO, Progress]
  ): Stream[IO, Unit] =
    val blocks = Stream
      .iterate(from)(_ + blockSize)
      .takeWhile(_ <= upTo)
      .map(start => (start, math.min(start + blockSize - 1, upTo)))
      .parEvalMap(cores)((first, last) => IO(last -> FastSearch.eulerBricksBetween(first, last)))
    Stream
      .resource(Files[IO].writeCursor(path, Flags.Append))
      .flatMap: cursor =>
        // A write cursor remembers its position, and each write gives back the moved cursor.
        blocks.evalScan(cursor): (cursor, block) =>
          val (last, bricks) = block
          for
            moved <- cursor.write(Chunk.array(HuntFile.blockText(bricks, last).getBytes("UTF-8")))
            _ <- progress.update(p => Progress(last, p.bricks + bricks.size))
            _ <- bricks
              .filter(_.isPerfect)
              .traverse_(c => IO.println(s"PERFECT CUBOID FOUND: ${c.a} × ${c.b} × ${c.c}"))
          yield moved
      .void

  /** Every few seconds, says how far the search has got. Runs alongside the search. */
  private def report(progress: Ref[IO, Progress], startedAt: Long, started: FiniteDuration): Stream[IO, Unit] =
    Stream
      .awakeEvery[IO](5.seconds)
      .evalMap: _ =>
        for
          p <- progress.get
          now <- IO.monotonic
          rate = (p.searchedUpTo - startedAt) / (now - started).toUnit(SECONDS)
          _ <- IO.println(f"  searched up to ${p.searchedUpTo}%,d  (${p.bricks}%,d bricks, $rate%,.0f edges/s)")
        yield ()

  private def summary(outcome: Outcome[IO, Throwable, Unit], p: Progress): String = outcome match
    case Outcome.Succeeded(_) =>
      f"Done: searched every smallest edge up to ${p.searchedUpTo}%,d, ${p.bricks}%,d bricks, none perfect."
    case Outcome.Canceled() =>
      f"Stopped after smallest edge ${p.searchedUpTo}%,d. Run the same command again to resume."
    case Outcome.Errored(e) => s"Failed after smallest edge ${p.searchedUpTo}: $e. Run again to resume."
