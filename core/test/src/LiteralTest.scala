package perfectcuboid

class LiteralTest extends munit.FunSuite:

  test("the constant bound is the computed one"):
    assertEquals(Edge.MaxLiteral, Edge.Max)

  test("literal edges and cuboids are the checked ones, the cuboid's edges sorted"):
    assertEquals(Edge(240): Long, 240L)
    assertEquals(Cuboid(240, 44, 117), Cuboid.of(44, 117, 240).get)
    assertEquals(Edge(5325116328314171699L): Long, Edge.Max)

  test("a literal out of range does not compile, and says why"):
    assert(compileErrors("Edge(0)").contains("an edge must be at least 1"))
    assert(compileErrors("Edge(-5)").contains("an edge must be at least 1"))
    assert(compileErrors("Edge(5325116328314171700L)").contains("an edge must be at most Edge.Max"))
    assert(compileErrors("Cuboid(44, 0, 240)").contains("an edge must be at least 1"))

  test("nor does a value known only at run time: that goes through Edge.from"):
    assert(compileErrors("val n = 240L; Edge(n)").nonEmpty)
