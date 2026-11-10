package perfectcuboid.speed

class GuardTest extends munit.FunSuite:

  test("the reference counts the pairs a < b with a² + b² a square"):
    // Up to 20: 3·4, 6·8, 5·12, 9·12, 8·15, 12·16, 15·20.
    assertEquals(Guard.reference(20), 7L)

  test("the timed search finds what the search finds"):
    assertEquals(Guard.search(1, 1000), perfectcuboid.FastSearch.eulerBricksBetween(1, 1000).size.toLong)
