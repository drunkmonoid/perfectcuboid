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
