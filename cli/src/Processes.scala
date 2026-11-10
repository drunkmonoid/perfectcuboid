package perfectcuboid

import cats.effect.IO
import cats.syntax.all.*
import fs2.io.file.{CopyFlag, CopyFlags, Files, Path}

/** A hunt split between processes on one machine: `hunt FILE UPTO --processes N`.
  *
  * The range still to search is cut into N shards, one after another, and each is searched by
  * a process of its own, this same program started again, into a file next to FILE. When they
  * have all finished, the shards are merged with what FILE already held, and the result
  * replaces FILE. Stopping loses nothing: the same command, run again, plans the same shards,
  * and each process resumes its own.
  *
  * Processes, not threads, are what the native build needs: on Scala Native, several threads
  * corrupt memory (RESULTS.md), but several processes are as safe as one.
  */
object Processes:

  /** One process's share: its number, the keys it covers, and its file. */
  final case class Shard(number: Int, from: Long, to: Long, path: Path)

  /** The keys from `first` to `upTo` cut into at most `n` runs one after another, as equal as
    * they can be, the longer ones first.
    */
  def plan(first: Long, upTo: Long, n: Int): List[(from: Long, to: Long)] =
    val size = upTo - first + 1
    val count = math.min(n.toLong, size).toInt
    if count <= 0 then Nil
    else
      val (each, extra) = (size / count, size % count)
      (0 until count).toList
        .scanLeft(first)((start, k) => start + each + (if k < extra then 1 else 0))
        .zip((0 until count).toList)
        .map((start, k) => (from = start, to = start + each - 1 + (if k < extra then 1 else 0)))

  /** Where shard k of FILE goes, and where its process writes what it prints. */
  def shardPath(file: Path, k: Int): Path = Path(s"$file.part$k")
  def logPath(file: Path, k: Int): Path = Path(s"$file.part$k.log")

  /** How to start this program again: its command line up to the word `hunt`, as in
    * `java -Xmx1g -jar perfectcuboid.jar` or `./perfectcuboid`. None if there is no `hunt`.
    */
  def prefix(commandLine: List[String]): Option[List[String]] =
    val before = commandLine.takeWhile(_ != "hunt")
    Option.when(before.size < commandLine.size && before.nonEmpty)(before)

  /** This process's command line, from Linux's /proc. Read to the end of the stream: files in
    * /proc say their size is 0, and Scala Native's `readAllBytes` believes them.
    */
  def ownCommandLine: IO[List[String]] =
    IO.blocking:
      val in = java.nio.file.Files.newInputStream(java.nio.file.Path.of("/proc/self/cmdline"))
      try
        val out = java.io.ByteArrayOutputStream()
        val buffer = new Array[Byte](4096)
        var n = in.read(buffer)
        while n >= 0 do
          out.write(buffer, 0, n)
          n = in.read(buffer)
        out.toString("UTF-8").split('\u0000').toList.filter(_.nonEmpty)
      finally in.close()

  /** The words that run a shard's hunt, after the program's own command. */
  def huntArgs(kind: String, shard: Shard, threads: Int, blockSize: Long): List[String] =
    List("hunt", shard.path.toString, shard.to.toString, "--from", shard.from.toString) ++
      List("--kind", kind, "--threads", threads.toString, "--block-size", blockSize.toString)

  /** The variable that tells a shard's process which process started it. */
  val ParentVariable: String = "PERFECTCUBOID_PARENT"

  /** This process's number, from Linux's /proc. */
  def ownPid: IO[String] =
    IO.blocking(java.nio.file.Files.readSymbolicLink(java.nio.file.Path.of("/proc/self")).toString)

  /** Completes once the process `pid` has gone, looking every two seconds. */
  def gone(pid: String): IO[Unit] =
    IO.blocking(java.nio.file.Files.exists(java.nio.file.Path.of(s"/proc/$pid")))
      .flatMap(alive => if alive then IO.sleep(scala.concurrent.duration.Duration(2, "s")) >> gone(pid) else IO.unit)

  /** A shard's hunt, stopped cleanly, as by Ctrl+C, if the process that started it goes away:
    * a signal to that process alone, or its crash, must not leave shards running, which a
    * second run would then write over. Without `ParentVariable`, just the hunt.
    *
    * Not on Scala Native: the watching needs a second thread, and beside the search's, that
    * crashes the native garbage collector (RESULTS.md). Native shards stop on the signal that
    * stops their parent when it goes to the whole process group, as Ctrl+C's does.
    */
  def stoppedWithParent(hunt: IO[Unit]): IO[Unit] =
    sys.env.get(ParentVariable) match
      case Some(pid) if !Platform.isNative => IO.race(hunt, gone(pid)).void
      case _                               => hunt

  /** Starts `command` as a process writing to `log`, and waits for its exit code. Cancelled, it
    * stops the process, which loses at most the block it was searching; and the process stops
    * by itself if this one goes (`stoppedWithParent`).
    */
  def launch(command: List[String], log: Path): IO[Int] =
    ownPid
      .flatMap: pid =>
        IO.blocking:
          val builder = new ProcessBuilder(command*)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(log.toNioPath.toFile))
          builder.environment().put(ParentVariable, pid): Unit
          builder.start()
      .flatMap: process =>
        // Looking every half second, not blocking in waitFor, so that cancelling never waits on
        // a thread to be interrupted.
        def exited: IO[Int] =
          IO.blocking(process.isAlive)
            .flatMap: alive =>
              if alive then IO.sleep(scala.concurrent.duration.Duration(500, "ms")) >> exited
              else IO.blocking(process.exitValue())
        exited.onCancel(IO.blocking(process.destroy()))

  /** The name `--kind` takes for this kind. */
  def kindName(quarry: Quarry[?]): String = Quarry.byName.collectFirst { case (name, q) if q eq quarry => name }.get

  /** The hunt to `upTo` on `n` processes, each shard run by `run`, which gives an exit code. */
  def hunt[A](
      quarry: Quarry[A],
      file: Path,
      upTo: Long,
      n: Int,
      from: Option[Long],
      run: Shard => IO[Int]
  ): IO[List[String]] =
    Hunt
      .readResume(quarry, file)
      .flatMap: found =>
        val first = if found.isEmpty then from.getOrElse(1L) else found.searchedUpTo + 1
        val shards = plan(first, upTo, n).zipWithIndex.map((r, k) => Shard(k, r.from, r.to, shardPath(file, k)))
        if shards.isEmpty then IO.pure(List(f"$file is searched up to ${found.searchedUpTo}%,d already"))
        else
          shards
            .traverse(s => Hunt.readResume(quarry, s.path).map(r => (shard = s, resume = r)))
            .flatMap: existing =>
              val wrong = existing.filter(e => !e.resume.isEmpty && e.resume.from != e.shard.from)
              if wrong.nonEmpty then
                IO.pure(
                  ("These shards are from a different split, so they cannot be resumed:" ::
                    wrong.map(e => f"  ${e.shard.path} starts at ${e.resume.from}%,d, not ${e.shard.from}%,d")) :+
                    "Run with the --processes of before, or merge them by hand."
                )
              else
                IO.println(
                  f"Searching ${quarry.key}s from $first%,d to $upTo%,d on ${shards.size} processes; " +
                    s"each writes its progress to $file.partN.log"
                ) >> shards
                  .parTraverse(s => run(s).map(code => (shard = s, code = code)))
                  .flatMap: results =>
                    val failed = results.filter(_.code != 0)
                    if failed.nonEmpty then
                      IO.pure(failed.map(r => s"Shard ${r.shard.number} stopped with code ${r.code}: see its log"))
                    else join(quarry, file, found, shards)

  /** Merges FILE, if it has anything, and the shards into FILE, and removes the shards. */
  private def join[A](quarry: Quarry[A], file: Path, found: HuntFile.Resume, shards: List[Shard]): IO[List[String]] =
    val merged = Path(s"$file.merging")
    val inputs = (if found.isEmpty || found.searchedUpTo < found.from then Nil else List(file)) ++ shards.map(_.path)
    Files[IO].deleteIfExists(merged) >> Merge
      .run(quarry, merged, inputs)
      .flatMap: report =>
        Files[IO]
          .exists(merged)
          .flatMap: done =>
            if !done then IO.pure(report)
            else
              Files[IO].move(merged, file, CopyFlags(CopyFlag.ReplaceExisting, CopyFlag.AtomicMove)) >>
                shards.traverse_(s =>
                  Files[IO].deleteIfExists(s.path) >> Files[IO].deleteIfExists(logPath(file, s.number))
                ) >>
                IO.pure(report.map(_.replace(merged.toString, file.toString)))

  /** The real thing: each shard by this same program, started again. */
  def huntOnProcesses[A](
      quarry: Quarry[A],
      file: Path,
      upTo: Long,
      n: Int,
      threadsEach: Int,
      blockSize: Long,
      from: Option[Long]
  ): IO[List[String]] =
    ownCommandLine
      .map(prefix)
      .flatMap:
        case None          => IO.pure(List("Cannot tell how this program was started, to start it again"))
        case Some(command) =>
          val kind = kindName(quarry)
          hunt(
            quarry,
            file,
            upTo,
            n,
            from,
            s => launch(command ++ huntArgs(kind, s, threadsEach, blockSize), logPath(file, s.number))
          )
