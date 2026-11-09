package perfectcuboid

import munit.ScalaCheckSuite
import org.scalacheck.{Gen, Prop}

class WideTest extends ScalaCheckSuite:

  val anyLeg: Gen[Long] = Gen.chooseNum(0L, Long.MaxValue)
  val edge: Gen[Long] = Gen.chooseNum(0L, Edge.Max)

  property("squares and sums agree with BigInt"):
    Prop.forAll(edge, edge): (b, c) =>
      (Wide.square(b) + Wide.square(c)).toBigInt == BigInt(b).pow(2) + BigInt(c).pow(2)

  property("isqrt agrees with BigInt, for any number up to (2⁶³ − 1)²"):
    Prop.forAll(anyLeg, Gen.chooseNum(0L, Long.MaxValue)): (r, extra) =>
      // r² + something below 2r + 1, so the floor of the root is r; but no more than the largest Wide.
      val n = (BigInt(r).pow(2) + BigInt(extra) % (2 * BigInt(r) + 1)).min(BigInt(Long.MaxValue).pow(2))
      val wide = Wide((n >> 64).toLong, n.toLong)
      Wide.isqrt(wide.hi, wide.lo) == r

  property("exactSqrt finds the roots of squares, and nothing else"):
    Prop.forAll(anyLeg): r =>
      Wide.exactSqrt(Wide.square(r)).contains(r) &&
        (r == 0 || r == Long.MaxValue || Wide.exactSqrt(Wide.square(r) + Wide(1)).isEmpty)

  property("isSumOfSquaresSquare agrees with BigInt"):
    Prop.forAll(edge, edge): (b, c) =>
      Wide.isSumOfSquaresSquare(b, c) == Arith.isPerfectSquareBig(BigInt(b).pow(2) + BigInt(c).pow(2))

  property("and finds the squares among multiples of triples, however large"):
    val triple = Gen.oneOf((3L, 4L), (20L, 21L), (119L, 120L), (44L, 117L))
    Prop.forAll(triple, Gen.chooseNum(1L, 10_000_000_000_000_000L)): (pair, k) =>
      val (x, y) = pair
      val (b, c) = (BigInt(x) * k, BigInt(y) * k)
      c > Edge.Max || Wide.isSumOfSquaresSquare(b.toLong, c.toLong)

  /** Numbers spread evenly over the powers of two from 2^lo to 2^hi, so that the two ways of
    * testing a square, rounding below 2⁵¹ and `isqrt` above, both get tried near their border.
    */
  def spread(lo: Int, hi: Int): Gen[Long] =
    for
      bits <- Gen.chooseNum(lo, hi - 1)
      n <- Gen.chooseNum(1L << bits, (1L << bits) * 2 - 1)
    yield n

  property("isSumOfSquaresSquare finds the squares made by Euclid's formula, and not one more"):
    // (m² − n²)² + (2mn)² = (m² + n²)², and adding 1 to 2mn lands strictly between two squares.
    Prop.forAll(spread(16, 31), spread(16, 31)): (x, y) =>
      val (m, n) = (x max y, x min y)
      val (b, c) = (m * m - n * n, 2 * m * n)
      m == n || (Wide.isSumOfSquaresSquare(b, c) && !Wide.isSumOfSquaresSquare(b, c + 1))

  property("and on both sides of 2⁵¹, where the rounded square root stops being exact"):
    // (3k)² + (4k)² = (5k)², and (4k ± 1) in place of 4k is never a square.
    Prop.forAll(spread(29, 60)): k =>
      Wide.isSumOfSquaresSquare(3 * k, 4 * k) &&
        !Wide.isSumOfSquaresSquare(3 * k, 4 * k + 1) && !Wide.isSumOfSquaresSquare(3 * k, 4 * k - 1)

  test("the largest Wide, and one past it"):
    val max = Wide.square(Long.MaxValue)
    assertEquals(max.toBigInt, BigInt(Long.MaxValue).pow(2))
    assertEquals(Wide.exactSqrt(max), Some(Long.MaxValue))
    val _ = intercept[IllegalArgumentException](max + Wide(1))

  test("a cuboid's diagonals are right even when their squares are beyond a Long"):
    val k = 1_000_000_000_000_000L // 10¹⁵: the edges reach 2.4·10¹⁷, their squares 5.8·10³⁴
    val brick = Cuboid.of(44 * k, 117 * k, 240 * k).get
    assertEquals(brick.faceDiagonals.map(_.map(d => d: Long)), List(Some(125 * k), Some(244 * k), Some(267 * k)))
    assert(brick.isEuler && !brick.isPerfect && brick.kind == Kind.Body)
    assertEquals(brick.kind, BigCuboid.from(brick).kind)

  test("the search now finds bricks whose largest edge is beyond the old cap"):
    // The first one, by smallest edge: 10 times 74745 × 23085952 × 186227160.
    val first = Cuboid.of(747_450, 230_859_520, 1_862_271_600).get
    assert(first.c > SquaredCuboid.MaxEdge && first.isEuler)
    assert(FastSearch.eulerBricksWithSmallestEdge(747_450).contains(first))
    assertEquals(FastSearch.eulerBricksWithSmallestEdge(747_450), Search.eulerBricksWithSmallestEdge(747_450))
