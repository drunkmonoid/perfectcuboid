package perfectcuboid

class ParallelepipedTest extends munit.FunSuite:

  test("Sawyer and Reiter's box has 13 whole lengths and is solid"):
    val box = Parallelepiped.sawyerReiter
    assert(box.isPerfect && box.isSolid && !box.isCuboid)
    assertEquals(box.faceDiagonalSquares.map(Arith.isqrt).sorted, List(101L, 183, 255, 266, 312, 323))
    assertEquals(box.bodyDiagonalSquares.map(Arith.isqrt).sorted, List(272L, 278, 300, 374))

  test("a number splits into two squares in every way there is"):
    assertEquals(Parallelepiped.twoSquares(50), List((7L, 1L), (5L, 5L)))
    assertEquals(Parallelepiped.twoSquares(3), Nil)

  test("an Euler brick is a parallelepiped at right angles, but not a perfect one"):
    val brick = Parallelepiped(240, 117, 44, 0, 0, 0)
    assert(brick.isCuboid && brick.isSolid && !brick.isPerfect)

  test("vectors in a line make no solid, and changing one angle spoils the body diagonals"):
    assert(!Parallelepiped(3, 4, 5, 2 * 3 * 4, 0, 0).isSolid) // u and v in a line
    assert(!Parallelepiped(271, 106, 103, -2 * 9826, 2 * 6647, 2 * 5822).isPerfect)

  test("the only perfect parallelepiped with edges up to 300 is Sawyer and Reiter's"):
    assertEquals(Parallelepiped.search(300), List(Parallelepiped.sawyerReiter))

  test("twice a perfect parallelepiped is one too, and says so"):
    val twice = Parallelepiped(542, 212, 206, 8 * 9826, 8 * 6647, 8 * 5822)
    assert(twice.isPerfect)
    assertEquals(twice.scaledFrom, Some((factor = 2L, from = Parallelepiped.sawyerReiter)))
    assertEquals(Parallelepiped.sawyerReiter.scaledFrom, None)

  test("sums of two squares from the factors are those found by trying every one"):
    for n <- 0L to 20_000L do assertEquals(Parallelepiped.twoSquares(n), Parallelepiped.twoSquaresByTrial(n), s"n = $n")

  test("and so for large numbers too"):
    val rng = scala.util.Random(83)
    for _ <- 1 to 200 do
      val n = rng.nextLong(1_000_000_000_000L) + 1
      assertEquals(Parallelepiped.twoSquares(n), Parallelepiped.twoSquaresByTrial(n), s"n = $n")

  test("searching in blocks finds what searching all at once does"):
    val whole = Parallelepiped.search(160)
    val blocks = List((1L, 40L), (41L, 97L), (98L, 160L)).flatMap(Parallelepiped.searchBetween)
    assertEquals(blocks, whole)
