package perfectcuboid

/** Arithmetic modulo an odd number m below 2⁶³, for any operands, without overflow.
  *
  * The difficulty is a·b mod m: for big a and b the product a·b needs 126 bits, and a Long
  * has 64. `Math.multiplyHigh` gives the top 64 bits of the product, so the product itself is
  * easy to get as two Longs, but there is no instruction to divide a 128-bit number by m.
  *
  * Montgomery's trick (1985) avoids that division. Write R = 2⁶⁴ and keep every number x as
  * x·R mod m, its "Montgomery form". The product of two such numbers is x·y·R², and the one
  * division needed to get back to x·y·R is a division by R, which is just taking the top 64
  * bits. `redc` does that, after adding the multiple of m that makes the bottom 64 bits zero.
  *
  * Every value is created once and never changed, so this is as pure as `Arith`.
  */
final class Montgomery(val m: Long):
  require(m > 1 && m % 2 == 1, s"Montgomery arithmetic needs an odd modulus above 1, not $m")

  /** −m⁻¹ mod 2⁶⁴. Newton's method for inverses: if x is right in the bottom k bits, then
    * x·(2 − m·x) is right in the bottom 2k bits. Every odd m is its own inverse mod 8, so x = m
    * starts with 3 correct bits, and five steps give 96, more than the 64 needed.
    */
  private val minusInverse: Long =
    val inverse = (1 to 5).foldLeft(m)((x, _) => x * (2 - m * x))
    -inverse

  /** x + y mod m, for x and y below m. Their sum can pass 2⁶³ and look negative as a signed
    * Long, so it is compared as an unsigned number.
    */
  private def addMod(x: Long, y: Long): Long =
    val sum = x + y
    if java.lang.Long.compareUnsigned(sum, m) >= 0 then sum - m else sum

  /** R mod m, which is 1 in Montgomery form. 2⁶⁴ − 1 is the unsigned Long −1. */
  private val one: Long = addMod(java.lang.Long.remainderUnsigned(-1L, m), 1)

  /** R² mod m, by doubling R mod m another 64 times. Multiplying by it turns x into x·R. */
  private val rSquared: Long = (1 to 64).foldLeft(one)((x, _) => addMod(x, x))

  /** The top 64 bits of x·y, taking both as unsigned. Java 18 has this built in; Java 17,
    * which this project runs on, only has the signed version, so it is corrected by hand.
    */
  private def unsignedMultiplyHigh(x: Long, y: Long): Long =
    Math.multiplyHigh(x, y) + ((x >> 63) & y) + ((y >> 63) & x)

  /** (hi·2⁶⁴ + lo) / R mod m, for hi·2⁶⁴ + lo < m·R.
    *
    * u = lo·(−m⁻¹) makes lo + u·m divisible by 2⁶⁴, so (hi·2⁶⁴ + lo + u·m) / 2⁶⁴ is exact. Its
    * bottom half lo + low(u·m) is 0 if lo is 0, and exactly 2⁶⁴ otherwise, which carries 1.
    */
  private def redc(hi: Long, lo: Long): Long =
    val u = lo * minusInverse
    val t = hi + unsignedMultiplyHigh(u, m) + (if lo != 0 then 1 else 0)
    if java.lang.Long.compareUnsigned(t, m) >= 0 then t - m else t

  /** Product of two numbers in Montgomery form, in Montgomery form. */
  private def multiply(x: Long, y: Long): Long = redc(Math.multiplyHigh(x, y), x * y)

  private def toMontgomery(x: Long): Long = multiply(Math.floorMod(x, m), rSquared)

  private def fromMontgomery(x: Long): Long = redc(0, x)

  /** a·b mod m, for any a and b, negative ones included. */
  def mulMod(a: Long, b: Long): Long =
    fromMontgomery(multiply(toMontgomery(a), toMontgomery(b)))

  /** baseᵉ mod m, by repeated squaring: e is read bit by bit from the bottom, squaring the base
    * each time and multiplying it into the result where the bit is 1.
    */
  def powMod(base: Long, e: Long): Long =
    require(e >= 0, s"negative exponent $e")
    var result = one
    var square = toMontgomery(base)
    var rest = e
    while rest > 0 do
      if (rest & 1) == 1 then result = multiply(result, square)
      square = multiply(square, square)
      rest >>= 1
    fromMontgomery(result)

  /** For Pollard's rho method, which needs only products and gcds with m, so its numbers can
    * stay in Montgomery form throughout: multiplying by R, which has no factor in common with
    * m, changes no gcd with m. These work on numbers below m that are already in that form.
    */
  def times(x: Long, y: Long): Long = multiply(x, y)

  /** The rho step x ↦ x² + c (in Montgomery form), for 0 <= c < m. */
  def squarePlus(x: Long, c: Long): Long = addMod(multiply(x, x), c)
