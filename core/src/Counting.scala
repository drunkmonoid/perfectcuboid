package perfectcuboid

/** How many Pythagorean triples a number belongs to, read off its prime factorisation
  * without listing them. Each formula is checked against the enumerations in the tests.
  *
  * All of them take the factorisation, so a number is factorised once for all the questions.
  */
object Counting:

  /** The primes ≡ 1 (mod 4) are exactly the odd primes that are sums of two squares (Fermat). */
  private def isOneMod4(p: Long): Boolean = p % 4 == 1

  /** In how many triples a² + y² = z² the number a is a leg.
    *
    * Each one comes from a factor pair d·e = a² with d < e of the same parity (see
    * `Pythagorean.completions`). For odd a every divisor is odd, so the pairs number
    * (d(a²) − 1) / 2, where d counts divisors and the − 1 drops d = e = a. For even a both
    * factors must be even, so they are 2d' · 2e' with d'·e' = (a/2)², and the same count
    * applies to (a/2)².
    */
  def asLeg(factors: NumberTheory.Factorization): Long =
    // The factors of a², with 2 taken out twice if a is even: then this is (a/2)².
    val counted = factors.map((p, k) => if p == 2 then p -> (2 * k - 2) else p -> 2 * k)
    (NumberTheory.divisorCount(counted).toLong - 1) / 2

  /** In how many triples x² + y² = n² (x < y) the number n is the hypotenuse.
    *
    * Writing n² as a sum of two squares can only use the primes ≡ 1 (mod 4): each p^k in n
    * contributes 2k + 1 choices. Their product counts the ways with x, y >= 0 in order, one of
    * which is 0² + n², and every other one comes twice, as (x, y) and (y, x).
    */
  def asHypotenuse(factors: NumberTheory.Factorization): Long =
    (factors.collect { case (p, k) if isOneMod4(p) => 2L * k + 1 }.product - 1) / 2

  /** r₂(n): the number of ways to write n as x² + y² with x and y any integers, signs and
    * order counted, so 5 = (±1)² + (±2)² = (±2)² + (±1)² counts 8 times.
    *
    * It is 0 when a prime ≡ 3 (mod 4) divides n an odd number of times, and otherwise
    * 4 · ∏ (k + 1) over the primes p^k ≡ 1 (mod 4) (Jacobi, 1829).
    */
  def sumsOfTwoSquares(factors: NumberTheory.Factorization): Long =
    if factors.exists((p, k) => p % 4 == 3 && k % 2 == 1) then 0
    else 4 * factors.collect { case (p, k) if isOneMod4(p) => k + 1L }.product

  /** In how many primitive triples a is a leg. The even leg of a primitive triple is 2mn with
    * m, n coprime and of opposite parity, so it is a multiple of 4; the odd leg is m² − n². Either
    * way, each distinct prime of a can go to one of two factors, which gives 2^(ω − 1) triples,
    * where ω counts the distinct primes.
    */
  def asPrimitiveLeg(a: Long, factors: NumberTheory.Factorization): Long =
    if a == 1 || a % 4 == 2 then 0 else 1L << (factors.size - 1)

  /** In how many primitive triples n is the hypotenuse: 2^(ω − 1) when every prime factor of
    * n is ≡ 1 (mod 4), and none otherwise.
    */
  def asPrimitiveHypotenuse(n: Long, factors: NumberTheory.Factorization): Long =
    if n == 1 || !factors.forall((p, _) => isOneMod4(p)) then 0 else 1L << (factors.size - 1)
