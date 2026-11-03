package perfectcuboid

class EtaTest extends munit.FunSuite:

  def sample(seconds: Double, upTo: Long) = Eta.Sample(seconds, upTo)

  test("the window keeps the last minute, and always two samples"):
    val samples = Vector(sample(0, 0), sample(30, 300), sample(70, 800))
    assertEquals(Eta.window(samples, sample(100, 1000)), Vector(sample(70, 800), sample(100, 1000)))
    assertEquals(Eta.window(Vector(sample(0, 0)), sample(500, 1000)), Vector(sample(0, 0), sample(500, 1000)))
    assertEquals(Eta.window(Vector.empty, sample(5, 10)), Vector(sample(5, 10)))

  test("the rate is over the window, and needs two moments to measure"):
    assertEquals(Eta.rate(Vector(sample(10, 100), sample(20, 600))), Some(50.0))
    assertEquals(Eta.rate(Vector(sample(10, 100))), None)

  test("time left"):
    assertEquals(Eta.secondsLeft(1000, 6000, 50), Some(100.0))
    assertEquals(Eta.secondsLeft(1000, 6000, 0), None)

  test("durations for people"):
    assertEquals(Eta.duration(42), "42 s")
    assertEquals(Eta.duration(754), "12 min 34 s")
    assertEquals(Eta.duration(3 * 3600 + 20 * 60), "3 h 20 min")
    assertEquals(Eta.duration(5 * 86400), "5 days")

  test("a progress line"):
    assertEquals(
      Eta.line(250_000, 1_000_000, 12_345, "Euler bricks", Some(5000)),
      "  searched up to 250,000 (25.0%), 12,345 Euler bricks, 5,000 per second, about 2 min 30 s left"
    )
    assertEquals(Eta.line(0, 100, 0, "face cuboids", None), "  searched up to 0 (0.0%), 0 face cuboids")
