package perfectcuboid

import scala.annotation.tailrec

/** Primes, prime factorisation and divisors. */
object NumberTheory:

  /** A number written as primes and their exponents, smallest prime first:
    * 360 = 2³·3²·5 is `List(2 -> 3, 3 -> 2, 5 -> 1)`. A type alias is just a shorter name.
    */
  type Factorization = List[(Long, Int)]

  /** The primes below a million, by the sieve of Eratosthenes.
    *
    * The sieve crosses out multiples in a local mutable array, for speed. The array never
    * escapes: the result is an `IArray`, Scala's immutable array, so this stays pure.
    */
  val smallPrimes: IArray[Long] =
    val limit = 1_000_000
    val composite = new Array[Boolean](limit)
    for
      p <- 2 until 1000 // up to √limit
      if !composite(p)
      multiple <- p * p until limit by p
    do composite(multiple) = true
    IArray.from((2 until limit).filterNot(composite(_)).map(_.toLong))

  /** Trial division tries the primes below 2¹⁶. That fully factorises any number below
    * 2³² (no composite can avoid a prime factor up to its square root), which covers every
    * edge the searches look at, and leaves only large factors for Pollard's rho.
    */
  private val TrialPrimes: Int = smallPrimes.indexWhere(_ > 65_536)

  /** The prime factorisation of any positive Long.
    *
    * Small primes are divided out by trial division. What is left has only prime factors
    * above 2¹⁶: if it is prime (Miller–Rabin says so for certain), it is the last factor;
    * otherwise Pollard's rho splits it into two smaller numbers, and each is factorised in turn.
    */
  def factorize(n: Long): Factorization =
    require(n >= 1, s"can only factorise positive numbers, not $n")

    // How many times p divides m, and what is left after dividing it out.
    @tailrec def divideOut(m: Long, p: Long, times: Int): (Long, Int) =
      if m % p == 0 then divideOut(m / p, p, times + 1) else (m, times)

    // Divides out the trial primes from the i-th on, and returns what is left with the factors found.
    @tailrec def trial(m: Long, i: Int, found: Factorization): (Long, Factorization) =
      val p = smallPrimes(i)
      if m == 1 then (1, found.reverse)
      else if p > m / p then (1, ((m -> 1) :: found).reverse) // no factor up to √m: m is prime
      else if i == TrialPrimes then (m, found.reverse)
      else
        divideOut(m, p, 0) match
          case (_, 0)        => trial(m, i + 1, found)
          case (rest, times) => trial(rest, i + 1, (p -> times) :: found)

    // The prime factors of m, repeated as often as they divide it, unsorted.
    def large(m: Long): List[Long] =
      if m == 1 then Nil
      else if isPrime(m) then List(m)
      else
        val d = PollardRho.factor(m)
        large(d) ++ large(m / d)

    val (rest, small) = trial(n, 0, Nil)
    small ++ large(rest).groupMapReduce(identity)(_ => 1)(_ + _).toList.sorted

  /** The number a factorisation stands for. */
  def value(factors: Factorization): Long =
    factors.foldLeft(1L)((acc, pk) => acc * power(pk._1, pk._2))

  /** All divisors of the number with these prime factors, unsorted.
    *
    * Each divisor picks an exponent from 0 to k for every prime p^k, so we start from
    * `List(1)` and, prime by prime, multiply every divisor so far by p⁰, p¹, …, pᵏ.
    */
  def divisors(factors: Factorization): List[Long] =
    factors.foldLeft(List(1L)): (divisorsSoFar, pk) =>
      val (p, k) = pk
      for
        d <- divisorsSoFar
        i <- (0 to k).toList
      yield d * power(p, i)

  def divisors(n: Long): List[Long] = divisors(factorize(n)).sorted

  /** The same divisors as BigInts, for factorisations of numbers too big for a Long, like a². */
  def divisorsBig(factors: Factorization): List[BigInt] =
    factors.foldLeft(List(BigInt(1))): (divisorsSoFar, pk) =>
      val (p, k) = pk
      for
        d <- divisorsSoFar
        i <- (0 to k).toList
      yield d * BigInt(p).pow(i)

  /** How many divisors the number with these prime factors has: (k₁ + 1)(k₂ + 1)… */
  def divisorCount(factors: Factorization): BigInt = factors.map((_, k) => BigInt(k + 1)).product

  /** Whether n is prime, for any Long, by the Miller–Rabin test.
    *
    * Fermat: if n is prime then aⁿ⁻¹ ≡ 1 (mod n). Write n − 1 = d·2ˢ with d odd; then the
    * sequence aᵈ, a²ᵈ, a⁴ᵈ, …, aⁿ⁻¹ must reach 1 either straight away or right after −1, since
    * modulo a prime only ±1 square to 1. A composite n fails this for most bases a.
    *
    * For n below 2⁶⁴ it is enough to try the seven bases below: every composite fails for at
    * least one of them (Jim Sinclair, 2011), so the answer is certain, not just probable.
    */
  def isPrime(n: Long): Boolean =
    if n < 2 then false
    else
      smallPrimes.iterator.take(25).find(n % _ == 0) match // the primes below 100
        case Some(p) => n == p
        case None    => n < 100 * 100 || passesMillerRabin(n)

  private val MillerRabinBases = List(2L, 325L, 9375L, 28178L, 450775L, 9780504L, 1795265022L)

  private def passesMillerRabin(n: Long): Boolean =
    val modular = Montgomery(n)
    val s = java.lang.Long.numberOfTrailingZeros(n - 1)
    val d = (n - 1) >> s
    def probablyPrimeFor(a: Long): Boolean =
      val start = modular.powMod(a, d)
      start == 1 || start == n - 1 ||
      Iterator.iterate(start)(x => modular.mulMod(x, x)).slice(1, s).contains(n - 1)
    // A base that is a multiple of n says nothing, so it is skipped.
    MillerRabinBases.filter(_ % n != 0).forall(probablyPrimeFor)

  private def power(p: Long, k: Int): Long = List.fill(k)(p).product
