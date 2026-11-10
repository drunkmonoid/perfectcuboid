package perfectcuboid

class CuboidTest extends munit.FunSuite:

  // `.get` is fine in tests: we know these edges are valid.
  def cuboid(x: Long, y: Long, z: Long): Cuboid = Cuboid.of(x, y, z).get

  test("of sorts the edges"):
    val c = cuboid(240, 44, 117)
    assertEquals((c.a, c.b, c.c), (44L, 117L, 240L))

  test("of rejects non-positive edges"):
    assertEquals(Cuboid.of(0, 1, 2), None)
    assertEquals(Cuboid.of(-1, 1, 2), None)

  test("44 × 117 × 240 is the smallest Euler brick, but not perfect"):
    val c = cuboid(44, 117, 240)
    assertEquals(c.faceDiagonals: List[Option[Long]], List(Some(125L), Some(244L), Some(267L)))
    assertEquals(c.spaceDiagonal: Option[Long], None)
    assert(c.isEuler)
    assert(!c.isPerfect)
    assertEquals(c.kind, Kind.Body)

  test("104 × 153 × 672 is a face cuboid"):
    val c = cuboid(104, 153, 672)
    assertEquals(c.faceDiagonals: List[Option[Long]], List(Some(185L), Some(680L), None))
    assertEquals(c.spaceDiagonal: Option[Long], Some(697L))
    assertEquals(c.kind, Kind.Face)

  test("an ordinary box"):
    assertEquals(cuboid(1, 2, 3).kind, Kind.Other)

  test("isPrimitive"):
    assert(cuboid(44, 117, 240).isPrimitive)
    assert(!cuboid(88, 234, 480).isPrimitive)

  test("from says which edge is wrong"):
    assertEquals(Cuboid.from(3, 0, 5), Left(CuboidError.NonPositiveEdge(0)))
    assertEquals(Cuboid.from(3, 4, Edge.Max + 1), Left(CuboidError.EdgeTooLarge(Edge.Max + 1)))
    assert(Cuboid.from(Edge.Max, Edge.Max, Edge.Max).isRight)

  test("the largest allowed cuboid's diagonals do not overflow"):
    val c = cuboid(Edge.Max, Edge.Max, Edge.Max)
    assertEquals(c.spaceDiagonal: Option[Long], None)
    assertEquals(c.kind, Kind.Other)

  test("an edge can be used as a number"):
    val e: Long = 5L.toEdge.toOption.get
    assertEquals(e * 2, 10L)

  test("a plain number is not an edge, and neither is a diagonal"):
    assertEquals(compileErrors("val e: Long = Edge.from(5L).toOption.get"), "")
    assert(compileErrors("val e: Edge = 5L").nonEmpty)
    assert(compileErrors("val d: Diagonal = Diagonal.fromSquare(25).get; val e: Edge = d").nonEmpty)
    assert(compileErrors("val d: Diagonal = 5L").nonEmpty)
