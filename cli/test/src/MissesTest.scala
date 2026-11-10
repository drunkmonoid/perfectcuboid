package perfectcuboid

class MissesTest extends munit.FunSuite:

  test("the fractional part of a square root, small and far beyond a Double"):
    assertEqualsDouble(Misses.fraction(BigInt(2)), math.sqrt(2.0) - 1, 1e-15)
    assertEqualsDouble(Misses.fraction(BigInt(73225)), math.sqrt(73225.0) - 270, 1e-12)
    assertEqualsDouble(Misses.fraction(BigInt(271).pow(2)), 0.0, 0.0)
    // √(r² + 1) − r = 1 / (√(r² + 1) + r), about 1 / 2r: a Double alone would round it to 0.
    val r = BigInt(10).pow(15) + 1
    assertEqualsDouble(Misses.fraction(r * r + 1), 0.5 / r.toDouble, 1e-30)

  test("the space diagonal of 44 × 117 × 240 is √73225 ≈ 270.6"):
    assertEqualsDouble(Misses.fraction(Cuboid(44, 117, 240)), math.sqrt(73225.0) - 270, 1e-12)

  test("evenly spread numbers are as near even as can be, and pass"):
    val n = 1000
    val even = (0 until n).map(i => (i + 0.5) / n)
    assertEqualsDouble(Misses.ksStatistic(even), 0.5 / n, 1e-12)
    assert(Misses.ksPValue(Misses.ksStatistic(even), n) > 0.99)

  test("numbers bunched near 0 fail"):
    val n = 1000
    val bunched = (0 until n).map(i => math.pow((i + 0.5) / n, 2))
    assert(Misses.ksPValue(Misses.ksStatistic(bunched), n) < 1e-6)

  test("the report has a row for all the bricks, and one for each decade with enough of them"):
    val bricks = FastSearch.eulerBricksBetween(1, 200_000).filter(_.isPrimitive)
    val lines = Misses.lines(bricks, 200_000)
    assert(lines.exists(_.startsWith(s"| all | ${bricks.size} |")), lines)
    assert(lines.exists(_.startsWith("| smallest edge to 100,000 |")), lines)
    assert(lines.last.startsWith("Nearest a whole number: "), lines)
