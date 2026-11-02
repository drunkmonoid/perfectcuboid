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
    assertEquals(c.faceDiagonals, List(Some(125L), Some(244L), Some(267L)))
    assertEquals(c.spaceDiagonal, None)
    assert(c.isEuler)
    assert(!c.isPerfect)
    assertEquals(c.kind, Kind.Body)

  test("104 × 153 × 672 is a face cuboid"):
    val c = cuboid(104, 153, 672)
    assertEquals(c.faceDiagonals, List(Some(185L), Some(680L), None))
    assertEquals(c.spaceDiagonal, Some(697L))
    assertEquals(c.kind, Kind.Face)

  test("an ordinary box"):
    assertEquals(cuboid(1, 2, 3).kind, Kind.Other)

  test("isPrimitive"):
    assert(cuboid(44, 117, 240).isPrimitive)
    assert(!cuboid(88, 234, 480).isPrimitive)
