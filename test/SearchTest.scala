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

  test("the unbounded search starts with the smallest Euler brick"):
    assertEquals(Search.allEulerBricks.head, Cuboid.of(44, 117, 240).get)

  test("the unbounded search agrees with the bounded one inside its range"):
    val limit = 5000L
    val unbounded = Search.allEulerBricks.takeWhile(_.a <= limit).filter(_.c <= limit)
    assertEquals(unbounded.sorted.toVector, Search.eulerBricks(limit))

  test("the unbounded search goes in order of smallest edge, and finds bricks beyond any bound"):
    val bricks = Search.allEulerBricks.takeWhile(_.a <= 1000).toList
    assertEquals(bricks.map(_.a), bricks.map(_.a).sorted)
    assert(bricks.exists(_.c > 30 * 1000)) // 975 × 3740 × 31680: far past the smallest edge bound
    assert(bricks.forall(_.isEuler))

  test("the command line streams the unbounded search"):
    val lines = Cli.run(Report.Unbounded(100))
    assert(lines.exists(_.contains("44 × 117 × 240")))
    assert(lines.last.startsWith("Done"))
