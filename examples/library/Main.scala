import perfectcuboid.PerfectCuboid

/** Every primitive Euler brick with smallest edge up to 10,000, and how near each comes to perfect. */
@main def bricks(): Unit =
  val found = PerfectCuboid.primitiveEulerBricks(1, 10_000)
  println(s"${found.size} primitive Euler bricks with smallest edge up to 10,000:")
  found.foreach(b => println(f"  ${b.a} × ${b.b} × ${b.c}%-30s space diagonal off by ${PerfectCuboid.miss(b)}%.4f"))
  println(s"Is 44 × 117 × 240 perfect? ${PerfectCuboid.isPerfect(44, 117, 240)}")
