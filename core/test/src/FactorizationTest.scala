package perfectcuboid

import munit.ScalaCheckSuite
import org.scalacheck.{Gen, Prop}

class FactorizationTest extends ScalaCheckSuite:

  /** A correct factorisation: primes in increasing order, multiplying back to n. */
  def isFactorizationOf(n: Long, factors: NumberTheory.Factorization): Boolean =
    val primes = factors.map(_._1)
    primes.forall(NumberTheory.isPrime) && primes == primes.distinct.sorted && factors.forall(_._2 >= 1) &&
    factors.map((p, k) => BigInt(p).pow(k)).product == n

  def nextPrime(n: Long): Long = Iterator.iterate(n)(_ + 1).find(NumberTheory.isPrime).get

  property("any Long factorises correctly"):
    Prop.forAll(Gen.chooseNum(1L, Long.MaxValue))(n => isFactorizationOf(n, NumberTheory.factorize(n)))

  /** The hardest case for rho: two primes of about the same size, each near 2³¹. */
  property("products of two large primes"):
    val prime = Gen.chooseNum(1L << 30, (1L << 31) - 1000).map(nextPrime)
    Prop.forAll(prime, prime): (p, q) =>
      NumberTheory.factorize(p * q) == (if p == q then List(p -> 2) else List(p.min(q) -> 1, p.max(q) -> 1))

  property("rho finds a proper factor of a composite"):
    val prime = Gen.chooseNum(70_000L, 1_000_000_000L).map(nextPrime)
    Prop.forAll(prime, prime): (p, q) =>
      val d = PollardRho.factor(p * q)
      d > 1 && d < p * q && (p * q) % d == 0

  test("numbers that are hard for one method or another"):
    List(
      1L,
      2L,
      65_537L * 65_537L, // the square of the first prime past trial division
      4_294_967_291L * 3L, // the largest prime below 2³², times 3
      1_000_000_007L * 1_000_000_009L, // two primes just past 10⁹
      3_037_000_493L * 3_037_000_493L, // the square of a prime near √(2⁶³)
      Long.MaxValue,
      2_305_843_009_213_693_951L, // 2⁶¹ − 1, prime
      614_889_782_588_491_410L // the first 15 primes multiplied
    ).foreach(n => assert(isFactorizationOf(n, NumberTheory.factorize(n)), n))

  test("the factorisation of 2⁶³ − 1"):
    assertEquals(
      NumberTheory.factorize(Long.MaxValue),
      List(7L -> 2, 73L -> 1, 127L -> 1, 337L -> 1, 92737L -> 1, 649657L -> 1)
    )

  test("legs of numbers too big for a Long square match the definition"):
    val a = 3825123056546413051L
    val legs = Pythagorean.legsPairedWithAny(a, NumberTheory.factorize(a))
    assertEquals(legs.size, 13) // (27 divisors of a² − 1) / 2
    legs.foreach(y => assert(Arith.isqrtBig(BigInt(a).pow(2) + y.pow(2)).pow(2) == BigInt(a).pow(2) + y.pow(2), y))

  test("for small legs, the BigInt version agrees with the Long one"):
    (1L to 2000L).foreach(a =>
      assertEquals(
        Pythagorean.legsPairedWithAny(a, NumberTheory.factorize(a)),
        Pythagorean.legsPairedWith(a).map(BigInt(_))
      )
    )
