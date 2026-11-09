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
    val expected = Bench.baseline.search(2000)
    Bench.others.foreach((name, search) => assertEquals(search(2000), expected, name))

  test("a scaling row compares its speed with the fewest threads, and perfect scaling"):
    val one = Scaling.Row(1, 1_000_000, 32.0)
    val twelve = Scaling.Row(12, 1_000_000, 8.0)
    assertEquals(Scaling.line(one, one), "   1 thread     32.00 s     31,250 edges/s   1.00x  (100% of perfect)")
    assertEquals(Scaling.line(twelve, one), "  12 threads     8.00 s    125,000 edges/s   4.00x  ( 33% of perfect)")
    assertEquals(Scaling.chart(List(one, twelve)).split("<circle").length - 1, 4)

  test("a scaling run times every thread count it is given"):
    import cats.effect.unsafe.implicits.global
    val rows = Scaling.run(1, 2000, 500, List(2, 1)).unsafeRunSync()
    assertEquals(rows.map(_.threads), List(1, 2))
    assert(rows.forall(_.seconds > 0))
