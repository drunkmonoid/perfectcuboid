package perfectcuboid

import munit.ScalaCheckSuite
import org.scalacheck.{Gen, Prop}

/** Every formula against a count made the slow way. */
class CountingTest extends ScalaCheckSuite:

  def factors(n: Long) = NumberTheory.factorize(n)

  /** Every x < y with x² + y² = n², by trying every x. */
  def hypotenuseTriples(n: Long): List[(Long, Long)] =
    (1L until n).toList.flatMap(x => Arith.exactSqrt(n * n - x * x).filter(_ > x).map(x -> _))

  test("as a leg: the formula counts what legsPairedWith lists"):
    for a <- 1L to 5000L do assertEquals(Counting.asLeg(factors(a)), legs(a).size.toLong, a)

  def legs(a: Long): List[Long] = Pythagorean.legsPairedWith(a)

  property("as a leg, for larger numbers"):
    Prop.forAll(Gen.chooseNum(1L, 1_000_000_000L))(a => Counting.asLeg(factors(a)) == legs(a).size)

  test("as a hypotenuse"):
    for n <- 1L to 3000L do assertEquals(Counting.asHypotenuse(factors(n)), hypotenuseTriples(n).size.toLong, n)

  test("sums of two squares, signs and order counted"):
    for n <- 1L to 3000L do
      val r = Arith.isqrt(n)
      val ways = for
        x <- -r to r
        y <- -r to r
        if x * x + y * y == n
      yield ()
      assertEquals(Counting.sumsOfTwoSquares(factors(n)), ways.size.toLong, n)

  test("as the leg of a primitive triple"):
    for a <- 1L to 5000L do
      val primitive = legs(a).count(y => Arith.gcd(a, y) == 1)
      assertEquals(Counting.asPrimitiveLeg(a, factors(a)), primitive.toLong, a)

  test("as the hypotenuse of a primitive triple"):
    for n <- 1L to 3000L do
      val primitive = hypotenuseTriples(n).count((x, y) => Arith.gcd(x, y) == 1)
      assertEquals(Counting.asPrimitiveHypotenuse(n, factors(n)), primitive.toLong, n)

  test("familiar cases"):
    assertEquals(Counting.asLeg(factors(240)), 31L) // as `legs 240` lists
    assertEquals(Counting.asHypotenuse(factors(5)), 1L) // 3, 4, 5
    assertEquals(Counting.asHypotenuse(factors(25)), 2L) // 7, 24, 25 and 15, 20, 25
    assertEquals(Counting.asPrimitiveHypotenuse(65, factors(65)), 2L) // 16, 63, 65 and 33, 56, 65
    assertEquals(Counting.sumsOfTwoSquares(factors(3)), 0L)
