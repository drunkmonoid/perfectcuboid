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

  /** The primitive bricks among these, closest first. */
  def ranked(bricks: Iterable[Cuboid]): Vector[(brick: Cuboid, miss: Double)] =
    bricks.iterator.filter(_.isPrimitive).map(b => (brick = b, miss = miss(b))).toVector.sortBy(_.miss)
