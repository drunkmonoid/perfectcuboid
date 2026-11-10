package perfectcuboid

import munit.ScalaCheckSuite
import org.scalacheck.{Gen, Prop}

class PrimalityTest extends ScalaCheckSuite:

  val oddModulus: Gen[Long] = Gen.chooseNum(3L, Long.MaxValue).map(m => if m % 2 == 0 then m - 1 else m)
  val anyLong: Gen[Long] = Gen.chooseNum(Long.MinValue, Long.MaxValue)

  property("mulMod agrees with BigInt arithmetic, even for the largest numbers"):
    Prop.forAll(oddModulus, anyLong, anyLong): (m, a, b) =>
      Montgomery(m).mulMod(a, b) == (BigInt(a) * b).mod(m).toLong

  property("powMod agrees with BigInt.modPow"):
    Prop.forAll(oddModulus, anyLong, Gen.chooseNum(0L, Long.MaxValue)): (m, a, e) =>
      Montgomery(m).powMod(a, e) == BigInt(a).modPow(e, m).toLong

  test("the edge cases of Montgomery arithmetic"):
    val m = Long.MaxValue // the largest modulus there is
    assertEquals(Montgomery(m).mulMod(m - 1, m - 1), 1L) // (−1)² = 1
    assertEquals(Montgomery(3).powMod(2, 0), 1L)
    assertEquals(Montgomery(3).mulMod(-1, 1), 2L)
    intercept[IllegalArgumentException](Montgomery(10))

  test("isPrime agrees with the sieve below a million"):
    val primes = NumberTheory.smallPrimes.toSet
    (0L until 1_000_000L).foreach(n => assertEquals(NumberTheory.isPrime(n), primes(n), n))

  property("isPrime agrees with Java's probabilistic test on random Longs"):
    Prop.forAll(Gen.chooseNum(1L, Long.MaxValue)): n =>
      NumberTheory.isPrime(n) == BigInt(n).isProbablePrime(100)

  test("composites that fool weaker tests"):
    List(
      561L, // the smallest Carmichael number: passes Fermat's test for every base coprime to it
      2047L, // the smallest strong pseudoprime to base 2: 23 · 89
      3215031751L, // a strong pseudoprime to bases 2, 3, 5 and 7
      3825123056546413051L, // a strong pseudoprime to every prime base up to 23
      Long.MaxValue // 2⁶³ − 1 = 7² · 73 · 127 · 337 · 92737 · 649657
    ).foreach(n => assert(!NumberTheory.isPrime(n), n))

  test("large primes"):
    List(
      2305843009213693951L, // 2⁶¹ − 1, a Mersenne prime
      9223372036854775783L // the largest prime below 2⁶³
    ).foreach(n => assert(NumberTheory.isPrime(n), n))

  property("primes near the top of the range, found by trying until one passes"):
    Prop.forAll(Gen.chooseNum(Long.MaxValue / 2, Long.MaxValue - 1000)): start =>
      val prime = Iterator.iterate(start)(_ + 1).find(NumberTheory.isPrime).get
      BigInt(prime).isProbablePrime(100)
