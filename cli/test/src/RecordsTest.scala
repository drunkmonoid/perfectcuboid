package perfectcuboid

class RecordsTest extends munit.FunSuite:

  val bricks: List[Cuboid] = FastSearch.eulerBricksBetween(1, 3000)

  test("an edge shared by several bricks is counted once for each"):
    val (edge, shared) = Records.sharedEdges(bricks, 1).head
    assert(shared.size >= 2 && shared.forall(b => Set(b.a: Long, b.b, b.c)(edge)), shared)
    assert(bricks.count(b => Set(b.a: Long, b.b, b.c)(edge)) == shared.size)

  test("the bands of legs cover every smallest edge, and every brick, once"):
    val rows = Records.byLegs(bricks, 3000)
    assertEquals(rows.map(_.edges).sum, 3000)
    assertEqualsDouble(rows.map(r => r.bricks * r.edges).sum, bricks.size.toDouble, 1e-6)

  test("the flattest and the most cube-like brick are the extremes of longest over smallest"):
    val lines = Records.lines(bricks, "up to 3000")
    val flattest = bricks.filter(_.isPrimitive).maxBy(Records.flatness)
    assert(lines.exists(_.startsWith(s"Flattest, longest edge over smallest: ${flattest.a} × ")), lines)
