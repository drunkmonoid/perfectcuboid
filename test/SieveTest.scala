package perfectcuboid

import munit.ScalaCheckSuite
import org.scalacheck.{Gen, Prop}

class SieveTest extends ScalaCheckSuite:

  def agreesWithTrialDivision(first: Long, last: Long): Unit =
    val sieved = Sieve.factorizeBlock(first, last)
    (first to last).foreach(n => assertEquals(sieved((n - first).toInt), NumberTheory.factorize(n), n))

  test("the first numbers, including 1, which has no prime factors"):
    agreesWithTrialDivision(1, 10_000)
    assertEquals(Sieve.factorizeBlock(1, 1).toList, List(Nil))

  test("blocks far from the start"):
    agreesWithTrialDivision(1_000_000_000, 1_000_010_000)
    agreesWithTrialDivision(Edge.Max - 10_000, Edge.Max)
    agreesWithTrialDivision(Sieve.MaxLast - 1000, Sieve.MaxLast)

  property("any block within range"):
    val block = for
      first <- Gen.chooseNum(1L, Sieve.MaxLast - 500)
      length <- Gen.chooseNum(0L, 500L)
    yield (first, first + length)
    Prop.forAll(block): (first, last) =>
      Sieve.factorizeBlock(first, last).toList == (first to last).map(NumberTheory.factorize).toList

  test("blocks beyond the small primes are refused"):
    val _ = intercept[IllegalArgumentException](Sieve.factorizeBlock(1, Sieve.MaxLast + 1))
    intercept[IllegalArgumentException](Sieve.factorizeBlock(10, 9))
