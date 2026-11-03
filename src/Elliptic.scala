package perfectcuboid

/** A fraction n/d in lowest terms, with d > 0. Exact, of any size. */
final case class Rational private (num: BigInt, den: BigInt):

  def +(that: Rational): Rational = Rational(num * that.den + that.num * den, den * that.den)
  def -(that: Rational): Rational = Rational(num * that.den - that.num * den, den * that.den)
  def *(that: Rational): Rational = Rational(num * that.num, den * that.den)
  def /(that: Rational): Rational = Rational(num * that.den, den * that.num)
  def unary_- : Rational = Rational(-num, den)
  def isZero: Boolean = num == 0

  /** The square root, if it is a fraction: a fraction in lowest terms is the square of a
    * fraction exactly when its numerator and denominator are both squares.
    */
  def sqrt: Option[Rational] =
    for
      n <- summon[WholeNumber[BigInt]].exactSqrt(num)
      d <- summon[WholeNumber[BigInt]].exactSqrt(den)
    yield Rational(n, d)

  override def toString: String = if den == 1 then num.toString else s"$num/$den"

object Rational:

  def apply(num: BigInt, den: BigInt = 1): Rational =
    require(den != 0, "a fraction with denominator 0")
    val g = num.gcd(den) * den.signum
    new Rational(num / g, den / g)

/** A point on an elliptic curve: an ordinary point (x, y), or the point at infinity, which is
  * the zero of the group: P + ∞ = P.
  */
enum Point:
  case Infinity
  case At(x: Rational, y: Rational)

/** The curve y² = x(x + a²)(x + b²), which holds the Euler bricks with two edges a and b.
  *
  * A third edge c needs c² + a² and c² + b² to be squares. Then x = c² makes the right-hand
  * side c²·(c² + a²)·(c² + b²) a square, so (c², c·√(c² + a²)·√(c² + b²)) is a point on the
  * curve. Conversely, a point whose x, x + a² and x + b² are all squares gives a third edge √x.
  *
  * Points on a cubic curve can be added: the line through P and Q meets the curve in a third
  * point, and reflecting that in the x-axis gives P + Q. With rational coordinates in, the
  * coordinates out are rational. The points with x, x + a², x + b² all squares are exactly the
  * doubles 2Q of rational points, so any multiple of a brick's point is again such a point,
  * and gives a new brick: a × b scaled, with a new third edge.
  */
final case class EulerCurve(a: BigInt, b: BigInt):

  // In the general form y² = x³ + a₂x² + a₄x, here a₂ = a² + b² and a₄ = a²b².
  private val a2 = Rational(a * a + b * b)
  private val a4 = Rational(a * a * b * b)

  def contains(p: Point): Boolean = p match
    case Point.Infinity => true
    case Point.At(x, y) => y * y == x * x * x + a2 * x * x + a4 * x

  /** The point of the brick a × b × c, if it is one. */
  def pointOf(c: BigInt): Option[Point] =
    val x = Rational(c * c)
    for
      e <- (x + Rational(a * a)).sqrt
      f <- (x + Rational(b * b)).sqrt
    yield Point.At(x, Rational(c) * e * f)

  def add(p: Point, q: Point): Point = (p, q) match
    case (Point.Infinity, _)                  => q
    case (_, Point.Infinity)                  => p
    case (Point.At(x1, y1), Point.At(x2, y2)) =>
      if x1 == x2 && (y1 + y2).isZero then Point.Infinity // q = −p: the line is vertical
      else
        // The slope of the line through p and q, or of the tangent at p when they are equal.
        val slope =
          if x1 == x2 then (Rational(3) * x1 * x1 + Rational(2) * a2 * x1 + a4) / (Rational(2) * y1)
          else (y2 - y1) / (x2 - x1)
        val x3 = slope * slope - a2 - x1 - x2
        Point.At(x3, slope * (x1 - x3) - y1)

  /** p, 2p, 3p, …, ending early if a multiple comes back to infinity (p has finite order). */
  def multiples(p: Point): LazyList[Point] =
    LazyList.iterate(p)(add(_, p)).takeWhile(_ != Point.Infinity)

  /** The brick a × b × √x for a point whose x is a square: a, b and √x scaled to whole
    * numbers, then reduced to the primitive brick of that shape.
    */
  def brickOf(p: Point): Option[BigCuboid] = p match
    case Point.Infinity => None
    case Point.At(x, _) =>
      for
        c <- x.sqrt
        if !c.isZero
        brick <- BigCuboid.of(a * c.den, b * c.den, c.num)
      yield brick.primitive
