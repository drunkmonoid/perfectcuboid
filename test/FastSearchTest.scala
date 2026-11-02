package perfectcuboid

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import scala.concurrent.ExecutionContext.Implicits.global

class FastSearchTest extends munit.ScalaCheckSuite:

  property("legsAbove is legsPairedWith, filtered to the legs above a"):
    forAll(Gen.chooseNum(1L, Edge.Max)): a =>
      FastSearch.legsAbove(a).toList == Pythagorean.legsPairedWith(a).filter(y => y > a && y <= Edge.Max)

  test("the fast search finds exactly what the readable one does"):
    for a <- 1L to 3000L do
      assertEquals(FastSearch.eulerBricksWithSmallestEdge(a), Search.eulerBricksWithSmallestEdge(a), s"a = $a")

  test("the parallel search gives the same bricks in the same order"):
    val limit = 5000L
    val parallel = FastSearch.allEulerBricksParallel(blockSize = 7, cores = 3).takeWhile(_.a <= limit).toList
    assertEquals(parallel, Search.allEulerBricks.takeWhile(_.a <= limit).toList)

  test("isPerfectSquare's quick filter never rejects a square"):
    for r <- 0L to 100_000L do assert(Arith.isPerfectSquare(r * r), r)
