package perfectcuboid

/** The Euler bricks that come closest to being perfect: those whose space diagonal is nearest
  * to a whole number.
  */
object ClosestCalls:

  /** How far √n is from the nearest whole number, for n >= 1.
    *
    * With r = ⌊√n⌋, √n − r = (n − r²) / (√n + r), and (r + 1) − √n = ((r + 1)² − n) / (r + 1 + √n).
    * The numerators are exact whole numbers, so the distance is accurate to a Double's 16 digits,
    * even when √n itself has 30 digits and the distance is tiny.
    */
  def distanceToWhole(n: BigInt): Double =
    val r = Arith.isqrtBig(n)
    val root = math.sqrt(n.toDouble)
    val below = (n - r * r).toDouble / (root + r.toDouble)
    val above = ((r + 1) * (r + 1) - n).toDouble / (root + r.toDouble + 1)
    math.min(below, above)

  /** How far the brick's space diagonal is from a whole number: 0 for a perfect cuboid. */
  def miss(brick: Cuboid): Double =
    distanceToWhole(BigInt(brick.a).pow(2) + BigInt(brick.b).pow(2) + BigInt(brick.c).pow(2))

  /** How far a face cuboid's one irrational face diagonal is from a whole number. */
  def faceMiss(cuboid: Cuboid): Double =
    val (a, b, c) = (BigInt(cuboid.a), BigInt(cuboid.b), BigInt(cuboid.c))
    List(a * a + b * b, a * a + c * c, b * b + c * c)
      .filterNot(Arith.isPerfectSquareBig)
      .map(distanceToWhole)
      .minOption
      .getOrElse(0.0)

  /** How far an edge cuboid's one irrational edge is from a whole number. */
  def edgeMiss(cuboid: SquaredCuboid): Double =
    List(cuboid.a2, cuboid.b2, cuboid.c2)
      .filterNot(Arith.isPerfectSquare)
      .map(n => distanceToWhole(BigInt(n)))
      .minOption
      .getOrElse(0.0)

  /** The near misses closest to perfect, `count` of them, and the lines that show them: how many
    * there are, the closest, and how close chance alone would put the closest of so many.
    */
  def nearLines[A](
      finds: Vector[A],
      what: String,
      miss: A => Double,
      show: A => String,
      count: Int = 12
  ): List[String] =
    val ranked = finds.map(f => (find = f, miss = miss(f))).sortBy(_.miss)
    val header = f"The primitive $what nearest to perfect, of ${finds.size}%,d:"
    val rows = ranked.take(count).toList.map(r => f"  ${show(r.find)}   off by ${r.miss}%.2e")
    val chance =
      f"Spread evenly between 0 and ½, the smallest of ${finds.size}%,d misses would be about ${0.5 / (finds.size + 1)}%.1e."
    (header :: rows) :+ chance

  /** The primitive bricks among these, closest first. */
  def ranked(bricks: Iterable[Cuboid]): Vector[(brick: Cuboid, miss: Double)] =
    bricks.iterator.filter(_.isPrimitive).map(b => (brick = b, miss = miss(b))).toVector.sortBy(_.miss)
