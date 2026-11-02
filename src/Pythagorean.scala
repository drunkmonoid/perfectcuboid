package perfectcuboid

import Arith.*

object Pythagorean:

  /** Every pair of legs (x, y) with x < y <= limit and x² + y² a perfect square.
    *
    * Uses Euclid's formula: for coprime m > n > 0 of opposite parity, the legs
    * m² - n² and 2mn form a primitive triple, and every primitive triple arises exactly
    * once this way. Multiplying both legs by k = 1, 2, 3, … gives all the others.
    */
  def legPairs(limit: Long): Vector[(Long, Long)] =
    // Smallest n keeping the leg m² - n² within the limit.
    def smallestN(m: Long): Long =
      if square(m) <= limit then 1L else ceilSqrt(square(m) - limit)

    // A for-comprehension: nested loops over m, n and k, with `if` filters,
    // collecting a value for every combination that gets through.
    val pairs =
      for
        m <- 2L to limit / 2
        n <- smallestN(m) to math.min(m - 1, limit / (2 * m))
        if (m - n) % 2 == 1 && gcd(m, n) == 1
        x = square(m) - square(n)
        y = 2 * m * n
        k <- 1L to limit / math.max(x, y)
      yield (k * math.min(x, y), k * math.max(x, y))
    pairs.toVector

  /** Every y with a² + y² a perfect square, smallest first. There are always finitely many.
    *
    * If a² + y² = z², then a² = (z − y)(z + y). So each answer comes from writing a² = d·e
    * with d < e, where d and e are both odd or both even; then y = (e − d) / 2. The
    * divisors of a² come straight from the factorisation of a, with every exponent doubled.
    */
  def legsPairedWith(a: Long): List[Long] =
    val aSquared = square(a)
    val factorsOfSquare = NumberTheory.factorize(a).map((p, k) => p -> 2 * k)
    val legs =
      for
        d <- NumberTheory.divisors(factorsOfSquare)
        if d < a
        e = aSquared / d
        if (e - d) % 2 == 0
      yield (e - d) / 2
    legs.sorted

  /** For each leg up to `limit`, the set of other legs it forms a Pythagorean triple with. */
  def partners(limit: Long): Map[Long, Set[Long]] =
    legPairs(limit)
      .flatMap((x, y) => List(x -> y, y -> x))
      .groupMap(_._1)(_._2)
      .map((leg, others) => leg -> others.toSet)
