package perfectcuboid

import Arith.*

/** A box with whole-number edges a <= b <= c.
  *
  * The constructor is `private`, so the only ways to get a Cuboid are `Cuboid.from` and
  * `Cuboid.of`, which check and sort the edges. Every Cuboid that exists is therefore valid,
  * and since every edge is at most `Edge.Max`, computing its diagonals cannot overflow.
  */
final case class Cuboid private (a: Edge, b: Edge, c: Edge):

  /** Diagonals of the ab, ac and bc faces, each `None` when it is not a whole number. */
  def faceDiagonals: List[Option[Diagonal]] =
    List(a -> b, a -> c, b -> c).map((x, y) => Diagonal.fromSquare(sumOfSquares(x, y)))

  /** The diagonal through the inside of the box, from one corner to the opposite one. */
  def spaceDiagonal: Option[Diagonal] = Diagonal.fromSquare(sumOfSquares(a, b, c))

  def isEuler: Boolean = faceDiagonals.forall(_.isDefined)

  def isPerfect: Boolean = isEuler && spaceDiagonal.isDefined

  /** True when the edges share no common factor, i.e. this is not a scaled-up copy of a smaller box. */
  def isPrimitive: Boolean = gcd(gcd(a, b), c) == 1

  /** The same box with every edge multiplied by k. */
  def scaled(k: Long): Either[CuboidError, Cuboid] =
    Cuboid.fromBigInt(BigInt(a) * k, BigInt(b) * k, BigInt(c) * k)

  /** The same box described by its squared edges, which never overflow for edges up to Edge.Max. */
  def squared: SquaredCuboid = SquaredCuboid.of(a * a, b * b, c * c).get

  def kind: Kind = squared.kind

object Cuboid:

  /** The cuboid with these edges, in any order, or the reason there is none.
    *
    * `Either` holds a result (`Right`) or an error (`Left`). A for-comprehension over Eithers
    * stops at the first `Left`, so this returns the first bad edge.
    */
  def from(x: Long, y: Long, z: Long): Either[CuboidError, Cuboid] =
    for
      a <- Edge.from(x)
      b <- Edge.from(y)
      c <- Edge.from(z)
    yield sorted(a, b, c)

  def fromBigInt(x: BigInt, y: BigInt, z: BigInt): Either[CuboidError, Cuboid] =
    for
      a <- Edge.fromBigInt(x)
      b <- Edge.fromBigInt(y)
      c <- Edge.fromBigInt(z)
    yield sorted(a, b, c)

  /** Like `from`, for when the reason doesn't matter. */
  def of(x: Long, y: Long, z: Long): Option[Cuboid] = from(x, y, z).toOption

  private def sorted(x: Edge, y: Edge, z: Edge): Cuboid =
    val (p, q) = if x <= y then (x, y) else (y, x)
    if z <= p then Cuboid(z, p, q)
    else if z <= q then Cuboid(p, z, q)
    else Cuboid(p, q, z)

  /** Order cuboids by largest edge, then middle, then smallest.
    *
    * A `given` is a value the compiler passes in automatically wherever one is needed,
    * so `cuboids.sorted` finds this without being told.
    */
  given Ordering[Cuboid] = Ordering.by(cuboid => (cuboid.c: Long, cuboid.b: Long, cuboid.a: Long))

/** How close a cuboid comes to being perfect, judged on its 7 lengths: 3 edges, 3 face
  * diagonals and the space diagonal. The "almost perfect" kinds have exactly one irrational length.
  */
enum Kind:
  /** All 7 lengths are whole numbers. None is known. */
  case Perfect

  /** Only the space diagonal is irrational: an Euler brick, e.g. 44 × 117 × 240. */
  case Body

  /** Only one face diagonal is irrational, e.g. 104 × 153 × 672. */
  case Face

  /** Only one edge is irrational, e.g. 520 × 576 × √618849. */
  case Edge

  /** Two or more irrational lengths. */
  case Other
