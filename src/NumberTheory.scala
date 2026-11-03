package perfectcuboid

import scala.annotation.tailrec

/** Prime factorisation and divisors. */
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

  /** The i-th number to try as a factor: the small primes, then every odd number after them. */
  private def candidate(i: Int): Long =
    if i < smallPrimes.length then smallPrimes(i)
    else smallPrimes.last + 2L * (i - smallPrimes.length + 1)

  /** Factorise by trial division: divide out each prime in turn, up to the square root.
    * Fast for numbers up to 10¹², whose square root is a million; correct for any Long.
    */
  def factorize(n: Long): Factorization =
    require(n >= 1, s"can only factorise positive numbers, not $n")

    // How many times p divides m, and what is left after dividing it out.
    @tailrec def divideOut(m: Long, p: Long, times: Int): (Long, Int) =
      if m % p == 0 then divideOut(m / p, p, times + 1) else (m, times)

    @tailrec def loop(m: Long, i: Int, found: Factorization): Factorization =
      val p = candidate(i)
      if m == 1 then found.reverse
      else if p > m / p then ((m -> 1) :: found).reverse // no factor up to √m: m is prime
      else
        divideOut(m, p, 0) match
          case (_, 0)        => loop(m, i + 1, found)
          case (rest, times) => loop(rest, i + 1, (p -> times) :: found)

    loop(n, 0, Nil)

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
