package perfectcuboid

/** Pollard's rho method (1975), with Brent's improvements (1980), for splitting a composite.
  *
  * Iterate x ↦ x² + c modulo n. Modulo an unknown prime factor p of n, the values can take
  * only p different values, so they soon repeat and then go round in a cycle, after about √p
  * steps (the birthday paradox). Two values x and y that are equal modulo p differ by a
  * multiple of p, so gcd(|x − y|, n) reveals p, unless they happen to be equal modulo n as well.
  *
  * The values trace the shape of the Greek letter ρ: a tail, then a loop.
  */
object PollardRho:

  /** A factor d of the odd composite n, with 1 < d < n. Tries c = 1, 2, 3, … until one works. */
  def factor(n: Long): Long =
    require(n > 3 && n % 2 == 1, s"rho needs an odd composite, not $n")
    val modular = Montgomery(n)
    Iterator.from(1).map(c => attempt(modular, n, c)).collectFirst { case Some(d) => d }.get

  /** How many differences are multiplied together before taking one gcd of the product.
    * A gcd costs far more than a multiplication, so this saves most of them.
    */
  private val Batch = 128

  /** One run of Brent's version. Instead of comparing every pair of values, it keeps x fixed
    * at the value reached after r = 1, 2, 4, 8, … steps and compares the next r values with it.
    * Once r passes the length of the cycle, the cycle's start is caught.
    *
    * The loops update local variables, for speed; nothing outside can see them.
    */
  private def attempt(modular: Montgomery, n: Long, c: Long): Option[Long] =
    def step(y: Long) = modular.squarePlus(y, c)
    var y = 2L
    var x = y
    var saved = y
    var product = 1L // in Montgomery form this is not "one", but any number coprime to n will do
    var g = 1L
    var r = 1
    while g == 1 do
      x = y
      for _ <- 1 to r do y = step(y)
      var k = 0
      while k < r && g == 1 do
        saved = y
        for _ <- 1 to math.min(Batch, r - k) do
          y = step(y)
          product = modular.times(product, math.abs(x - y))
        g = Arith.gcd(product, n)
        k += Batch
      r *= 2
    // The batch may have gone past the factor and multiplied in a multiple of n as well. If so,
    // step through the last batch again one value at a time.
    if g == n then
      g = 1
      while g == 1 do
        saved = step(saved)
        g = Arith.gcd(math.abs(x - saved), n)
    Option.when(g != n)(g)
