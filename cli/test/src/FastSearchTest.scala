package perfectcuboid

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import scala.concurrent.ExecutionContext.Implicits.global

class FastSearchTest extends munit.ScalaCheckSuite:

  property("legsAbove is legsPairedWith, filtered to the legs above a"):
    forAll(Gen.chooseNum(1L, Search.MaxSmallestEdge)): a =>
      FastSearch.legsAbove(a).toList == Pythagorean.legsPairedWith(a).filter(_ > a)

  property("legs worked out with BigInt agree with legs worked out with Longs, where both can"):
    forAll(Gen.chooseNum(1L, Search.MaxSmallestEdge)): a =>
      val factors = NumberTheory.factorize(a)
      val long = FastSearch.legsAboveLong(a, factors).toList
      FastSearch.legsAboveBig(a, factors).toList == long && FastSearch.legsAboveWide(a, factors).toList == long

  /** Edges past the Long square limit: mostly just past it, where the long hunts go, and up to
    * `Edge.Max`, and some with many small factors, which have the most legs.
    */
  private val pastTheLimit: Gen[Long] =
    val limit = Search.MaxSmallestEdge
    Gen.frequency(
      4 -> Gen.chooseNum(limit + 1, 100_000_000_000L),
      2 -> Gen.chooseNum(limit + 1, Edge.Max),
      2 -> Gen.chooseNum(limit / 720_720 + 1, 1_000_000_000L).map(_ * 720_720),
      1 -> Gen.chooseNum(limit / 27_720 + 1, Edge.Max / 27_720).map(_ * 27_720)
    )

  property("past the Long square limit, legs in Long arithmetic agree with legs in BigInt"):
    forAll(pastTheLimit): a =>
      val factors = NumberTheory.factorize(a)
      FastSearch.legsAboveWide(a, factors).toList == FastSearch.legsAboveBig(a, factors).toList

  test("and so do the edges at the limit, and the largest edge"):
    for a <- List(Search.MaxSmallestEdge, Search.MaxSmallestEdge + 1, 3_265_000_000L, 9_999_999_000L, Edge.Max) do
      val factors = NumberTheory.factorize(a)
      assertEquals(FastSearch.legsAboveWide(a, factors).toList, FastSearch.legsAboveBig(a, factors).toList, s"a = $a")

  test("past the Long square limit, a known brick scaled up is still found"):
    // 44k × 117k × 240k is an Euler brick for any k; this k puts 44k just past 3,037,000,499.
    val k = Search.MaxSmallestEdge / 44 + 1
    val a = 44 * k
    assert(a > Search.MaxSmallestEdge)
    val found = FastSearch.eulerBricksBetween(a, a)
    assert(found.exists(c => (c.b: Long) == 117 * k && (c.c: Long) == 240 * k), found)
    assert(found.forall(c => BigCuboid.from(c).isEuler))

  test("and past 10¹², where the sieve gives way to factorising one edge at a time"):
    val k = 100_000_000_000L
    assert(44 * k > Sieve.MaxLast)
    assert(FastSearch.eulerBricksBetween(44 * k, 44 * k).exists(c => (c.b: Long) == 117 * k))

  test("the fast search finds exactly what the readable one does"):
    for a <- 1L to 3000L do
      assertEquals(FastSearch.eulerBricksWithSmallestEdge(a), Search.eulerBricksWithSmallestEdge(a), s"a = $a")

  test("sieving blocks and going one edge at a time give the same bricks"):
    val limit = 5000L
    val expected = Search.allEulerBricks.takeWhile(_.a <= limit).toList
    assertEquals(FastSearch.allEulerBricks.takeWhile(_.a <= limit).toList, expected)
    assertEquals(FastSearch.allEulerBricksOneByOne.takeWhile(_.a <= limit).toList, expected)
    assertEquals(FastSearch.eulerBricksBetween(1234, 4321), expected.filter(c => c.a >= 1234 && c.a <= 4321))

  test("the parallel search gives the same bricks in the same order"):
    val limit = 5000L
    val parallel = FastSearch.allEulerBricksParallel(blockSize = 7, cores = 3).takeWhile(_.a <= limit).toList
    assertEquals(parallel, Search.allEulerBricks.takeWhile(_.a <= limit).toList)

  test("isPerfectSquare's quick filter never rejects a square"):
    for r <- 0L to 100_000L do assert(Arith.isPerfectSquare(r * r), r)
