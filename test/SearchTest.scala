package perfectcuboid

class SearchTest extends munit.FunSuite:

  test("every leg pair really is Pythagorean, and none is missing"):
    val limit = 500L
    val expected =
      for
        x <- 1L to limit
        y <- x + 1 to limit
        if Arith.isPerfectSquare(Arith.sumOfSquares(x, y))
      yield (x, y)
    assertEquals(Pythagorean.legPairs(limit).sorted, expected.toVector)

  test("the fast Euler brick search matches brute force"):
    assertEquals(Search.eulerBricks(2000), Search.eulerBricksBruteForce(2000))

  test("the smallest Euler bricks"):
    val found = Search.eulerBricks(300).map(c => (c.a, c.b, c.c))
    assertEquals(found, Vector((44L, 117L, 240L), (240L, 252L, 275L)))

  test("no perfect cuboid with small edges"):
    assertEquals(Search.perfectCuboids(10_000), Vector.empty)
