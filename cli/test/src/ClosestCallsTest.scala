package perfectcuboid

import munit.ScalaCheckSuite
import org.scalacheck.{Gen, Prop}

class ClosestCallsTest extends ScalaCheckSuite:

  test("44 × 117 × 240: the space diagonal √73225 = 270.6012 misses 271 by 0.3988"):
    assertEqualsDouble(ClosestCalls.miss(Cuboid(44, 117, 240)), 271 - math.sqrt(73225), 1e-12)

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

  test("a face cuboid misses by its irrational face diagonal, an edge cuboid by its irrational edge"):
    // 104 × 153 × 672: the face 153 × 672 has diagonal √474993 ≈ 689.197, 0.197 from 689.
    assertEqualsDouble(ClosestCalls.faceMiss(Cuboid(104, 153, 672)), math.sqrt(474993) - 689, 1e-9)
    // 520 × 576 × √618849: √618849 ≈ 786.669, 0.331 from 787.
    assertEqualsDouble(
      ClosestCalls.edgeMiss(SquaredCuboid.of(520L * 520, 576L * 576, 618849).get),
      787 - math.sqrt(618849),
      1e-9
    )

  test("the near-miss lines rank the finds and compare the best with chance"):
    val lines = ClosestCalls.nearLines(Vector(0.3, 0.1, 0.2), "numbers", identity, _.toString, count = 2)
    assertEquals(lines.head, "The primitive numbers nearest to perfect, of 3:")
    assertEquals(lines.slice(1, 3), List("  0.1   off by 1.00e-01", "  0.2   off by 2.00e-01"))
    assert(lines.last.contains("about 1.3e-01"), lines.last)

  test("an edge cuboid scaled up is not primitive"):
    val first = SquaredCuboid.of(461776, 1443L * 1443, 1800L * 1800).get
    assert(first.isPrimitive)
    assert(!SquaredCuboid.of(4 * 461776L, 4 * 1443L * 1443, 4 * 1800L * 1800).get.isPrimitive)
