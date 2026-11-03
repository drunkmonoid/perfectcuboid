package perfectcuboid

/** Where primitive Euler bricks come from: which ones the formulas and maps of the earlier
  * phases produce from something simpler, and which ones only a search finds.
  */
object Origins:

  /** The cube root of n, if it is a whole number. */
  def exactCbrt(n: BigInt): Option[BigInt] =
    val guess = BigInt(math.round(math.cbrt(n.toDouble)))
    (guess - 1 to guess + 1).find(r => r * r * r == n)

  /** Whether the brick has the shape of one of Saunderson's.
    *
    * Saunderson's brick from (u, v, w) has edges u·|4v² − w²| and v·|4u² − w²| whose face
    * diagonal is w³: u²(4v² − w²)² + v²(4u² − w²)² = w⁶. So look for a face diagonal that is a
    * cube w³, try each triple with hypotenuse w, and compare shapes.
    */
  def isSaunderson(brick: BigCuboid): Boolean =
    val primitive = brick.primitive
    val edges = List(brick.a, brick.b, brick.c)
    val cubes =
      for
        List(x, y) <- edges.combinations(2).toList
        d <- summon[WholeNumber[BigInt]].exactSqrt(x * x + y * y).toList
        w <- exactCbrt(d).toList
      yield w
    cubes.exists: w =>
      (BigInt(1) until w).exists: u =>
        summon[WholeNumber[BigInt]]
          .exactSqrt(w * w - u * u)
          .exists: v =>
            v > 0 && Families.saundersonBig(Triple(u.toLong, v.toLong, w.toLong)).primitive == primitive

  /** The bricks from 2P, …, mP on each of the brick's three curves. */
  def curveMultiples(brick: BigCuboid, maxMultiple: Int): List[BigCuboid] =
    val List(a, b, c) = List(brick.a, brick.b, brick.c): @unchecked
    List((a, b, c), (a, c, b), (b, c, a)).flatMap: (x, y, third) =>
      val curve = EulerCurve(x, y)
      curve.pointOf(third).toList.flatMap(p => curve.multiples(p).slice(1, maxMultiple).flatMap(curve.brickOf))

  /** How one brick comes about. */
  final case class Origin(saunderson: Boolean, derivedFromSmaller: Boolean, curveMultiple: Boolean):
    def any: Boolean = saunderson || derivedFromSmaller || curveMultiple

  /** The origin of each primitive brick in the list. A brick is derived from a smaller one when
    * its derived partner has a shorter longest edge, and a curve multiple when it is 2P, 3P or 4P
    * for a point of another brick in the list.
    */
  def classify(bricks: Vector[BigCuboid]): Vector[(BigCuboid, Origin)] =
    val fromCurves = bricks.flatMap(curveMultiples(_, 4)).toSet
    bricks.map: brick =>
      brick -> Origin(
        isSaunderson(brick),
        Ordering[BigCuboid].lt(Derived.derived(brick), brick),
        fromCurves(brick)
      )
