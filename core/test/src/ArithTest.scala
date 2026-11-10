package perfectcuboid

import Arith.*

class ArithTest extends munit.FunSuite:

  test("isqrt agrees with the definition for small numbers"):
    for n <- 0L to 100_000L do
      val r = isqrt(n)
      assert(r * r <= n && (r + 1) * (r + 1) > n, s"isqrt($n) = $r")

  test("isqrt is exact around squares too big for a Double to hold precisely"):
    for r <- List(94_906_265L, 3_037_000_000L, 3_037_000_499L) do
      val n = r * r
      assertEquals(isqrt(n), r)
      assertEquals(isqrt(n - 1), r - 1)
      if r < 3_037_000_499L then assertEquals(isqrt(n + 1), r)

  test("isqrt of Long.MaxValue"):
    assertEquals(isqrt(Long.MaxValue), 3_037_000_499L)

  test("exactSqrt"):
    assertEquals(exactSqrt(0), Some(0L))
    assertEquals(exactSqrt(144), Some(12L))
    assertEquals(exactSqrt(145), None)
    assertEquals(exactSqrt(-4), None)

  test("ceilSqrt"):
    assertEquals(ceilSqrt(16), 4L)
    assertEquals(ceilSqrt(17), 5L)

  test("gcd"):
    assertEquals(gcd(12, 18), 6L)
    assertEquals(gcd(-12, 18), 6L)
    assertEquals(gcd(7, 0), 7L)

  test("sumOfSquares throws on overflow instead of wrapping around"):
    intercept[ArithmeticException](sumOfSquares(3_000_000_000L, 3_000_000_000L))
