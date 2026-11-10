package perfectcuboid

import munit.ScalaCheckSuite
import org.scalacheck.{Gen, Prop}

class EllipticTest extends ScalaCheckSuite:

  val curve = EulerCurve(44, 117)
  val p = curve.pointOf(240).get

  val small: Gen[Rational] =
    for
      n <- Gen.chooseNum(-1000L, 1000L)
      d <- Gen.chooseNum(1L, 1000L)
    yield Rational(n, d)

  property("fractions are kept in lowest terms, so equal fractions are equal values"):
    Prop.forAll(small, small, small): (x, y, z) =>
      (x + y) * z == x * z + y * z && (y.isZero || (x / y) * y == x) && Rational(6, -4) == Rational(-3, 2)

  test("square roots of fractions"):
    assertEquals(Rational(9, 4).sqrt, Some(Rational(3, 2)))
    assertEquals(Rational(2, 9).sqrt, None)

  test("a brick's third edge gives a point on the curve, and only an Euler brick's does"):
    assert(curve.contains(p))
    assertEquals(curve.pointOf(241), None)

  test("the group law: adding points stays on the curve, in any grouping"):
    val twice = curve.add(p, p)
    val thrice = curve.add(twice, p)
    assert(curve.contains(twice) && curve.contains(thrice))
    assertEquals(curve.add(twice, twice), curve.add(thrice, p))
    assertEquals(curve.add(curve.add(p, twice), thrice), curve.add(p, curve.add(twice, thrice)))
    assertEquals(curve.add(p, Point.Infinity), p)
    val Point.At(x, y) = p: @unchecked
    assertEquals(curve.add(p, Point.At(x, -y)), Point.Infinity)

  test("every multiple of a brick's point gives an Euler brick"):
    val bricks = FastSearch.eulerBricksBetween(1, 2000).filter(_.isPrimitive).map(BigCuboid.from)
    for
      brick <- bricks
      curve = EulerCurve(brick.a, brick.b)
      point = curve.pointOf(brick.c).get
      (multiple, i) <- curve.multiples(point).take(3).zipWithIndex
    do
      val found = curve.brickOf(multiple)
      assert(found.exists(_.isEuler), s"${i + 1} × the point of $brick")
      if i == 0 then assertEquals(found, Some(brick))

  test("two bricks built on curves are found by the search as well"):
    // 2P on the curves through 44 × 117 × 240 with edges (44, 117) and (117, 240).
    assert(FastSearch.eulerBricksWithSmallestEdge(9_555_040).contains(Cuboid.of(9_555_040, 22_855_959, 25_407_720).get))
    assert(FastSearch.eulerBricksWithSmallestEdge(9_813_375).contains(Cuboid.of(9_813_375, 20_130_000, 24_523_072).get))
    assertEquals(curve.brickOf(curve.add(p, p)), BigCuboid.of(9_555_040, 22_855_959, 25_407_720))
