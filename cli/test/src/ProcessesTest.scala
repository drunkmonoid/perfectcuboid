package perfectcuboid

import cats.effect.IO
import fs2.io.file.{Files, Path}
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class ProcessesTest extends munit.CatsEffectSuite with munit.ScalaCheckSuite:

  property("a plan covers the range in order, with no gaps or overlaps, in shares of nearly one size"):
    forAll(Gen.chooseNum(1L, 1_000_000L), Gen.chooseNum(0L, 1_000_000L), Gen.chooseNum(1, 64)): (first, length, n) =>
      val upTo = first + length - 1
      val shards = Processes.plan(first, upTo, n)
      val sizes = shards.map(s => s.to - s.from + 1)
      if length == 0 then shards.isEmpty
      else
        shards.head.from == first && shards.last.to == upTo &&
        shards.zip(shards.drop(1)).forall((a, b) => b.from == a.to + 1) &&
        shards.size == math.min(n.toLong, length) && sizes.max - sizes.min <= 1 && sizes.min >= 1

  test("the program starts again with its command line up to the word hunt"):
    assertEquals(
      Processes.prefix(List("java", "-Xmx1g", "-jar", "perfectcuboid.jar", "hunt", "f.txt", "1e10")),
      Some(List("java", "-Xmx1g", "-jar", "perfectcuboid.jar"))
    )
    assertEquals(Processes.prefix(List("./perfectcuboid", "hunt", "f.txt", "9")), Some(List("./perfectcuboid")))
    assertEquals(Processes.prefix(List("java", "-jar", "p.jar", "verify", "f.txt")), None)
    assertEquals(Processes.prefix(List("hunt", "f.txt")), None)

  def withDir[A](body: Path => IO[A]): IO[A] = Files[IO].tempDirectory.use(body)

  /** Each shard run in this JVM, the way its process would run it. */
  def inProcess(shard: Processes.Shard): IO[Int] =
    Hunt.run(Quarry.primitiveBricks, shard.path, shard.to, blockSize = 500, cores = 2, Some(shard.from)).as(0)

  def bricks(path: Path): IO[List[Cuboid]] =
    Files[IO].readUtf8Lines(path).compile.toList.map(HuntFile.parse(Quarry.primitiveBricks, _))

  test("shards on several processes, merged, find what one hunt finds, and leave nothing behind"):
    withDir: dir =>
      val file = dir / "hunt.txt"
      for
        report <- Processes.hunt(Quarry.primitiveBricks, file, 20_000, 3, None, inProcess)
        found <- bricks(file)
        resume <- Hunt.readResume(Quarry.primitiveBricks, file)
        verified <- Verify.run(Quarry.primitiveBricks, file, blocks = 3)
        left <- Files[IO].list(dir).compile.toList
      yield
        assertEquals(found, FastSearch.eulerBricksBetween(1, 20_000).filter(_.isPrimitive))
        assertEquals(resume.searchedUpTo, 20_000L)
        assertEquals(verified.last, "OK", verified)
        assertEquals(left, List(file), report)

  test("they carry on a file that is part searched"):
    withDir: dir =>
      val file = dir / "hunt.txt"
      for
        _ <- Hunt.run(Quarry.primitiveBricks, file, 7_000, blockSize = 500, cores = 2)
        _ <- Processes.hunt(Quarry.primitiveBricks, file, 20_000, 4, None, inProcess)
        found <- bricks(file)
      yield assertEquals(found, FastSearch.eulerBricksBetween(1, 20_000).filter(_.isPrimitive))

  test("a shard from another split is not resumed, and nothing is merged"):
    withDir: dir =>
      val file = dir / "hunt.txt"
      for
        _ <- Hunt.run(
          Quarry.primitiveBricks,
          Processes.shardPath(file, 1),
          3_000,
          blockSize = 500,
          cores = 2,
          Some(2_001)
        )
        report <- Processes.hunt(Quarry.primitiveBricks, file, 20_000, 3, None, inProcess)
        exists <- Files[IO].exists(file)
      yield
        assert(report.head.startsWith("These shards are from a different split"), report)
        assert(!exists)

  test("when a shard fails, the file is left as it was"):
    withDir: dir =>
      val file = dir / "hunt.txt"
      for
        _ <- Hunt.run(Quarry.primitiveBricks, file, 2_000, blockSize = 500, cores = 2)
        before <- Files[IO].readUtf8(file).compile.string
        report <- Processes.hunt(
          Quarry.primitiveBricks,
          file,
          20_000,
          2,
          None,
          s => if s.number == 1 then IO.pure(3) else inProcess(s)
        )
        after <- Files[IO].readUtf8(file).compile.string
      yield
        assertEquals(report, List("Shard 1 stopped with code 3: see its log"))
        assertEquals(after, before)
