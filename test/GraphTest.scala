package perfectcuboid

class GraphTest extends munit.FunSuite:

  val graph: PythagoreanGraph = PythagoreanGraph.upTo(3000)

  test("its triangles are exactly the Euler bricks with every edge up to the limit"):
    val bricks = Search.eulerBricks(3000).map(c => (c.a: Long, c.b: Long, c.c: Long))
    assertEquals(graph.triangles.sorted, bricks.sorted)
    assertEquals(graph.triangles.size, bricks.size)

  test("degrees count the legs that pair with a number, up to the limit"):
    for x <- 1L to 3000L do assertEquals(graph.degree(x), Pythagorean.legsPairedWith(x).count(_ <= 3000), x)

  test("links are counted once"):
    assertEquals(graph.links, Pythagorean.legPairs(3000).size.toLong)

  test("the pieces split the numbers that have neighbours, and no link crosses between pieces"):
    val pieces = graph.components
    assertEquals(pieces.map(_.size).sum, graph.neighbours.size)
    val pieceOf = pieces.zipWithIndex.flatMap((piece, i) => piece.map(_ -> i)).toMap
    for (x, ns) <- graph.neighbours; y <- ns do assertEquals(pieceOf(x), pieceOf(y))

  test("3 and 4 pair only with each other, so they are a piece of their own"):
    assert(graph.components.contains(Set(3L, 4L)))
    assertEquals((graph.degree(1), graph.degree(2)), (0, 0))
