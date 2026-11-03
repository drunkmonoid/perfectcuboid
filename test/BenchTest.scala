package perfectcuboid

class BenchTest extends munit.FunSuite:

  test("the median is the middle run, or the mean of the middle two"):
    assertEquals(Timing(Vector(3.0, 1.0, 2.0)).median, 2.0)
    assertEquals(Timing(Vector(4.0, 1.0, 3.0, 2.0)).median, 2.5)
    assertEquals(Timing(Vector(5.0)).median, 5.0)

  test("one slow run moves the median far less than the mean"):
    val timing = Timing(Vector(1.0, 1.1, 0.9, 1.0, 9.0))
    assertEquals(timing.median, 1.0)
    assertEquals((timing.min, timing.max), (0.9, 9.0))

  test("a timing needs at least one run"):
    intercept[IllegalArgumentException](Timing(Vector.empty))

  test("every search in the benchmark finds the same bricks"):
    val expected = Bench.baseline._2(2000)
    Bench.others.foreach((name, search) => assertEquals(search(2000), expected, name))
