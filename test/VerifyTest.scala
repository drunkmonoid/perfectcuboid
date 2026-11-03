package perfectcuboid

import cats.effect.IO
import fs2.io.file.{Files, Path}

class VerifyTest extends munit.CatsEffectSuite:

  def withTempFile[A](body: Path => IO[A]): IO[A] = Files[IO].tempFile.use(body)

  def write(path: Path, lines: List[String]): IO[Unit] =
    fs2.Stream.emit(lines.map(_ + "\n").mkString).through(Files[IO].writeUtf8(path)).compile.drain

  def lines(path: Path): IO[List[String]] = Files[IO].readUtf8Lines(path).compile.toList.map(_.filter(_.nonEmpty))

  /** A hunt's file, changed by `tamper`, then verified with every block searched again. */
  def verifyTampered(tamper: List[String] => List[String]): IO[List[String]] =
    withTempFile: path =>
      for
        _ <- Hunt.run(Quarry.eulerBricks, path, 3000, blockSize = 500, cores = 2)
        original <- lines(path)
        _ <- write(path, tamper(original))
        report <- Verify.run(Quarry.eulerBricks, path, blocks = 6)
      yield report

  test("a hunt's own file passes"):
    verifyTampered(identity).map: report =>
      assertEquals(report.last, "OK", report)
      val bricks = FastSearch.eulerBricksBetween(1, 3000).size
      assert(report.exists(_.contains(s"$bricks Euler bricks, 6 checkpoints")), report)

  test("a wrong brick is caught by checking it again"):
    verifyTampered(_.map(line => if line == "44 117 240" then "44 117 241" else line)).map: report =>
      assertEquals(report.last, "PROBLEMS FOUND")
      assert(report.exists(_.contains("not one of the Euler bricks: 44 117 241")), report)

  test("a missing brick is caught by searching its block again"):
    verifyTampered(_.filterNot(_ == "44 117 240")).map: report =>
      assertEquals(report.last, "PROBLEMS FOUND")
      assert(report.exists(_.contains("the block 1 to 500 differs")), report)

  test("a checkpoint going backwards is caught"):
    verifyTampered(_.map(line => if line.endsWith("up to 1500") then "# searched smallest edges up to 900" else line))
      .map(report => assertEquals(report.last, "PROBLEMS FOUND"))

  test("an edge cuboid file passes too"):
    withTempFile: path =>
      for
        _ <- Hunt.run(Quarry.edgeCuboids, path, 700_000, blockSize = 100_000, cores = 4)
        report <- Verify.run(Quarry.edgeCuboids, path, blocks = 2)
      yield assertEquals(report.last, "OK", report)

  test("blocks to search again are spread through the file"):
    val checkpoints = Vector(100L, 200, 300, 400, 500, 600)
    assertEquals(Verify.blocksToSearch(checkpoints, 3), List((1L, 100L), (201L, 300L), (401L, 500L)))
    assertEquals(Verify.blocksToSearch(checkpoints, 0), Nil)
    assertEquals(Verify.blocksToSearch(Vector.empty, 3), Nil)
