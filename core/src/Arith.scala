package perfectcuboid

import scala.annotation.tailrec

/** Exact integer arithmetic on `Long`.
  *
  * Every function here is pure: the result depends only on the arguments, and nothing
  * else happens. `require` is used only to reject inputs that indicate a bug in the caller.
  */
object Arith:

  /** The largest r with r * r <= Long.MaxValue. Squaring anything bigger overflows. */
  private val MaxRoot: Long = 3037000499L

  private def squareAtMost(r: Long, n: Long): Boolean =
    r <= MaxRoot && r * r <= n

  /** Floor of the square root of `n`, exact for every non-negative Long.
    *
    * `math.sqrt` works on Doubles, which hold only 53 bits of precision, so for large n
    * its answer can be slightly off. We use it as a first guess and step to the exact value.
    */
  def isqrt(n: Long): Long =
    require(n >= 0, s"isqrt of a negative number: $n")
    // @tailrec makes the compiler check these recursions compile to plain loops.
    @tailrec def down(r: Long): Long = if squareAtMost(r, n) then r else down(r - 1)
    @tailrec def up(r: Long): Long = if squareAtMost(r + 1, n) then up(r + 1) else r
    up(down(math.sqrt(n.toDouble).toLong))

  /** Floor of the square root of a BigInt, by Newton's method: start above the root and
    * repeat x → (x + n/x) / 2, which only goes down, until it stops going down.
    */
  def isqrtBig(n: BigInt): BigInt =
    require(n >= 0, s"isqrt of a negative number: $n")
    @tailrec def descend(x: BigInt): BigInt =
      val next = (x + n / x) / 2
      if next >= x then x else descend(next)
    if n < 2 then n else descend(BigInt(1) << ((n.bitLength + 1) / 2))

  /** Smallest r with r * r >= n. */
  def ceilSqrt(n: Long): Long =
    val r = isqrt(n)
    if r * r == n then r else r + 1

  /** The square root of `n` if it is a whole number, `None` otherwise. */
  def exactSqrt(n: Long): Option[Long] =
    if n < 0 then None
    else
      val r = isqrt(n)
      Option.when(r * r == n)(r)

  /** Bit r is set when some square leaves remainder r on division by 64. Only 12 of the 64
    * bits are set, so most numbers can be ruled out as squares without taking a square root.
    */
  private val SquareRemaindersMod64: Long =
    (0L until 64L).foldLeft(0L)((bits, i) => bits | (1L << (i * i % 64)))

  /** False when n's remainder mod 64 shows it is not a square. Uses only the bottom 6 bits,
    * which stay right even when the arithmetic that produced n overflowed.
    */
  def mightBeSquare(n: Long): Boolean = ((SquareRemaindersMod64 >>> (n & 63)) & 1) == 1

  /** Whether n is a square. If it is, its root r is below 2³², and the Double square root of n
    * is within far less than ½ of r, so rounding it gives r exactly; squaring that and
    * comparing then answers for every n, with no correcting steps and no `Option`.
    */
  def isPerfectSquare(n: Long): Boolean =
    n >= 0 && mightBeSquare(n) && {
      val r = math.round(math.sqrt(n.toDouble))
      r * r == n
    }

  /** Whether n is a square, for a BigInt of any size. */
  def isPerfectSquareBig(n: BigInt): Boolean =
    n >= 0 && { val r = isqrtBig(n); r * r == n }

  @tailrec def gcd(a: Long, b: Long): Long =
    if b == 0 then a.abs else gcd(b, a % b)

  /** x², throwing on overflow instead of silently wrapping around to a wrong number. */
  def square(x: Long): Long = Math.multiplyExact(x, x)

  /** x₁² + x₂² + …, also throwing on overflow. */
  def sumOfSquares(xs: Long*): Long =
    xs.foldLeft(0L)((acc, x) => Math.addExact(acc, square(x)))
