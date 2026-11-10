package perfectcuboid

/** The primitive Euler bricks found by their odd edge, instead of their smallest.
  *
  * Every primitive brick has exactly one odd edge (`Theory`): with two odd edges, the sum of
  * their squares would be 2 mod 4, which no square is. So searching every odd u finds every
  * primitive brick exactly once, at its odd edge, which can be its shortest, middle or longest.
  * The other two edges are legs that pair with u, from either side of it.
  *
  * The search covers a different region from the search by smallest edge: up to N it finds
  * the bricks whose odd edge is at most N, wherever their smallest edge is. It searches only
  * half the numbers, and an odd number has every divisor pair of u² as a leg: (e − d) / 2 is
  * whole whenever d·e = u² with both odd.
  */
object OddSearch:

  /** The primitive Euler bricks with odd edge u. */
  def primitiveBricksWithOddEdge(u: Long, factors: NumberTheory.Factorization): List[Cuboid] =
    require(u % 2 == 1, s"$u is not odd")
    FastSearch.eulerBricksWithLegs(u, FastSearch.legsOf(u, factors)).filter(_.isPrimitive)

  /** The same, factorising u. */
  def primitiveBricksWithOddEdge(u: Long): List[Cuboid] = primitiveBricksWithOddEdge(u, NumberTheory.factorize(u))

  /** The primitive Euler bricks with odd edge from `first` to `last`, by odd edge, factorised
    * together by the sieve.
    */
  def primitiveBricksIterator(first: Long, last: Long): Iterator[Cuboid] =
    val factors =
      if last <= Sieve.MaxLast then Sieve.factorizeBlock(first, last)
      else IArray.from((first to last).map(NumberTheory.factorize))
    (first to last).iterator
      .filter(_ % 2 == 1)
      .flatMap(u => primitiveBricksWithOddEdge(u, factors((u - first).toInt)))

  /** The odd edge of a primitive brick. */
  def oddEdge(brick: Cuboid): Long = List(brick.a: Long, brick.b, brick.c).find(_ % 2 != 0).getOrElse(0L)
