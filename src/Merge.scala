package perfectcuboid

import cats.effect.IO
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}

/** Joining the files of hunts over separate ranges, shards, into one file, as if a single hunt
  * had searched the whole range. Shards let one range be searched by several processes or
  * machines at once, or on Scala Native, which keeps to one thread per process.
  *
  * Nothing is searched or checked again here; `verify` does that on the result.
  */
object Merge:

  /** One shard: its file, the first key it covers and how far it has got. */
  final case class Shard(name: String, from: Long, searchedUpTo: Long)

  /** Why these shards cannot be joined, if they cannot. In order of where they start, each must
    * have searched something and begin just after the one before it ends: no gaps, no overlaps.
    */
  def problems(shards: List[Shard]): List[String] =
    val sorted = shards.sortBy(_.from)
    val empty = sorted.filter(s => s.searchedUpTo < s.from).map(s => s"${s.name} has no checkpoint yet")
    val joins = sorted
      .zip(sorted.drop(1))
      .collect:
        case (before, after) if after.from != before.searchedUpTo + 1 =>
          val how = if after.from <= before.searchedUpTo then "overlaps" else "leaves a gap after"
          f"${after.name}, from ${after.from}%,d, $how ${before.name}, which ends at ${before.searchedUpTo}%,d"
    empty ++ joins

  /** Writes the shards at `inputs` into a new file at `out`, and says what it did. */
  def run[A](quarry: Quarry[A], out: Path, inputs: List[Path]): IO[List[String]] =
    Files[IO]
      .exists(out)
      .flatMap: exists =>
        if exists then IO.pure(List(s"$out exists already: merge only writes new files"))
        else
          inputs
            .traverse(path => Hunt.readResume(quarry, path).map(resume => (path = path, resume = resume)))
            .flatMap: read =>
              val shards = read.sortBy(_.resume.from)
              val found = problems(shards.map(s => Shard(s.path.toString, s.resume.from, s.resume.searchedUpTo)))
              if found.nonEmpty then IO.pure("Cannot merge:" :: found.map("  " + _))
              else write(quarry, out, shards).as(report(quarry, out, shards.map(_.resume)))

  /** Each shard's bytes up to its last checkpoint, without its start line; one start line, if
    * the whole range does not start at 1, goes first. Anything after a shard's last checkpoint
    * belongs to an unfinished block, and is left out.
    */
  private def write[A](quarry: Quarry[A], out: Path, shards: List[(path: Path, resume: HuntFile.Resume)]): IO[Unit] =
    val from = shards.head.resume.from
    val start = Stream.emit(HuntFile.startLine(quarry, from)).filter(_ => from > 1).through(fs2.text.utf8.encode)
    val bodies = Stream
      .emits(shards)
      .flatMap: (path, resume) =>
        val skip = if resume.from > 1 then HuntFile.startLine(quarry, resume.from).getBytes("UTF-8").length else 0
        Files[IO].readRange(path, 64 * 1024, skip.toLong, resume.validBytes)
    (start ++ bodies).through(Files[IO].writeAll(out)).compile.drain

  private def report(quarry: Quarry[?], out: Path, resumes: List[HuntFile.Resume]): List[String] =
    val found = resumes.foldMap(_.found)
    List(
      f"Merged ${resumes.size} files into $out: ${quarry.key}s from ${resumes.head.from}%,d to ${resumes.last.searchedUpTo}%,d,",
      f"  ${found.found}%,d ${quarry.name}. `verify` checks the result."
    )
