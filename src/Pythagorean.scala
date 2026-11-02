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

  /** For each leg up to `limit`, the set of other legs it forms a Pythagorean triple with. */
  def partners(limit: Long): Map[Long, Set[Long]] =
    legPairs(limit)
      .flatMap((x, y) => List(x -> y, y -> x))
      .groupMap(_._1)(_._2)
      .map((leg, others) => leg -> others.toSet)
