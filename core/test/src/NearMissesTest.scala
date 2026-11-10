package perfectcuboid

import org.scalacheck.Prop.forAll
import Arith.*
import Generators.*

class NearMissesTest extends munit.ScalaCheckSuite:

  def squared(a2: Long, b2: Long, c2: Long): SquaredCuboid = SquaredCuboid.of(a2, b2, c2).get

  test("the kind of each almost-perfect example"):
    assertEquals(Cuboid(44, 117, 240).kind, Kind.Body)
    assertEquals(Cuboid(104, 153, 672).kind, Kind.Face)
    assertEquals(squared(520 * 520, 576 * 576, 618849).kind, Kind.Edge)
    assertEquals(squared(2, 3, 5).kind, Kind.Other)

  property("an ordinary cuboid has whole edges once squared"):
    forAll(cuboid)(c => c.squared.get.edges == List(Some(c.a: Long), Some(c.b: Long), Some(c.c: Long)))

  test("completions agree with checking every candidate"):
    for n <- 1L to 500L do
      val expected = (1L to n).filter(x => isPerfectSquare(x * x + n)).toList
      assertEquals(Pythagorean.completions(n), expected, s"n = $n")

  test("the first face cuboid is 104 × 153 × 672"):
    assertEquals(NearMisses.allFaceCuboids.head, Cuboid(104, 153, 672))

  test("the face cuboid search agrees with a brute-force search"):
    val n = 700L
    val bruteForce =
      for
        s <- 1L to n
        x <- 1L to n
        if x != s && isPerfectSquare(s * s + x * x)
        y <- x + 1 to n
        if y != s && isPerfectSquare(s * s + y * y) && isPerfectSquare(s * s + x * x + y * y)
      yield Cuboid.of(s, x, y).get
    val searched = (1L to n).flatMap(NearMisses.faceCuboidsWithSharedEdge).filter(_.c <= n)
    assert(bruteForce.nonEmpty)
    assertEquals(searched.sorted, bruteForce.sorted)
    assert(searched.forall(_.kind == Kind.Face))

  test("the first two edge cuboids, by irrational edge"):
    val firstTwo = NearMisses.allEdgeCuboids.take(2).toList
    assertEquals(firstTwo, List(squared(461776, 1443 * 1443, 1800 * 1800), squared(520 * 520, 576 * 576, 618849)))
    assert(firstTwo.forall(_.kind == Kind.Edge))

  test("the legs under a hypotenuse are those a brute-force search finds"):
    for h <- 1L to 2000L do
      val brute =
        (1L until h).filter(x => Arith.isPerfectSquare(h * h - x * x)).map(x => (x, Arith.isqrt(h * h - x * x)))
      assertEquals(Pythagorean.legsWithHypotenuse(h), brute.filter(_ < _).toList, s"h = $h")

  test("face cuboids found from the space diagonal are those found from the shared edge"):
    val (fromDiagonals, fromSharedEdges) = NearMisses.bothWays(5000)
    assertEquals(fromDiagonals, fromSharedEdges)
    assert(fromDiagonals.contains(Cuboid(104, 153, 672)))
    assert(fromDiagonals.forall(c => c.kind == Kind.Face))
