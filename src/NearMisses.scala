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

  /** The largest whole edge for which a² + b² + c² is sure to fit in a Long. The near-miss
    * searches keep to edges up to here.
    */
  val MaxEdge: Long = isqrt(Long.MaxValue / 3)

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
    val legs = Pythagorean.legsPairedWith(s).filter(_ <= SquaredCuboid.MaxEdge)
    for
      case x :: bigger <- legs.tails.toList
      y <- bigger
      if isPerfectSquare(sumOfSquares(s, x, y))
      cuboid <- Cuboid.of(s, x, y)
    yield cuboid

  /** Face cuboids, and any perfect cuboid, whose space diagonal is d: found the other way
    * round from `faceCuboidsWithSharedEdge`, as an independent check on it.
    *
    * If a face cuboid has whole face diagonal p on the face s × x, then d² = p² + y², with y
    * its third edge. So (p, y) is a pair of legs under the hypotenuse d, and (s, x) a pair under
    * p: two splittings of a hypotenuse, where the other search pairs legs. Each face cuboid
    * comes up twice this way, once for each of its whole face diagonals, and is kept once.
    */
  def faceCuboidsWithSpaceDiagonal(d: Long): List[Cuboid] =
    val found =
      for
        (u, v) <- Pythagorean.legsWithHypotenuse(d)
        (p, y) <- List((u, v), (v, u))
        (s, x) <- Pythagorean.legsWithHypotenuse(p)
        if y <= SquaredCuboid.MaxEdge && x <= SquaredCuboid.MaxEdge
        if isPerfectSquare(s * s + y * y) || isPerfectSquare(x * x + y * y)
        cuboid <- Cuboid.of(s, x, y)
      yield cuboid
    found.distinct.sortBy(c => (c.a: Long, c.b: Long))

  /** The face cuboids with space diagonal up to `limit`, found both ways: from each space
    * diagonal, and from each shared edge (which is always shorter than the space diagonal).
    */
  def bothWays(limit: Long): (List[Cuboid], List[Cuboid]) =
    def diagonal(c: Cuboid): Long = c.spaceDiagonal.fold(Long.MaxValue)(d => d: Long)
    val fromDiagonals = (1L to limit).toList.flatMap(faceCuboidsWithSpaceDiagonal)
    val fromSharedEdges = (1L to limit).toList.flatMap(faceCuboidsWithSharedEdge).filter(diagonal(_) <= limit)
    def inOrder(cuboids: List[Cuboid]) = cuboids.sortBy(c => (diagonal(c), c.a: Long, c.b: Long))
    (inOrder(fromDiagonals), inOrder(fromSharedEdges))

  /** Every face cuboid with edges up to SquaredCuboid.MaxEdge, ordered by shared edge. */
  def allFaceCuboids: LazyList[Cuboid] =
    LazyList.iterate(1L)(_ + 1).takeWhile(_ <= SquaredCuboid.MaxEdge).flatMap(faceCuboidsWithSharedEdge)

  /** Edge cuboids whose irrational edge is √c2.
    *
    * Each whole edge x needs x² + c2 to be a square, so x is a completion of c2. The two whole
    * edges a and b must also make a whole face diagonal e, and then e² + c2 is the squared
    * space diagonal, so e must be a completion of c2 too.
    */
  def edgeCuboidsWithSquaredEdge(c2: Long): List[SquaredCuboid] =
    if isPerfectSquare(c2) then Nil
    else
      val xs = Pythagorean.completions(c2).filter(_ <= SquaredCuboid.MaxEdge)
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
