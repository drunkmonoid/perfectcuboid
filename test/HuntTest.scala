package perfectcuboid

import cats.effect.IO
import fs2.io.file.{Files, Path}
import scala.concurrent.duration.*

/** A CatsEffectSuite runs tests that return an `IO`. */
class HuntTest extends munit.CatsEffectSuite:

  def expected(upTo: Long): List[(Long, Long, Long)] =
    FastSearch.eulerBricksBetween(1, upTo).map(c => (c.a: Long, c.b: Long, c.c: Long))

  def bricksIn(path: Path): IO[List[(Long, Long, Long)]] =
    Files[IO]
      .readUtf8Lines(path)
      .compile
      .toList
      .map(lines => HuntFile.parse(Quarry.eulerBricks, lines).map(c => (c.a: Long, c.b: Long, c.c: Long)))

  def withTempFile[A](body: Path => IO[A]): IO[A] = Files[IO].tempFile.use(body)

  test("a checkpoint only counts once its line is complete"):
    val text = HuntFile.blockText(
      Quarry.eulerBricks,
      Cuboid.of(44, 117, 240).toList,
      100
    ) + "85 132 720\n# searched smallest edges up to 2"
    val lines = text.split("\n", -1).toList
    val flags = lines.indices.map(_ < lines.size - 1)
    val resume =
      lines.zip(flags).foldLeft(HuntFile.Resume.Fresh)((s, lf) => HuntFile.step(Quarry.eulerBricks)(s, lf._1, lf._2))
    assertEquals(resume.searchedUpTo, 100L)
    assertEquals(resume.found, Summary.of(Cuboid.of(44, 117, 240)))
    assertEquals(
      resume.validBytes,
      HuntFile.blockText(Quarry.eulerBricks, Cuboid.of(44, 117, 240).toList, 100).length.toLong
    )

  test("a hunt records every brick and its progress"):
    withTempFile: path =>
      for
        _ <- Hunt.run(Quarry.eulerBricks, path, 3000, blockSize = 100, cores = 4)
        bricks <- bricksIn(path)
        resume <- Hunt.readResume(Quarry.eulerBricks, path)
      yield
        assertEquals(bricks, expected(3000))
        assertEquals(resume.searchedUpTo, 3000L)
        assertEquals(resume.found, Summary.of(FastSearch.eulerBricksBetween(1, 3000)))

  test("a hunt in two runs gives the same file as one run"):
    withTempFile: path =>
      for
        _ <- Hunt.run(Quarry.eulerBricks, path, 1500, blockSize = 100, cores = 4)
        _ <- Hunt.run(Quarry.eulerBricks, path, 3000, blockSize = 100, cores = 4)
        bricks <- bricksIn(path)
      yield assertEquals(bricks, expected(3000))

  test("stopping a hunt part-way loses nothing"):
    withTempFile: path =>
      val upTo = 300_000L
      for
        _ <- Hunt.run(Quarry.eulerBricks, path, upTo, blockSize = 500, cores = 4).timeoutTo(200.millis, IO.unit)
        stoppedAt <- Hunt.readResume(Quarry.eulerBricks, path)
        _ <- Hunt.run(Quarry.eulerBricks, path, upTo, blockSize = 500, cores = 4)
        bricks <- bricksIn(path)
      yield
        assert(stoppedAt.searchedUpTo > 0 && stoppedAt.searchedUpTo < upTo, stoppedAt)
        assertEquals(bricks, expected(upTo))

  test("a half-written block, as after a crash, is thrown away and searched again"):
    withTempFile: path =>
      val complete = HuntFile.blockText(Quarry.eulerBricks, FastSearch.eulerBricksBetween(1, 100), 100)
      val halfWritten = "85 132 720\n87 41\n# searched smallest edges up to 2"
      for
        _ <- fs2.Stream.emit(complete + halfWritten).through(Files[IO].writeUtf8(path)).compile.drain
        _ <- Hunt.run(Quarry.eulerBricks, path, 3000, blockSize = 100, cores = 4)
        bricks <- bricksIn(path)
      yield assertEquals(bricks, expected(3000))

  test("a face cuboid hunt records what the face cuboid search finds"):
    withTempFile: path =>
      for
        _ <- Hunt.run(Quarry.faceCuboids, path, 1000, blockSize = 100, cores = 4)
        lines <- Files[IO].readUtf8Lines(path).compile.toList
      yield assertEquals(
        HuntFile.parse(Quarry.faceCuboids, lines),
        (1L to 1000L).toList.flatMap(NearMisses.faceCuboidsWithSharedEdge)
      )

  test("an edge cuboid hunt, with its √ signs, resumes to the same file as one run"):
    def lines(path: Path) = Files[IO].readUtf8Lines(path).compile.toList
    withTempFile: once =>
      withTempFile: twice =>
        for
          _ <- Hunt.run(Quarry.edgeCuboids, once, 600_000, blockSize = 50_000, cores = 4)
          _ <- Hunt.run(Quarry.edgeCuboids, twice, 300_000, blockSize = 50_000, cores = 4)
          _ <- Hunt.run(Quarry.edgeCuboids, twice, 600_000, blockSize = 50_000, cores = 4)
          one <- lines(once)
          two <- lines(twice)
          resume <- Hunt.readResume(Quarry.edgeCuboids, twice)
        yield
          assertEquals(two, one)
          assert(one.contains("√461776 1443 1800"), one)
          assertEquals(resume.found.found, 1L)
          assertEquals(resume.found.largest.map(_.text), Some("√461776 × 1443 × 1800"))
