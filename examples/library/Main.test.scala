import perfectcuboid.{Kind, PerfectCuboid}

class LibraryTest extends munit.FunSuite:

  test("the library tells boxes apart"):
    assertEquals(PerfectCuboid.kind(44, 117, 240), Some(Kind.Body))
    assertEquals(PerfectCuboid.kind(0, 117, 240), None)
    assert(!PerfectCuboid.isPerfect(44, 117, 240))

  test("the library searches"):
    val bricks = PerfectCuboid.primitiveEulerBricks(1, 1000)
    assertEquals(bricks.map(b => (b.a: Long, b.b: Long, b.c: Long)).take(2), List((44L, 117L, 240L), (85L, 132L, 720L)))
    assert(PerfectCuboid.legs(240).contains(44L))
    assertEquals(
      PerfectCuboid.primitiveEulerBricksByOddEdge(1, 1000).toSet,
      PerfectCuboid
        .primitiveEulerBricks(1, 1000)
        .filter(b => List(b.a: Long, b.b, b.c).exists(e => e % 2 == 1 && e <= 1000))
        .toSet
    )
