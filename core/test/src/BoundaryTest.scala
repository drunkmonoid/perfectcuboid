package perfectcuboid

/** The cases that mutation testing (phase 76) showed no test reached: each test here fails for
  * a change to the code that every other test let through.
  */
class BoundaryTest extends munit.FunSuite:

  test("isqrtBig stops at the root even where Newton's method would go back up"):
    // For n = r(r + 2), one step past the root goes back up to r + 1, so the descent must stop
    // when it stops going down, not only when it stands still.
    for n <- 0L to 100_000L do assertEquals(Arith.isqrtBig(BigInt(n)), BigInt(Arith.isqrt(n)), s"n = $n")
    assertEquals(Arith.isqrtBig(BigInt(8)), BigInt(2))

  test("a pair whose squares' remainders add up to exactly the filter's modulus is still tried"):
    // b² + c² ≡ 0 (mod 13·47·29): the sum of the remainders is 17,719 itself, which the filter
    // must bring down to 0. Taking it as is would rule this brick out.
    val (b, c) = (2023632L, 2844205L)
    assertEquals((b * b % 17719) + (c * c % 17719), 17719L)
    assert(FastSearch.eulerBricksWithSmallestEdge(2018940).contains(Cuboid(2018940, 2023632, 2844205)))

  test("Wide numbers from 0 to the largest, and no further"):
    assertEquals(Wide(0L).toBigInt, BigInt(0))
    val max = BigInt(Long.MaxValue).pow(2)
    assertEquals(Wide((1L << 62) - 1, 1L).toBigInt, max)
    assertEquals(Wide((1L << 62) - 1, 0L).toBigInt, max - 1)
    intercept[IllegalArgumentException](Wide((1L << 62) - 1, 2L)): Unit
    intercept[IllegalArgumentException](Wide(-1L))

  test("Wide products: of 0, and refused when either factor is past a Long"):
    val n = summon[WholeNumber[Wide]]
    assertEquals(n.times(Wide(0L), Wide(5L)), Wide(0L))
    assertEquals(n.times(Wide(5L), Wide(0L)), Wide(0L))
    assertEquals(n.times(Wide(3L), Wide(4L)), Wide(12L))
    for (x, y) <- List(
        (Wide(1L, 0L), Wide(2L)),
        (Wide(2L), Wide(1L, 0L)),
        (Wide(0L, -1L), Wide(2L)), // lo read as unsigned: 2⁶⁴ − 1
        (Wide(2L), Wide(0L, -1L))
      )
    do intercept[ArithmeticException](n.times(x, y)): Unit

  test("Montgomery arithmetic refuses a modulus of 1, and gives 0 for products that are multiples"):
    intercept[IllegalArgumentException](Montgomery(1)): Unit
    assertEquals(Montgomery(15).mulMod(3, 5), 0L)
    assertEquals(Montgomery(15).mulMod(6, 10), 0L)
    assertEquals(Montgomery(9).powMod(3, 2), 0L)
    assertEquals(Montgomery(9).powMod(6, 4), 0L)

  test("Pollard's rho refuses what is not an odd composite above 3"):
    intercept[IllegalArgumentException](PollardRho.factor(3)): Unit
    intercept[IllegalArgumentException](PollardRho.factor(10)): Unit
    val d = PollardRho.factor(9)
    assertEquals(d, 3L)

  test("the pairs of legs up to a limit are exactly the right triangles' legs, all positive"):
    val limit = 300L
    val brute =
      for x <- 1L to limit; y <- x + 1 to limit if Arith.isPerfectSquare(x * x + y * y) yield (x, y)
    assertEquals(Pythagorean.legPairs(limit).sorted, brute.toVector)
