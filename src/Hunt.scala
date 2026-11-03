package perfectcuboid

import cats.effect.{IO, Outcome, Ref}
import cats.syntax.all.*
import fs2.{Chunk, Stream}
import fs2.io.file.{Files, Flag, Flags, Path}
import scala.concurrent.duration.*

/** The format of a hunt's results file, as pure functions: no files are touched here.
  *
  * Each block of keys (smallest edges, for Euler bricks) is written as what it found, one
  * per line, followed by a checkpoint line saying how far the search has got:
  *
  *   44 117 240
  *   85 132 720
  *   # searched smallest edges up to 100
  */
object HuntFile:

  def checkpointPrefix(quarry: Quarry[?]): String = s"# searched ${quarry.key}s up to "

  def blockText[A](quarry: Quarry[A], finds: List[A], searchedUpTo: Long): String =
    (finds.map(quarry.line) :+ s"${checkpointPrefix(quarry)}$searchedUpTo").map(_ + "\n").mkString

  /** What reading a results file tells us. `validBytes` is where the last complete checkpoint
    * ends: anything after it belongs to a block that was not finished, and is thrown away.
    * `found` sums up the finds before that point, `sinceCheckpoint` those after it.
    */
  final case class Resume(
      searchedUpTo: Long,
      found: Summary,
      validBytes: Long,
      sinceCheckpoint: Summary,
      bytesRead: Long
  )

  object Resume:
    val Fresh: Resume = Resume(0, Summary.empty, 0, Summary.empty, 0)

  /** Takes one line of the file into account. A line only counts once it is known to be
    * complete, i.e. followed by a newline: a checkpoint cut off half-way would give the wrong number.
    */
  def step[A](quarry: Quarry[A])(state: Resume, line: String, complete: Boolean): Resume =
    val bytesRead = state.bytesRead + line.getBytes("UTF-8").length + (if complete then 1 else 0)
    val prefix = checkpointPrefix(quarry)
    if !complete then state.copy(bytesRead = bytesRead)
    else if line.startsWith(prefix) then
      line.stripPrefix(prefix).toLongOption match
        case Some(n) => Resume(n, state.found |+| state.sinceCheckpoint, bytesRead, Summary.empty, bytesRead)
        case None    => state.copy(bytesRead = bytesRead)
    else
      quarry.parse(line) match
        case Some(find) =>
          state.copy(sinceCheckpoint = state.sinceCheckpoint |+| quarry.summarize(find), bytesRead = bytesRead)
        case None => state.copy(bytesRead = bytesRead)

  /** The finds recorded in a results file, for tests and later analysis. */
  def parse[A](quarry: Quarry[A], lines: List[String]): List[A] =
    lines.filterNot(_.startsWith("#")).flatMap(quarry.parse)

/** One block of a parallel search: the last key in it, what it found, and their summary. */
final case class Block[A](last: Long, finds: Chunk[A], summary: Summary)

object Blocks:

  /** The blocks of keys from `from` to `upTo`, searched on `cores` cores at once by
    * `parEvalMap`, which hands them on in their original order. Each block's finds go straight
    * from the search's iterator into one `Chunk`, an array-backed piece of a stream, and, if
    * `summarize`, are summed up on the core that found them.
    */
  def stream[A](
      quarry: Quarry[A],
      from: Long,
      upTo: Long,
      blockSize: Long,
      cores: Int,
      summarize: Boolean = true
  ): Stream[IO, Block[A]] =
    Stream
      .iterate(from)(_ + blockSize)
      .takeWhile(_ <= upTo)
      .map(start => (start, math.min(start + blockSize - 1, upTo)))
      .parEvalMap(cores): (first, last) =>
        // A block can take seconds of pure computation. On Cats Effect's own few threads that
        // would keep everything else waiting, progress reports included, so it runs on the
        // pool meant for long, blocking work.
        IO.blocking:
          val finds = Chunk.from(quarry.search(first, last).toVector)
          val summary =
            if summarize then finds.foldLeft(Summary.empty)((summary, find) => summary |+| quarry.summarize(find))
            else Summary.empty
          Block(last, finds, summary)

  /** The finds alone, one by one, from blocks of 2000 keys on every core. */
  def finds[A](quarry: Quarry[A], from: Long, upTo: Long): Stream[IO, A] =
    stream(quarry, from, upTo, 2000, Runtime.getRuntime.availableProcessors, summarize = false)
      .flatMap(block => Stream.chunk(block.finds))

/** A long search, for any kind of cuboid, that writes to a file, can be stopped at any time with Ctrl+C,
  * and resumes where it left off when run again.
  *
  * Everything here is a description of a program, an `IO` value: building it does nothing.
  * Only the runtime in `Main` actually runs it. That is what keeps it pure.
  */
