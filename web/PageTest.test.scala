package perfectcuboid.web

class PageTest extends munit.FunSuite:

  test("an Euler brick, its derived brick and the bricks on its curves"):
    val r = Check(List("240", "117", "44")).toOption.get
    assertEquals(r.box, "44 × 117 × 240")
    assertEquals(r.faces, List("125", "244", "267"))
    assertEquals(r.space, "√73225")
    assertEquals(r.derived, Some("429 × 880 × 2340"))
    assert(r.curves.contains("keeping 44 and 117: 9555040 × 22855959 × 25407720"), r.curves)

  test("a scaled box says what it is a copy of"):
    assertEquals(Check(List("2", "4", "6")).toOption.flatMap(_.primitive), Some("1 × 2 × 3"))

  test("bad input is explained, not thrown"):
    assert(Check(List("1", "two", "3")).isLeft)
    assert(Check(List("0", "1", "2")).isLeft)
    assert(Page.render(Check(List("x", "y", "z"))).contains("problem"))

  test("text is escaped in the page"):
    assert(Page.render(Left("<b>")).contains("&lt;b&gt;"))

  test("a block of the browser's search finds what the search finds"):
    val block = Hunting.block(1, 1000)
    assertEquals(block.last, 1000L)
    assertEquals(block.bricks, 69) // every Euler brick with smallest edge up to 1000, as in RESULTS.md
    assertEquals(block.primitive.take(2), List("44 × 117 × 240", "85 × 132 × 720"))

  test("the progress line says how far, how many and how long yet"):
    assertEquals(
      Hunting.progress(5000, 20000, 69, 14, 2.0),
      "Searched up to 5,000 of 20,000: 69 Euler bricks, 14 primitive, 2,500 edges a second, about 6 s left."
    )
    assert(!Hunting.progress(20000, 20000, 1, 1, 1.0).contains("left"))
