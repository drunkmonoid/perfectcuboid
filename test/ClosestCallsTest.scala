package perfectcuboid

import munit.ScalaCheckSuite
import org.scalacheck.{Gen, Prop}

class ClosestCallsTest extends ScalaCheckSuite:

  test("44 × 117 × 240: the space diagonal √73225 = 270.6012 misses 271 by 0.3988"):
    assertEqualsDouble(ClosestCalls.miss(Cuboid.of(44, 117, 240).get), 271 - math.sqrt(73225), 1e-12)

  property("a whole number's square misses by nothing, and one more misses by about 1/2r"):
    Prop.forAll(Gen.chooseNum(1000L, 3_000_000_000L)): r => // for large r: the exact miss is 1/2r − 1/8r³ + …
      val n = BigInt(r).pow(2)
      ClosestCalls.distanceToWhole(n) == 0 &&
      math.abs(ClosestCalls.distanceToWhole(n + 1) - 1.0 / (2 * r)) < 1e-6 / r

  property("the distance is never more than a half"):
    Prop.forAll(Gen.chooseNum(1L, Long.MaxValue)): n =>
      val d = ClosestCalls.distanceToWhole(BigInt(n))
      d >= 0 && d <= 0.5

  test("tiny distances stay accurate for numbers far beyond a Double's 16 digits"):
    val r = BigInt(10).pow(20) + 7
    assertEqualsDouble(ClosestCalls.distanceToWhole(r * r + 1) * 2 * r.toDouble, 1.0, 1e-9)

  test("the ranking keeps only primitive bricks, closest first"):
    val ranked = ClosestCalls.ranked(FastSearch.eulerBricksBetween(1, 5000))
    assert(ranked.forall(_.brick.isPrimitive))
    assertEquals(ranked.map(_.miss), ranked.map(_.miss).sorted)
