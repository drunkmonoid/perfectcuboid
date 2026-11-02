package perfectcuboid

import Arith.*

/** A box with whole-number edges a <= b <= c.
  *
  * The constructor is `private`, so the only way to get a Cuboid is `Cuboid.of`, which
  * sorts the edges and rejects non-positive ones. Every Cuboid that exists is therefore valid.
  */
final case class Cuboid private (a: Long, b: Long, c: Long):

  /** Diagonals of the ab, ac and bc faces, each `None` when it is not a whole number. */
  def faceDiagonals: List[Option[Long]] =
    List(a -> b, a -> c, b -> c).map((x, y) => exactSqrt(sumOfSquares(x, y)))

  /** The diagonal through the inside of the box, from one corner to the opposite one. */
  def spaceDiagonal: Option[Long] = exactSqrt(sumOfSquares(a, b, c))

  def isEuler: Boolean = faceDiagonals.forall(_.isDefined)

  def isPerfect: Boolean = isEuler && spaceDiagonal.isDefined

  /** True when the edges share no common factor, i.e. this is not a scaled-up copy of a smaller box. */
  def isPrimitive: Boolean = gcd(gcd(a, b), c) == 1

  def kind: Kind =
    (faceDiagonals.count(_.isDefined), spaceDiagonal.isDefined) match
      case (3, true)  => Kind.Perfect
      case (3, false) => Kind.Body
      case (2, true)  => Kind.Face
      case _          => Kind.Other

object Cuboid:

  /** The largest edge for which a² + b² + c² is sure to fit in a Long. */
  val MaxSafeEdge: Long = Arith.isqrt(Long.MaxValue / 3)

  def of(x: Long, y: Long, z: Long): Option[Cuboid] =
    List(x, y, z).sorted match
      case List(a, b, c) if a > 0 => Some(Cuboid(a, b, c))
      case _                      => None

  /** Order cuboids by largest edge, then middle, then smallest.
    *
    * A `given` is a value the compiler passes in automatically wherever one is needed,
    * so `cuboids.sorted` finds this without being told.
    */
  given Ordering[Cuboid] = Ordering.by(cuboid => (cuboid.c, cuboid.b, cuboid.a))

/** How close a cuboid with whole-number edges comes to being perfect. */
enum Kind:
  /** All three face diagonals and the space diagonal are whole numbers. None is known. */
  case Perfect

  /** All face diagonals whole, the space diagonal not: an Euler brick, e.g. 44 × 117 × 240. */
  case Body

  /** The space diagonal and two face diagonals whole, one face diagonal not, e.g. 104 × 153 × 672. */
  case Face

  case Other
