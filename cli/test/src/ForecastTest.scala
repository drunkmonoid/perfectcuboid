package perfectcuboid

class ForecastTest extends munit.FunSuite:

  // The local densities mod 2¹³ take seconds alone, and far longer while `./mill __.test` builds
  // and runs the other platforms' tests beside these.
  override val munitTimeout: scala.concurrent.duration.Duration = scala.concurrent.duration.Duration(180, "s")

  test("an exact power law gives its own exponent, no slope, and an exact forecast"):
    val counts = List(1_000_000L, 100_000_000L, 10_000_000_000L, 1_000_000_000_000L).map: n =>
      (limit = n, count = math.round(math.sqrt(n.toDouble)).toInt)
    Forecast.exponents(counts).foreach(e => assertEqualsDouble(e.k, 0.5, 1e-9))
    val growth = Forecast.growth(counts, 4_000_000_000_000L)
    assertEqualsDouble(growth.slope, 0.0, 1e-9)
    assertEqualsDouble(growth.expected, 2_000_000.0, 1e-3)
    assertEqualsDouble(growth.low, growth.high, 1e-6)

  test("a falling exponent forecasts less than carrying the last one on"):
    val counts = List((1_000_000L, 1000), (10_000_000L, 2800), (100_000_000L, 7000), (1_000_000_000L, 16_000))
      .map((n, c) => (limit = n, count = c))
    val growth = Forecast.growth(counts, 10_000_000_000L)
    assert(growth.slope < 0, growth)
    assert(growth.low < growth.expected && growth.expected < growth.high, growth)
    assertEqualsDouble(growth.high, 16_000 * math.pow(10, Forecast.exponents(counts).last.k), 1e-6)

  test("the chance integral matches a sum over the bricks the power law puts in each step"):
    val (scale, k, count, s, t) = (5.0, 0.35, 15_000.0, 3e9, 1e10)
    def countAt(a: Double) = count * math.pow(a / s, k)
    val steps = 100_000
    val width = (t - s) / steps
    val sum = (0 until steps)
      .map: i =>
        val a = s + (i + 0.5) * width
        (countAt(a + width / 2) - countAt(a - width / 2)) * scale / a
      .sum
    assertEqualsDouble(Forecast.chance(scale, k, count, s, t), sum, sum * 1e-6)

  test("no new bricks cannot beat the closest call, and many almost surely do"):
    assertEqualsDouble(Forecast.beats(4e-5, 0), 0.0, 1e-12)
    assert(Forecast.beats(4e-5, 1e6) > 0.999)

  test("each prime's shares add up to one on both sides of the split"):
    val bricks = FastSearch.eulerBricksBetween(1, 30_000).filter(_.isPrimitive)
    Forecast
      .shares(bricks, 10_000, 100)
      .foreach: (p, shares) =>
        assertEqualsDouble(shares.map(_.below).sum, 1.0, 1e-9, s"p = $p")
        assertEqualsDouble(shares.map(_.above).sum, 1.0, 1e-9, s"p = $p")
        shares.foreach(s => assert(s.low <= s.above && s.above <= s.high, s))

  test("the forecast's lines name the range, the count and the closest call"):
    val bricks = FastSearch.eulerBricksBetween(1, 1_200_000).filter(_.isPrimitive)
    val lines = Forecast.lines(bricks, 1_200_000, 10_000_000)
    assert(lines.head.startsWith("Forecast for the primitive bricks with smallest edge from 1,200,001 to 10,000,000"))
    assert(lines.exists(_.startsWith("Primitive bricks up to 10,000,000: about ")), lines)
    assert(lines.exists(_.startsWith("The closest call so far, ")), lines)
