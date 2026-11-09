package perfectcuboid

/** A whole number from 0 to (2⁶³ − 1)², too big for a Long, held in two: hi·2⁶⁴ + lo, with
  * `lo` read as unsigned. That range is exactly what sums of squares of edges need: the
  * squares of a cuboid's diagonals are at most (2⁶³ − 1)² whenever its edges are within
  * `Edge.Max`, and so their square roots, the diagonals themselves, fit in a Long.
  *
  * Only what the cuboid checks need is here: squares of Longs, sums, and exact square roots.
  */
final case class Wide(hi: Long, lo: Long):
  require(hi >= 0 && Wide.notAboveMax(hi, lo), s"$this is outside the range of Wide")

  def toBigInt: BigInt = (BigInt(hi) << 64) + BigInt(java.lang.Long.toUnsignedString(lo))

  def +(that: Wide): Wide =
    val sum = lo + that.lo
    val carry = if java.lang.Long.compareUnsigned(sum, lo) < 0 then 1L else 0L
    Wide(Math.addExact(Math.addExact(hi, that.hi), carry), sum)

object Wide:

  /** (2⁶³ − 1)² = 2¹²⁶ − 2⁶⁴ + 1, the largest Wide. */
  private val MaxHi: Long = (1L << 62) - 1
  private val MaxLo: Long = 1L

  private def notAboveMax(hi: Long, lo: Long): Boolean =
    hi < MaxHi || (hi == MaxHi && java.lang.Long.compareUnsigned(lo, MaxLo) <= 0)

  def apply(x: Long): Wide =
    require(x >= 0, s"Wide numbers are not negative: $x")
    Wide(0, x)

  /** x², for any non-negative Long. `Math.multiplyHigh` gives the top 64 bits of the product,
    * and ordinary multiplication the bottom 64, wrapping round as it overflows.
    */
  def square(x: Long): Wide =
    require(x >= 0, s"Wide squares only of non-negative numbers: $x")
    Wide(Math.multiplyHigh(x, x), x * x)

  private val TwoTo64: Double = 18446744073709551616.0

  private def unsignedToDouble(x: Long): Double =
    if x >= 0 then x.toDouble else TwoTo64 + x.toDouble

  /** True when r² <= hi·2⁶⁴ + lo, for 0 <= r < 2⁶³. */
  private def squareAtMost(r: Long, hi: Long, lo: Long): Boolean =
    val rHi = Math.multiplyHigh(r, r)
    rHi < hi || (rHi == hi && java.lang.Long.compareUnsigned(r * r, lo) <= 0)

  /** The floor of the square root of hi·2⁶⁴ + lo, taken as a Wide (but without building one,
    * since this is used in the search's inner loop).
    *
    * A Double holds 53 bits, so `math.sqrt` of the number as a Double can be off by about a
    * thousand. One step of Newton's method, r + (n − r²) / 2r, with n − r² computed exactly,
    * brings it within one; comparing squares exactly then settles the last step.
    */
  def isqrt(hi: Long, lo: Long): Long =
    if hi == 0 && lo >= 0 then Arith.isqrt(lo)
    else
      val guess = math.sqrt(hi * TwoTo64 + unsignedToDouble(lo)).toLong
      // n − guess², as a signed 128-bit number: subtract the low halves, then the high ones with the borrow.
      val gHi = Math.multiplyHigh(guess, guess)
      val gLo = guess * guess
      val borrow = if java.lang.Long.compareUnsigned(lo, gLo) < 0 then 1L else 0L
      val difference = (hi - gHi - borrow) * TwoTo64 + unsignedToDouble(lo - gLo)
      var r = guess + math.round(difference / (2.0 * guess))
      while !squareAtMost(r, hi, lo) do r -= 1
      while r < Long.MaxValue && squareAtMost(r + 1, hi, lo) do r += 1
      r

  def exactSqrt(n: Wide): Option[Long] =
    val r = isqrt(n.hi, n.lo)
    Option.when(Wide.square(r) == n)(r)

  /** Below hi = 2³⁸, a number is below 2¹⁰², so the root of any square among them is below
    * 2⁵¹. For those, the Double square root is off by less than a quarter, and rounding it
    * gives the root exactly: converting the number to a Double is off by at most n·2⁻⁵³,
    * which moves the root by at most r·2⁻⁵⁴ < ⅛, and `math.sqrt` itself rounds to within ⅛.
    */
  private val RoundingLimitHi: Long = 1L << 38

  /** Whether b² + c² is a perfect square, for any non-negative b and c up to `Edge.Max`.
    *
    * This is the test the search spends half its time in, so it builds no `Wide`s. With both
    * below 2³¹ the sum fits in a Long and the usual test does. Otherwise, the bottom 6 bits of
    * the sum are correct even when the Long arithmetic overflows, so the remainder mod 64 rules
    * out most non-squares first. For the rest, the root is rounded from the Double square root
    * while that is exact (any brick up to 10¹⁵), and `isqrt` takes over beyond. Either way,
    * the candidate root is squared exactly and compared, so the answer is never a guess.
    */
  def isSumOfSquaresSquare(b: Long, c: Long): Boolean =
    if b <= Int.MaxValue && c <= Int.MaxValue then Arith.isPerfectSquare(b * b + c * c)
    else
      val lo = b * b + c * c
      Arith.mightBeSquare(lo) && {
        val carry = if java.lang.Long.compareUnsigned(lo, b * b) < 0 then 1L else 0L
        val hi = Math.multiplyHigh(b, b) + Math.multiplyHigh(c, c) + carry
        val r =
          if hi < RoundingLimitHi then Math.round(math.sqrt(hi * TwoTo64 + unsignedToDouble(lo)))
          else isqrt(hi, lo)
        r * r == lo && Math.multiplyHigh(r, r) == hi
      }

  /** Wide numbers are whole numbers, so the cuboid checks in `Box` work for them as well.
    * Products are only needed for squaring edges, which are Longs.
    */
  given WholeNumber[Wide]:
    def fromInt(n: Int): Wide = Wide(n.toLong)
    def plus(x: Wide, y: Wide): Wide = x + y
    def times(x: Wide, y: Wide): Wide =
      if x.hi == 0 && y.hi == 0 && x.lo >= 0 && y.lo >= 0 then Wide(Math.multiplyHigh(x.lo, y.lo), x.lo * y.lo)
      else throw ArithmeticException(s"Wide product too large: $x × $y")
    def gcd(x: Wide, y: Wide): Wide =
      val g = x.toBigInt.gcd(y.toBigInt)
      Wide((g >> 64).toLong, g.toLong)
    def exactSqrt(n: Wide): Option[Wide] = Wide.exactSqrt(n).map(Wide(_))