object Hunt:

  final case class Progress(searchedUpTo: Long, found: Summary)

  /** Reads an existing results file, line by line in constant memory, to find where to resume. */
  def readResume[A](quarry: Quarry[A], path: Path): IO[HuntFile.Resume] =
    Files[IO]
      .exists(path)
      .flatMap: exists =>
        if !exists then IO.pure(HuntFile.Resume.Fresh)
        else
          Files[IO]
            .readUtf8Lines(path)
            .zipWithNext
            // A line followed by another was ended by a newline; the very last one was not.
            .fold(HuntFile.Resume.Fresh)((state, pair) => HuntFile.step(quarry)(state, pair._1, pair._2.isDefined))
            .compile
            .lastOrError

  /** Cuts off anything after the last complete checkpoint. (Careful: `Flags.Write` would
    * also empty the file on opening, so only the bare `Write` flag is used.)
    */
  def dropUnfinishedBlock(path: Path, resume: HuntFile.Resume): IO[Unit] =
    Files[IO].open(path, Flags(Flag.Write)).use(handle => handle.truncate(resume.validBytes))

  def run[A](
      quarry: Quarry[A],
      path: Path,
      upTo: Long,
      blockSize: Long = 10_000,
      cores: Int = Runtime.getRuntime.availableProcessors
  ): IO[Unit] =
    for
      resume <- readResume(quarry, path)
      _ <- dropUnfinishedBlock(path, resume).whenA(resume.bytesRead > resume.validBytes)
      _ <- IO.println(
        if resume.searchedUpTo == 0 then s"Hunting for ${quarry.name}, writing to $path"
        else s"Resuming after ${quarry.key} ${resume.searchedUpTo} (${resume.found.found} ${quarry.name} so far)"
      )
      progress <- Ref.of[IO, Progress](Progress(resume.searchedUpTo, resume.found))
      started <- IO.monotonic
      _ <- search(quarry, path, resume.searchedUpTo + 1, upTo, blockSize, cores, progress)
        .concurrently(report(quarry, progress, upTo, started))
        .compile
        .drain
        .guaranteeCase: outcome =>
          for
            p <- progress.get
            now <- IO.monotonic
            _ <- IO.println(summary(quarry, outcome, p, (now - started).toUnit(SECONDS)))
          yield ()
    yield ()

  /** The blocks come in order, so each is written after the one before it. Their summaries,
    * made on the cores that searched them, are combined with `|+|` as they are written.
    */
  private def search[A](
      quarry: Quarry[A],
      path: Path,
      from: Long,
      upTo: Long,
      blockSize: Long,
      cores: Int,
      progress: Ref[IO, Progress]
  ): Stream[IO, Unit] =
    val blocks = Blocks.stream(quarry, from, upTo, blockSize, cores)
    Stream
      .resource(Files[IO].writeCursor(path, Flags.Append))
      .flatMap: cursor =>
        // A write cursor remembers its position, and each write gives back the moved cursor.
        blocks.evalScan(cursor): (cursor, block) =>
          for
            moved <- cursor.write(
              Chunk.array(HuntFile.blockText(quarry, block.finds.toList, block.last).getBytes("UTF-8"))
            )
            _ <- progress.update(p => Progress(block.last, p.found |+| block.summary))
            _ <- block.finds
              .filter(quarry.summarize(_).perfect > 0)
              .traverse_(find => IO.println(s"PERFECT CUBOID FOUND: ${quarry.line(find)}"))
          yield moved
      .void

  /** Every few seconds, says how far the search has got. Runs alongside the search. */
  /** Every few seconds, says how far the search has got, how fast it is going over the last
    * minute, and how long it may take yet. Runs alongside the search. The recent samples are
    * carried from one report to the next by `evalScan`.
    */
  private def report(
      quarry: Quarry[?],
      progress: Ref[IO, Progress],
      upTo: Long,
      started: FiniteDuration
  ): Stream[IO, Unit] =
    val first = progress.get.map(p => Vector(Eta.Sample(0, p.searchedUpTo)))
    Stream
      .eval(first)
      .flatMap: start =>
        Stream
          .awakeEvery[IO](5.seconds)
          .evalScan(start): (samples, _) =>
            for
              p <- progress.get
              now <- IO.monotonic
              recent = Eta.window(samples, Eta.Sample((now - started).toUnit(SECONDS), p.searchedUpTo))
              _ <- IO.println(Eta.line(p.searchedUpTo, upTo, p.found.found, quarry.name, Eta.rate(recent)))
            yield recent
      .void

  private def summary(quarry: Quarry[?], outcome: Outcome[IO, Throwable, Unit], p: Progress, seconds: Double): String =
    val headline = outcome match
      case Outcome.Succeeded(_) =>
        f"Done: searched every ${quarry.key} up to ${p.searchedUpTo}%,d, in ${Eta.duration(seconds)}."
      case Outcome.Canceled() =>
        f"Stopped after ${quarry.key} ${p.searchedUpTo}%,d. Run the same command again to resume."
      case Outcome.Errored(e) => s"Failed after ${quarry.key} ${p.searchedUpTo}: $e. Run again to resume."
    (headline :: p.found.describe(quarry.name).map("  " + _)).mkString("\n")
