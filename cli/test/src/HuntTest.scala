package perfectcuboid

import cats.effect.IO
import fs2.io.file.{Files, Path}
import scala.concurrent.duration.*

/** A CatsEffectSuite runs tests that return an `IO`. */
class HuntTest extends munit.CatsEffectSuite:

  // These run whole hunts, some several times over: beside a long hunt, or the other platforms'
  // tests, they can take longer than munit's usual 30 seconds.
  override val munitIOTimeout: scala.concurrent.duration.Duration = scala.concurrent.duration.Duration(180, "s")

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

  test("the streamed unbounded report prints the same lines as the pure one"):
    Unbounded
      .stream(3000, 4)
      .compile
      .toList
      .map(lines => assertEquals(lines, Cli.run(Report.Unbounded(3000, 3)).toList))

  test("blocks come in order, each with the summary of its own finds"):
    Blocks
      .stream(Quarry.eulerBricks, 1, 5000, 700, 3)
      .compile
      .toList
      .map: blocks =>
        assertEquals(blocks.map(_.last), List(700L, 1400, 2100, 2800, 3500, 4200, 4900, 5000))
        assertEquals(blocks.flatMap(_.finds.toList), FastSearch.eulerBricksBetween(1, 5000))
        blocks.foreach(block => assertEquals(block.summary, Summary.of(block.finds.toList)))

  test("a primitive hunt keeps exactly the primitive bricks"):
    withTempFile: path =>
      for
        _ <- Hunt.run(Quarry.primitiveBricks, path, 20_000, blockSize = 1000, cores = 4)
        lines <- Files[IO].readUtf8Lines(path).compile.toList
        report <- Verify.run(Quarry.primitiveBricks, path, blocks = 3)
      yield
        assertEquals(
          HuntFile.parse(Quarry.primitiveBricks, lines),
          FastSearch.eulerBricksBetween(1, 20_000).filter(_.isPrimitive)
        )
        assertEquals(report.last, "OK", report)

  test("a hunt by odd edge finds what the odd-edge search finds, resumes to the same file, and verifies"):
    def lines(path: Path) = Files[IO].readUtf8Lines(path).compile.toList
    withTempFile: once =>
      withTempFile: twice =>
        for
          _ <- Hunt.run(Quarry.oddEdge, once, 30_000, blockSize = 1000, cores = 4)
          _ <- Hunt.run(Quarry.oddEdge, twice, 12_000, blockSize = 1000, cores = 4)
          _ <- Hunt.run(Quarry.oddEdge, twice, 30_000, blockSize = 1000, cores = 4)
          one <- lines(once)
          two <- lines(twice)
          report <- Verify.run(Quarry.oddEdge, once, blocks = 3)
        yield
          assertEquals(two, one)
          assertEquals(HuntFile.parse(Quarry.oddEdge, one), OddSearch.primitiveBricksIterator(1, 30_000).toList)
          assertEquals(report.last, "OK", report)

  test("a hunt by space diagonal finds the face cuboids the other search finds, and verifies"):
    withTempFile: path =>
      for
        _ <- Hunt.run(Quarry.faceByDiagonal, path, 8_000, blockSize = 1000, cores = 4)
        lines <- Files[IO].readUtf8Lines(path).compile.toList
        report <- Verify.run(Quarry.faceByDiagonal, path, blocks = 2)
      yield
        val (byDiagonal, bySharedEdge) = NearMisses.bothWays(8_000)
        val found = HuntFile.parse(Quarry.faceByDiagonal, lines)
        assertEquals(found, byDiagonal)
        assertEquals(found.toSet, bySharedEdge.toSet)
        assert(found.nonEmpty)
        assertEquals(report.last, "OK", report)

  test("a hunt of parallelepipeds finds what their search finds, resumes to the same file, and verifies"):
    def lines(path: Path) = Files[IO].readUtf8Lines(path).compile.toList
    withTempFile: once =>
      withTempFile: twice =>
        for
          _ <- Hunt.run(Quarry.parallelepipeds, once, 200, blockSize = 50, cores = 4)
          _ <- Hunt.run(Quarry.parallelepipeds, twice, 100, blockSize = 50, cores = 4)
          _ <- Hunt.run(Quarry.parallelepipeds, twice, 200, blockSize = 50, cores = 4)
          one <- lines(once)
          two <- lines(twice)
          report <- Verify.run(Quarry.parallelepipeds, once, blocks = 2)
        yield
          assertEquals(two, one)
          assertEquals(HuntFile.parse(Quarry.parallelepipeds, one), Parallelepiped.search(200))
          assertEquals(report.last, "OK", report)

  test("a new file says what it holds on its first line"):
    withTempFile: path =>
      for
        _ <- Hunt.run(Quarry.primitiveBricks, path, 1000, blockSize = 100, cores = 2)
        lines <- Files[IO].readUtf8Lines(path).compile.toList
        resume <- Hunt.readResume(Quarry.primitiveBricks, path)
      yield
        assertEquals(lines.head, "# perfectcuboid results, format 2, kind primitive")
        assertEquals(resume.kind, Some("primitive"))
        assertEquals(resume.searchedUpTo, 1000L)

  test("a file written before headers still resumes and verifies"):
    withTempFile: path =>
      val old =
        HuntFile.blockText(Quarry.primitiveBricks, FastSearch.eulerBricksBetween(1, 500).filter(_.isPrimitive), 500)
      for
        _ <- fs2.Stream.emit(old).through(Files[IO].writeUtf8(path)).compile.drain
        _ <- Hunt.run(Quarry.primitiveBricks, path, 2000, blockSize = 100, cores = 2)
        lines <- Files[IO].readUtf8Lines(path).compile.toList
        report <- Verify.run(Quarry.primitiveBricks, path, blocks = 3)
      yield
        assert(!lines.head.startsWith("# perfectcuboid"), lines.head)
        assertEquals(
          HuntFile.parse(Quarry.primitiveBricks, lines),
          FastSearch.eulerBricksBetween(1, 2000).filter(_.isPrimitive)
        )
        assertEquals(report.last, "OK", report)

  test("hunt, verify and merge refuse a file of another kind"):
    withTempFile: path =>
      withTempFile: out =>
        for
          _ <- Hunt.run(Quarry.primitiveBricks, path, 1000, blockSize = 100, cores = 2)
          before <- Files[IO].readUtf8(path).compile.string
          _ <- Hunt.run(Quarry.eulerBricks, path, 2000, blockSize = 100, cores = 2)
          after <- Files[IO].readUtf8(path).compile.string
          report <- Verify.run(Quarry.eulerBricks, path, blocks = 3)
          _ <- Files[IO].delete(out)
          merged <- Merge.run(Quarry.eulerBricks, out, List(path))
        yield
          assertEquals(after, before)
          assertEquals(report.last, "PROBLEMS FOUND")
          assert(report.exists(_.contains("holds primitive Euler bricks (--kind primitive)")), report)
          assert(merged.exists(_.contains("holds primitive Euler bricks")), merged)

  test("a hunt can start later, and a later run resumes it from its own start"):
    withTempFile: path =>
      for
        _ <- Hunt.run(Quarry.eulerBricks, path, 2000, blockSize = 100, cores = 4, from = Some(1001))
        _ <- Hunt.run(Quarry.eulerBricks, path, 3000, blockSize = 100, cores = 4)
        _ <- Hunt.run(Quarry.eulerBricks, path, 4000, blockSize = 100, cores = 4, from = Some(5)) // refused
        lines <- Files[IO].readUtf8Lines(path).compile.toList
        resume <- Hunt.readResume(Quarry.eulerBricks, path)
        report <- Verify.run(Quarry.eulerBricks, path, blocks = 3)
      yield
        assertEquals(
          lines.take(2),
          List("# perfectcuboid results, format 2, kind bricks", "# smallest edges from 1001")
        )
        assertEquals(HuntFile.parse(Quarry.eulerBricks, lines), FastSearch.eulerBricksBetween(1001, 3000))
        assertEquals((resume.from, resume.searchedUpTo), (1001L, 3000L))
        assertEquals(report.last, "OK", report)

  test("shards merge into the bricks of one hunt, in a file that verify accepts"):
    Files[IO].tempDirectory.use: dir =>
      val (low, high, all) = (dir / "low.txt", dir / "high.txt", dir / "all.txt")
      for
        _ <- Hunt.run(Quarry.eulerBricks, high, 3000, blockSize = 100, cores = 4, from = Some(1501))
        _ <- Hunt.run(Quarry.eulerBricks, low, 1500, blockSize = 100, cores = 4)
        merged <- Merge.run(Quarry.eulerBricks, all, List(high, low))
        again <- Merge.run(Quarry.eulerBricks, all, List(low, high))
        bricks <- bricksIn(all)
        report <- Verify.run(Quarry.eulerBricks, all, blocks = 3)
      yield
        assert(merged.head.startsWith("Merged 2 files"), merged)
        assert(again.head.contains("exists already"), again)
        assertEquals(bricks, expected(3000))
        assertEquals(report.last, "OK", report)

  test("shards with a gap, an overlap or no checkpoint cannot be merged"):
    import Merge.Shard
    assertEquals(Merge.problems(List(Shard("b", 101, 200), Shard("a", 1, 100))), Nil)
    assert(Merge.problems(List(Shard("a", 1, 100), Shard("b", 102, 200))).head.contains("leaves a gap"))
    assert(Merge.problems(List(Shard("a", 1, 100), Shard("b", 90, 200))).head.contains("overlaps"))
    assert(Merge.problems(List(Shard("a", 1, 100), Shard("b", 101, 100))).head.contains("no checkpoint"))

  test("a hunt past the Long square limit verifies, and other kinds stop at their own limit"):
    withTempFile: path =>
      val first = Search.MaxSmallestEdge - 99
      for
        _ <- Hunt.run(Quarry.eulerBricks, path, first + 299, blockSize = 100, cores = 4, from = Some(first))
        report <- Verify.run(Quarry.eulerBricks, path, blocks = 3)
        resume <- Hunt.readResume(Quarry.eulerBricks, path)
        _ <- Hunt.run(Quarry.faceCuboids, path, Search.MaxSmallestEdge + 1) // refused before reading on
      yield
        assertEquals(resume.searchedUpTo, first + 299)
        assertEquals(report.last, "OK", report)
