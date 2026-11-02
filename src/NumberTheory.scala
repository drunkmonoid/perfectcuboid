package perfectcuboid

import scala.annotation.tailrec

/** Prime factorisation and divisors. */
object NumberTheory:

  /** A number written as primes and their exponents, smallest prime first:
    * 360 = 2³·3²·5 is `List(2 -> 3, 3 -> 2, 5 -> 1)`. A type alias is just a shorter name.
    */
  type Factorization = List[(Long, Int)]

  /** Factorise by trial division: divide out 2, then 3, 5, 7, 9, … up to the square root.
    * Plenty fast for numbers up to about 10¹², whose square root is 10⁶.
    */
  def factorize(n: Long): Factorization =
    require(n >= 1, s"can only factorise positive numbers, not $n")

    // How many times p divides m, and what is left after dividing it out.
    @tailrec def divideOut(m: Long, p: Long, times: Int): (Long, Int) =
      if m % p == 0 then divideOut(m / p, p, times + 1) else (m, times)

    @tailrec def loop(m: Long, p: Long, found: Factorization): Factorization =
      if m == 1 then found.reverse
      else if p > m / p then ((m -> 1) :: found).reverse // no factor up to √m: m is prime
      else
        val next = if p == 2 then 3 else p + 2
        divideOut(m, p, 0) match
          case (_, 0)        => loop(m, next, found)
          case (rest, times) => loop(rest, next, (p -> times) :: found)

    loop(n, 2, Nil)

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

  def isPrime(n: Long): Boolean = n >= 2 && factorize(n) == List(n -> 1)

  private def power(p: Long, k: Int): Long = List.fill(k)(p).product
