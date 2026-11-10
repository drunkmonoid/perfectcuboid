package perfectcuboid.web

import perfectcuboid.BigCuboid

class PageTest extends munit.FunSuite:

  val catalogue: Vector[Catalogue.Row] =
    Catalogue.rows(
      "44 117 240\n85 132 720\n# searched smallest edges up to 100\n140 480 693\n160 231 792\n240 252 275\n"
    )

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

  test("the catalogue reads a hunt's file, skipping its checkpoints"):
    assertEquals(catalogue.map(_.brick.a: Long), Vector(44L, 85, 140, 160, 240))

  test("the catalogue sorts and filters"):
    import Catalogue.Order
    assertEquals(Catalogue.page(catalogue, Order.Squarest, 0).rows.head.brick.a: Long, 240L)
    assertEquals(Catalogue.page(catalogue, Order.Flattest, 0).rows.head.brick.a: Long, 85L)
    assertEquals(Catalogue.filter(catalogue, Some(240), None, None).map(_.brick.a: Long), Vector(44L, 240))
    assertEquals(Catalogue.filter(catalogue, None, Some(100), Some(200)).size, 2)
    assertEquals(Catalogue.page(catalogue, Order.Smallest, 0).pages, 1)

  test("each row links to the checker with its box"):
    assert(Catalogue.tableRow(catalogue.head).contains("""href="../check/?box=44,117,240""""))

  test("the drawing has the twelve edges, three face diagonals, the space diagonal and their lengths"):
    val svg = Drawing.svg(BigCuboid.of(44, 117, 240).get, 0.6, 0.45)
    assertEquals("<line ".r.findAllIn(svg).size, 16)
    assertEquals("<text ".r.findAllIn(svg).size, 7)
    // Halcke's brick: every face diagonal whole, the space diagonal not.
    assertEquals("""class="space irrational"""".r.findAllIn(svg).size, 1)
    assertEquals("""class="face whole"""".r.findAllIn(svg).size, 3)
    assert(svg.contains(">125<") && svg.contains(">244<") && svg.contains(">267<") && svg.contains(">√73225<"), svg)

  test("turning keeps the drawing inside its frame, and a flat brick still shows"):
    for yaw <- List(0.0, 1.0, 2.5, -3.0); pitch <- List(-1.4, 0.0, 1.4) do
      val numbers = """[xy][12]="(-?[0-9.]+)"""".r
        .findAllMatchIn(Drawing.svg(BigCuboid.of(61427951, 12916225080L, 3375128053440L).get, yaw, pitch))
        .map(_.group(1).nn.toDouble)
        .toList
      assert(numbers.forall(v => v >= 0 && v <= Drawing.Width), (yaw, pitch))
    assertEquals(Drawing.proportions(BigCuboid.of(1, 1000, 1000000).get), Drawing.P3(0.05, 0.05, 1.0))
