package perfectcuboid

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import NumberTheory.*

class NumberTheoryTest extends munit.ScalaCheckSuite:

  test("examples"):
    assertEquals(factorize(1), Nil)
    assertEquals(factorize(360), List(2L -> 3, 3L -> 2, 5L -> 1))
    assertEquals(factorize(1_000_000_007), List(1_000_000_007L -> 1))
    assertEquals(divisors(12), List(1L, 2, 3, 4, 6, 12))

  property("factorize gives increasing primes whose product is the number"):
    forAll(Gen.chooseNum(1L, 1_000_000_000_000L)): n =>
      val factors = factorize(n)
      val primes = factors.map(_._1)
      value(factors) == n && primes == primes.sorted.distinct && primes.forall(isPrimeSlowly)

  test("divisors agree with checking every candidate"):
    for n <- 1L to 2000L do assertEquals(divisors(n), (1L to n).filter(n % _ == 0).toList)

  test("the legs paired with 240 include the other edges of two Euler bricks"):
    val legs = Pythagorean.legsPairedWith(240)
    assert(List(44L, 117L, 252L, 275L).forall(legs.contains), legs)

  property("every leg returned really makes a Pythagorean triple"):
    forAll(Gen.chooseNum(1L, Edge.Max)): a =>
      Pythagorean
        .legsPairedWith(a)
        .forall: y =>
          val z = BigInt(a).pow(2) + BigInt(y).pow(2)
          Arith.isqrtBig(z).pow(2) == z

  test("legsPairedWith finds exactly the partners the old method finds"):
    val limit = 3000L
    val partners = Pythagorean.partners(limit)
    for a <- 1L to limit do
      val fromFactors = Pythagorean.legsPairedWith(a).filter(_ <= limit).toSet
      assertEquals(fromFactors, partners.getOrElse(a, Set.empty), s"a = $a")

  // Independent of factorize, so it can check it.
  def isPrimeSlowly(p: Long): Boolean =
    p >= 2 && (2L to math.sqrt(p.toDouble).toLong + 1).forall(d => d == p || p % d != 0)
