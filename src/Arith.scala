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

  def isPerfectSquare(n: Long): Boolean = exactSqrt(n).isDefined

  @tailrec def gcd(a: Long, b: Long): Long =
    if b == 0 then a.abs else gcd(b, a % b)

  /** x², throwing on overflow instead of silently wrapping around to a wrong number. */
  def square(x: Long): Long = Math.multiplyExact(x, x)

  /** x₁² + x₂² + …, also throwing on overflow. */
  def sumOfSquares(xs: Long*): Long =
    xs.foldLeft(0L)((acc, x) => Math.addExact(acc, square(x)))
