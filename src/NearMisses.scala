package perfectcuboid

import Arith.*

/** A box described by the squares of its edges, a2 <= b2 <= c2, so that an edge may be
  * irrational (√618849) while everything stays in whole-number arithmetic.
  *
  * `of` only builds one when the sum of all three fits in a Long, so no length overflows.
  */
final case class SquaredCuboid private (a2: Long, b2: Long, c2: Long):

  def edges: List[Option[Long]] = List(a2, b2, c2).map(exactSqrt)

  def faceDiagonals: List[Option[Long]] = List(a2 + b2, a2 + c2, b2 + c2).map(exactSqrt)

  def spaceDiagonal: Option[Long] = exactSqrt(a2 + b2 + c2)

  def kind: Kind =
    val irrationalEdges = edges.count(_.isEmpty)
    val irrationalFaces = faceDiagonals.count(_.isEmpty)
    val irrationalSpace = if spaceDiagonal.isEmpty then 1 else 0
    (irrationalEdges, irrationalFaces, irrationalSpace) match
      case (0, 0, 0) => Kind.Perfect
      case (0, 0, 1) => Kind.Body
      case (0, 1, 0) => Kind.Face
      case (1, 0, 0) => Kind.Edge
      case _         => Kind.Other

object SquaredCuboid:

  def of(x2: Long, y2: Long, z2: Long): Option[SquaredCuboid] =
    val sorted = List(x2, y2, z2).sorted
    val fits = BigInt(x2) + y2 + z2 <= Long.MaxValue
    if sorted.head > 0 && fits then Some(SquaredCuboid(sorted(0), sorted(1), sorted(2))) else None

/** Searches for the almost-perfect cuboids that are not Euler bricks. */
object NearMisses:

  /** Face cuboids whose two whole face diagonals meet at edge s, plus any perfect cuboid
    * with an edge s (none is known).
    *
    * The other edges x and y must both be legs that pair with s, and s² + x² + y² must be a
    * square. Each face cuboid has exactly one such shared edge, so each is found exactly once.
    */
  def faceCuboidsWithSharedEdge(s: Long): List[Cuboid] =
    val legs = Pythagorean.legsPairedWith(s).filter(_ <= Edge.Max)
    for
      case x :: bigger <- legs.tails.toList
      y <- bigger
      if isPerfectSquare(sumOfSquares(s, x, y))
      cuboid <- Cuboid.of(s, x, y)
    yield cuboid

  /** Every face cuboid with edges up to Edge.Max, ordered by shared edge. */
  def allFaceCuboids: LazyList[Cuboid] =
    LazyList.iterate(1L)(_ + 1).takeWhile(_ <= Edge.Max).flatMap(faceCuboidsWithSharedEdge)

  /** Edge cuboids whose irrational edge is √c2.
    *
    * Each whole edge x needs x² + c2 to be a square, so x is a completion of c2. The two whole
    * edges a and b must also make a whole face diagonal e, and then e² + c2 is the squared
    * space diagonal, so e must be a completion of c2 too.
    */
  def edgeCuboidsWithSquaredEdge(c2: Long): List[SquaredCuboid] =
    if isPerfectSquare(c2) then Nil
    else
      val xs = Pythagorean.completions(c2).filter(_ <= Edge.Max)
      val isCompletion = xs.toSet
      for
        case a :: bigger <- xs.tails.toList
        b <- bigger
        e <- exactSqrt(sumOfSquares(a, b)).toList
        if isCompletion(e)
        cuboid <- SquaredCuboid.of(a * a, b * b, c2)
      yield cuboid

  /** Every edge cuboid, ordered by the square of its irrational edge. */
  def allEdgeCuboids: LazyList[SquaredCuboid] =
    LazyList.iterate(2L)(_ + 1).flatMap(edgeCuboidsWithSquaredEdge)
