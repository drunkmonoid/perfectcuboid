package perfectcuboid

class ChartsTest extends munit.FunSuite:

  val series = List(
    Charts.Series("all", 1, List(1e3 -> 69.0, 1e4 -> 849.0, 1e5 -> 9004.0)),
    Charts.Series("primitive", 2, List(1e3 -> 14.0, 1e4 -> 56.0, 1e5 -> 180.0))
  )

  /** Every number in every cx="…" or cy="…" attribute. */
  def coordinates(svg: String, name: String): List[Double] =
    s"""$name="([0-9.]+)"""".r
      .findAllMatchIn(svg)
      .map(_.group(1).nn.toDouble)
      .toList // `.nn`: with explicit nulls, a missing group would be null

  test("a log axis puts each power of ten an equal step apart"):
    val axis = Charts.LogAxis(0, 4, 100, 500)
    assertEqualsDouble(axis(1), 100, 1e-9)
    assertEqualsDouble(axis(100), 300, 1e-9)
    assertEqualsDouble(axis(1e4), 500, 1e-9)

  test("a line chart has a dot for every point, all inside the picture"):
    val svg = Charts.lines("t", "x", "y", series)
    assertEquals(coordinates(svg, "cx").size, 6)
    assert(coordinates(svg, "cx").forall(x => x >= 0 && x <= 720))
    assert(coordinates(svg, "cy").forall(y => y >= 0 && y <= 440))
    assert(svg.startsWith("<svg") && svg.trim.endsWith("</svg>"))

  test("text is escaped, and every series is in the legend and labelled"):
    val svg = Charts.lines("a < b & c", "x", "y", series)
    assert(svg.contains("a &lt; b &amp; c"))
    assert(svg.split("primitive</text>").length - 1 == 2) // once in the legend, once at the line's end

  test("a scatter has a titled dot per point"):
    val svg = Charts.scatter("t", "x", "y", List((44.0, 240.0, "44 × 117 × 240"), (85.0, 720.0, "85 × 132 × 720")))
    assertEquals(coordinates(svg, "cx").size, 2)
    assert(svg.contains("<title>44 × 117 × 240</title>"))
