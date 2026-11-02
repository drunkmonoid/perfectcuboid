package perfectcuboid

/** Why some numbers cannot be the edges of a cuboid. Each case carries the offending value. */
enum CuboidError:
  case NonPositiveEdge(edge: BigInt)
  case EdgeTooLarge(edge: BigInt)

  def message: String = this match
    case NonPositiveEdge(edge) => s"edge $edge is not positive"
    case EdgeTooLarge(edge)    => s"edge $edge is above ${Edge.Max}, where diagonals would overflow"

/** The length of an edge: a whole number from 1 to `Edge.Max`.
  *
  * `opaque` means that inside this file an Edge is simply a Long, while everywhere else it is a
  * type of its own. `<: Long` lets any Edge be used as a Long, but a Long only becomes an Edge
  * through `Edge.from`, which checks it. At runtime it is a plain Long, with no wrapper to pay for.
  */
opaque type Edge <: Long = Long

object Edge:

  /** The largest edge for which a² + b² + c² is sure to fit in a Long. */
  val Max: Long = Arith.isqrt(Long.MaxValue / 3)

  def from(n: Long): Either[CuboidError, Edge] =
    if n <= 0 then Left(CuboidError.NonPositiveEdge(n))
    else if n > Max then Left(CuboidError.EdgeTooLarge(n))
    else Right(n)

  def fromBigInt(n: BigInt): Either[CuboidError, Edge] =
    if n.isValidLong then from(n.toLong)
    else if n <= 0 then Left(CuboidError.NonPositiveEdge(n))
    else Left(CuboidError.EdgeTooLarge(n))

/** The length of a diagonal, always a whole number: one that isn't is `None` instead. */
opaque type Diagonal <: Long = Long

object Diagonal:

  /** The diagonal whose square is `n`, if that is a whole number. */
  def fromSquare(n: Long): Option[Diagonal] = Arith.exactSqrt(n)

/** Extension methods add methods to a type from the outside, here to every Long. */
extension (n: Long)
  def toEdge: Either[CuboidError, Edge] = Edge.from(n)
