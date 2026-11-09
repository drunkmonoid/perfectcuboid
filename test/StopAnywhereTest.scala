package perfectcuboid

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import munit.ScalaCheckSuite
import org.scalacheck.{Gen, Prop, Test}
import scala.concurrent.duration.*

/** A hunt can be stopped at any moment, or its file cut off at any byte as by a crash, and
  * resuming it still ends in exactly the file one uninterrupted run writes, byte for byte.
  *
  * Checkpoints come after whole blocks, and blocks always start at multiples of the block size
  * plus one, so a resumed hunt cuts its blocks where an uninterrupted one would: the bytes, not
  * only the bricks, must come out the same.
  */
class StopAnywhereTest extends ScalaCheckSuite:

  override def scalaCheckTestParameters: Test.Parameters = super.scalaCheckTestParameters.withMinSuccessfulTests(12)

  private val (upTo, blockSize) = (40_000L, 500L)

  private def text(path: Path): IO[String] = Files[IO].readUtf8(path).compile.string

  private def hunt(path: Path, threads: Int): IO[Unit] =
    Hunt.run(Quarry.eulerBricks, path, upTo, blockSize, threads)

  /** The file an uninterrupted hunt writes. */
  private lazy val reference: String =
    Files[IO].tempFile.use(path => hunt(path, 4) >> text(path)).unsafeRunSync()

  property("stopped after any delays, on any number of threads, a resumed hunt ends in the same file"):
    Prop.forAll(Gen.listOfN(3, Gen.chooseNum(0, 120)), Gen.chooseNum(1, 4)): (delays, threads) =>
      val result = Files[IO].tempFile.use: path =>
        delays.traverse_(ms => hunt(path, threads).timeoutTo(ms.millis, IO.unit)) >> hunt(path, threads) >> text(path)
      result.unsafeRunSync() == reference

  property("cut off at any byte, as by a crash while writing, a resumed hunt ends in the same file"):
    Prop.forAll(Gen.chooseNum(0, reference.length)): cut =>
      val result = Files[IO].tempFile.use: path =>
        fs2.Stream.emit(reference.take(cut)).through(Files[IO].writeUtf8(path)).compile.drain >> hunt(path, 3) >> text(
          path
        )
      result.unsafeRunSync() == reference

  test("the reference file holds every brick up to its last edge"):
    assertEquals(
      HuntFile.parse(Quarry.eulerBricks, reference.linesIterator.toList),
      FastSearch.eulerBricksBetween(1, upTo)
    )
