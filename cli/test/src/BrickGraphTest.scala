package perfectcuboid

class BrickGraphTest extends munit.FunSuite:

  def brick(a: Long, b: Long, c: Long): BigCuboid = BigCuboid.of(a, b, c).get

  val halcke: BigCuboid = brick(44, 117, 240)
  val partner: BigCuboid = brick(429, 880, 2340)
  // 2P on Halcke's curve fixing 44 and 117 (RESULTS.md).
  val double: BigCuboid = brick(9555040, 22855959, 25407720)
  val other: BigCuboid = brick(240, 252, 275)

  test("a brick is joined to its derived partner and to its curve multiples, once each"):
    val links = BrickGraph.links(Vector(halcke, partner, double, other))
    assert(links.contains(BrickGraph.Link(halcke, partner, BrickGraph.Join.Derivation)), links)
    assert(links.contains(BrickGraph.Link(halcke, double, BrickGraph.Join.Curve)), links)
    assertEquals(links.distinct, links)
    assert(links.forall(l => Ordering[BigCuboid].lt(l.small, l.large)), links)

  test("the pieces are the joined families, every brick in exactly one"):
    val graph = BrickGraph.of(Vector(other, double, partner, halcke))
    assertEquals(graph.pieces, Vector(Vector(halcke, partner, double), Vector(other)))

  test("on real bricks, the pieces cover every brick once, and derived pairs share a piece"):
    val bricks = FastSearch.eulerBricksBetween(1, 100_000).filter(_.isPrimitive).map(BigCuboid.from).toVector
    val graph = BrickGraph.of(bricks)
    assertEquals(graph.pieces.flatten.sorted, bricks.sorted)
    val pieceOf = graph.pieces.zipWithIndex.flatMap((p, k) => p.map(_ -> k)).toMap
    bricks.foreach: b =>
      val d = Derived.derived(b)
      if pieceOf.contains(d) then assertEquals(pieceOf(d), pieceOf(b), b)
    assert(BrickGraph.lines(graph, "up to 100,000").head.startsWith(s"The ${bricks.size} primitive Euler bricks"))
