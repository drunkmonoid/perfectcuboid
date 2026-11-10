package perfectcuboid

class FiniteFieldsTest extends munit.FunSuite:

  /** Every (a, b, c, x, y, z, w) mod p, tried one by one. */
  def bruteForce(p: Int): (all: Long, noZero: Long) =
    val sq = Array.tabulate(p)(x => x * x % p)
    var all = 0L
    var noZero = 0L
    for a <- 0 until p; b <- 0 until p; c <- 0 until p; x <- 0 until p; y <- 0 until p; z <- 0 until p; w <- 0 until p
    do
      if (sq(a) + sq(b)) % p == sq(x) && (sq(a) + sq(c)) % p == sq(y) && (sq(b) + sq(c)) % p == sq(z) &&
        (sq(a) + sq(b) + sq(c)) % p == sq(w)
      then
        all += 1
        if List(a, b, c, x, y, z, w).forall(_ != 0) then noZero += 1
    (all = all, noZero = noZero)

  test("counting by square roots gives what trying every one of the p⁷ choices gives"):
    for p <- List(3, 5, 7) do
      val c = FiniteFields.count(p)
      assertEquals((all = c.all, noZero = c.noZero), bruteForce(p), s"p = $p")

  test("the formulas give the count for every odd prime up to 200"):
    for p <- NumberTheory.smallPrimes.takeWhile(_ <= 200).map(_.toInt).filter(_ > 2) do
      assertEquals(FiniteFields.predicted(p), FiniteFields.count(p).all, s"p = $p")

  test("no solution without a zero mod 3, 5, 7, 11, 13, 17, 19, 29 and 37, but some mod 23 and 31"):
    val none = List(3, 5, 7, 11, 13, 17, 19, 29, 37)
    none.foreach(p => assertEquals(FiniteFields.count(p).noZero, 0L, s"p = $p"))
    List(23, 31, 41, 43).foreach(p => assert(FiniteFields.count(p).noZero > 0, s"p = $p"))

  test("primes as sums of squares"):
    assertEquals(FiniteFields.form(13, 1), Some((x = 3L, y = 2L)))
    assertEquals(FiniteFields.form(11, 2), Some((x = 3L, y = 1L)))
    assertEquals(FiniteFields.form(7, 2), None)
