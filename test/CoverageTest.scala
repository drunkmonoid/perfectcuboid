package perfectcuboid

class CoverageTest extends munit.FunSuite:

  val catalogue: Set[BigCuboid] =
    FastSearch.eulerBricksBetween(1, 100_000).filter(_.isPrimitive).map(BigCuboid.from).toSet

  test("Saunderson's family starts with the smallest brick, from (3, 4, 5)"):
    val saunderson = Coverage.families(100).head
    assert(saunderson.members.contains(BigCuboid.from(Cuboid.of(44, 117, 240).get)))

  test("every family member with edges up to 10⁵ is among the bricks the search finds"):
    val lines = Coverage.lines(catalogue, 100_000, 1000)
    assertEquals(lines.last, "Every member in range was found by the search.")
    val rows = Coverage.rows(Coverage.families(1000), catalogue, 100_000)
    assert(rows.forall(r => r.found == r.inRange), rows)
    assert(rows.last.inRange >= rows.head.inRange)

  test("a member the search did not find would be named"):
    val lines = Coverage.lines(catalogue - BigCuboid.from(Cuboid.of(44, 117, 240).get), 100_000, 100)
    assert(lines.contains("  44 × 117 × 240"), lines)
