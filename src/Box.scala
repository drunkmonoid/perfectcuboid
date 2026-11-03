package perfectcuboid

/** What the cuboid checks need to know about a type of whole numbers.
  *
  * A type class: a trait with a type parameter, and `given` instances saying how `Long` and
  * `BigInt` do each thing. Code written against `WholeNumber[N]` works for any N that has an
  * instance, and the compiler picks the instance from the types at each call.
  */
trait WholeNumber[N]:
  def fromInt(n: Int): N
  def plus(x: N, y: N): N
  def times(x: N, y: N): N
  def gcd(x: N, y: N): N

  /** The square root of n if it is a whole number. */
  def exactSqrt(n: N): Option[N]

  // Extension methods let generic code write x + y and x.squared instead of plus(x, y).
  extension (x: N)
    def +(y: N): N = plus(x, y)
    def squared: N = times(x, x)

object WholeNumber:

  /** Long arithmetic that throws on overflow, instead of wrapping round to a wrong answer. */
  given WholeNumber[Long]:
    def fromInt(n: Int): Long = n.toLong
    def plus(x: Long, y: Long): Long = Math.addExact(x, y)
    def times(x: Long, y: Long): Long = Math.multiplyExact(x, y)
    def gcd(x: Long, y: Long): Long = Arith.gcd(x, y)
    def exactSqrt(n: Long): Option[Long] = Arith.exactSqrt(n)

  /** BigInt arithmetic never overflows: the numbers grow as needed. */
  given WholeNumber[BigInt]:
    def fromInt(n: Int): BigInt = BigInt(n)
    def plus(x: BigInt, y: BigInt): BigInt = x + y
    def times(x: BigInt, y: BigInt): BigInt = x * y
    def gcd(x: BigInt, y: BigInt): BigInt = x.gcd(y)
    def exactSqrt(n: BigInt): Option[BigInt] =
      Option.when(n >= 0)(Arith.isqrtBig(n)).filter(r => r * r == n)

/** The checks on a box a × b × c, written once for any type of whole numbers. `Cuboid` uses
  * them with Long edges and `BigCuboid` with BigInt edges.
  *
  * `[N: WholeNumber]` asks for a given `WholeNumber[N]`, and `summon` fetches it.
  */
object Box:

  def faceDiagonalSquares[N: WholeNumber](a: N, b: N, c: N): List[N] =
    List(a.squared + b.squared, a.squared + c.squared, b.squared + c.squared)

  def spaceDiagonalSquare[N: WholeNumber](a: N, b: N, c: N): N =
    a.squared + b.squared + c.squared

  def faceDiagonals[N: WholeNumber](a: N, b: N, c: N): List[Option[N]] =
    faceDiagonalSquares(a, b, c).map(summon[WholeNumber[N]].exactSqrt)

  def spaceDiagonal[N: WholeNumber](a: N, b: N, c: N): Option[N] =
    summon[WholeNumber[N]].exactSqrt(spaceDiagonalSquare(a, b, c))

  def isEuler[N: WholeNumber](a: N, b: N, c: N): Boolean =
    faceDiagonals(a, b, c).forall(_.isDefined)

  def isPerfect[N: WholeNumber](a: N, b: N, c: N): Boolean =
    isEuler(a, b, c) && spaceDiagonal(a, b, c).isDefined

  def isPrimitive[N: WholeNumber](a: N, b: N, c: N): Boolean =
    val whole = summon[WholeNumber[N]]
    whole.gcd(whole.gcd(a, b), c) == whole.fromInt(1)

  /** The kind of a box whose edges are all whole, so it can't be an edge cuboid. */
  def kind[N: WholeNumber](a: N, b: N, c: N): Kind =
    val irrationalFaces = faceDiagonals(a, b, c).count(_.isEmpty)
    (irrationalFaces, spaceDiagonal(a, b, c).isDefined) match
      case (0, true)  => Kind.Perfect
      case (0, false) => Kind.Body
      case (1, true)  => Kind.Face
      case _          => Kind.Other

/** A box with whole edges of any size, a <= b <= c. For checking boxes too big for `Cuboid`,
  * such as those built by formulas.
  */
final case class BigCuboid private (a: BigInt, b: BigInt, c: BigInt):

  def faceDiagonals: List[Option[BigInt]] = Box.faceDiagonals(a, b, c)
  def spaceDiagonal: Option[BigInt] = Box.spaceDiagonal(a, b, c)
  def isEuler: Boolean = Box.isEuler(a, b, c)
  def isPerfect: Boolean = Box.isPerfect(a, b, c)
  def isPrimitive: Boolean = Box.isPrimitive(a, b, c)
  def kind: Kind = Box.kind(a, b, c)

  /** The smallest box of the same shape: every edge divided by their greatest common divisor. */
  def primitive: BigCuboid =
    val g = a.gcd(b).gcd(c)
    BigCuboid(a / g, b / g, c / g)

  /** The same box as a `Cuboid`, if its edges are small enough. */
  def toCuboid: Either[CuboidError, Cuboid] = Cuboid.fromBigInt(a, b, c)

object BigCuboid:

  /** The box with these edges, in any order, if they are all positive. */
  def of(x: BigInt, y: BigInt, z: BigInt): Option[BigCuboid] =
    val List(a, b, c) = List(x, y, z).sorted: @unchecked // three in, three out
    Option.when(a > 0)(BigCuboid(a, b, c))

  def from(cuboid: Cuboid): BigCuboid = BigCuboid(cuboid.a, cuboid.b, cuboid.c)

  given Ordering[BigCuboid] = Ordering.by(box => (box.c, box.b, box.a))
