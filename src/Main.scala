package perfectcuboid

/** The program's entry point, and the only place with side effects (printing).
  * Everything it calls is pure.
  *
  * Run with `scala-cli run .`, or `scala-cli run . -- 5000` for a different limit.
  */
@main def explore(args: String*): Unit =
  val limit = args.headOption.flatMap(_.toLongOption).getOrElse(1000L)
  val bricks = Search.eulerBricks(limit)
  println(s"Euler bricks with every edge <= $limit: ${bricks.size} (* = primitive)")
  bricks.map(describe).foreach(println)
  println(verdict(bricks.filter(_.isPerfect)))

def describe(cuboid: Cuboid): String =
  val edges = s"${cuboid.a} × ${cuboid.b} × ${cuboid.c}"
  val faces = cuboid.faceDiagonals.flatten.mkString(", ")
  val mark = if cuboid.isPrimitive then "*" else " "
  f"$mark $edges%-20s face diagonals $faces"

def verdict(perfect: Vector[Cuboid]): String =
  if perfect.isEmpty then "No perfect cuboid (as expected)."
  else s"PERFECT CUBOID FOUND: ${perfect.mkString(", ")}"
