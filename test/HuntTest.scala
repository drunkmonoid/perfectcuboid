package perfectcuboid

import cats.effect.IO
import fs2.io.file.{Files, Path}
import scala.concurrent.duration.*

/** A CatsEffectSuite runs tests that return an `IO`. */
class HuntTest extends munit.CatsEffectSuite:

  def expected(upTo: Long): List[(Long, Long, Long)] =
    FastSearch.eulerBricksBetween(1, upTo).map(c => (c.a: Long, c.b: Long, c.c: Long))

  def bricksIn(path: Path): IO[List[(Long, Long, Long)]] =
    Files[IO].readUtf8Lines(path).compile.toList.map(HuntFile.parseBricks)

  def withTempFile[A](body: Path => IO[A]): IO[A] = Files[IO].tempFile.use(body)

  test("a checkpoint only counts once its line is complete"):
    val text = HuntFile.blockText(Cuboid.of(44, 117, 240).toList, 100) + "85 132 720\n# searched smallest edges up to 2"
    val lines = text.split("\n", -1).toList
    val flags = lines.indices.map(_ < lines.size - 1)
    val resume = lines.zip(flags).foldLeft(HuntFile.Resume.Fresh)((s, lf) => HuntFile.step(s, lf._1, lf._2))
    assertEquals(resume.searchedUpTo, 100L)
    assertEquals(resume.found, Summary.of(Cuboid.of(44, 117, 240)))
    assertEquals(resume.validBytes, HuntFile.blockText(Cuboid.of(44, 117, 240).toList, 100).length.toLong)

  test("a hunt records every brick and its progress"):
    withTempFile: path =>
      for
        _ <- Hunt.run(path, 3000, blockSize = 100, cores = 4)
        bricks <- bricksIn(path)
        resume <- Hunt.readResume(path)
      yield
        assertEquals(bricks, expected(3000))
        assertEquals(resume.searchedUpTo, 3000L)
        assertEquals(resume.found, Summary.of(FastSearch.eulerBricksBetween(1, 3000)))

  test("a hunt in two runs gives the same file as one run"):
    withTempFile: path =>
      for
        _ <- Hunt.run(path, 1500, blockSize = 100, cores = 4)
        _ <- Hunt.run(path, 3000, blockSize = 100, cores = 4)
        bricks <- bricksIn(path)
      yield assertEquals(bricks, expected(3000))

  test("stopping a hunt part-way loses nothing"):
    withTempFile: path =>
      val upTo = 300_000L
      for
        _ <- Hunt.run(path, upTo, blockSize = 500, cores = 4).timeoutTo(200.millis, IO.unit)
        stoppedAt <- Hunt.readResume(path)
        _ <- Hunt.run(path, upTo, blockSize = 500, cores = 4)
        bricks <- bricksIn(path)
      yield
        assert(stoppedAt.searchedUpTo > 0 && stoppedAt.searchedUpTo < upTo, stoppedAt)
        assertEquals(bricks, expected(upTo))

  test("a half-written block, as after a crash, is thrown away and searched again"):
    withTempFile: path =>
      val complete = HuntFile.blockText(FastSearch.eulerBricksBetween(1, 100), 100)
      val halfWritten = "85 132 720\n87 41\n# searched smallest edges up to 2"
      for
        _ <- fs2.Stream.emit(complete + halfWritten).through(Files[IO].writeUtf8(path)).compile.drain
        _ <- Hunt.run(path, 3000, blockSize = 100, cores = 4)
        bricks <- bricksIn(path)
      yield assertEquals(bricks, expected(3000))
