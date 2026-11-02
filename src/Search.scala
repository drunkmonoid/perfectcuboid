package perfectcuboid

import Arith.*

object Search:

  /** All Euler bricks with every edge <= limit, sorted.
    *
    * In an Euler brick every pair of edges is a pair of Pythagorean legs. So for each
    * edge a, take the bigger legs it pairs with; any two of those, b and c, give an
    * Euler brick exactly when b and c pair up as well.
    */
  def eulerBricks(limit: Long): Vector[Cuboid] =
    val partners = Pythagorean.partners(limit)
    val bricks =
      for
        (a, legs) <- partners.toVector
        bigger = legs.filter(_ > a).toVector.sorted
        case Vector(b, c) <- bigger.combinations(2)
        if partners.getOrElse(b, Set.empty).contains(c)
        cuboid <- Cuboid.of(a, b, c)
      yield cuboid
    bricks.sorted

  /** The Euler bricks with smallest edge a, in order of middle edge.
    *
    * The other two edges must both be legs that pair with a, and only finitely many legs do,
    * so this list is finite. Bricks with an edge above `Edge.Max` are left out.
    */
  def eulerBricksWithSmallestEdge(a: Long): List[Cuboid] =
    val legs = Pythagorean.legsPairedWith(a).filter(leg => leg > a && leg <= Edge.Max)
    for
      // `tails` gives the list, then the list without its first element, and so on, so
      // this pairs each leg b with every bigger leg c exactly once.
      case b :: bigger <- legs.tails.toList
      c <- bigger
      if isPerfectSquare(sumOfSquares(b, c))
      cuboid <- Cuboid.of(a, b, c)
    yield cuboid

  /** Every Euler brick whose edges fit in an Edge, ordered by smallest edge.
    *
    * An infinite list in principle (it stops at Edge.Max), computed only as far as it is read.
    * A `def`, so nothing holds on to its start and memory stays flat however far it is read.
    */
  def allEulerBricks: LazyList[Cuboid] =
    LazyList.iterate(1L)(_ + 1).takeWhile(_ <= Edge.Max).flatMap(eulerBricksWithSmallestEdge)

  def perfectCuboids(limit: Long): Vector[Cuboid] =
    eulerBricks(limit).filter(_.isPerfect)

  /** Same result as `eulerBricks`, found the slow and obvious way. Used to check it in tests. */
  def eulerBricksBruteForce(limit: Long): Vector[Cuboid] =
    val bricks =
      for
        b <- 1L to limit
        a <- 1L to b
        if isPerfectSquare(sumOfSquares(a, b))
        c <- b to limit
        cuboid <- Cuboid.of(a, b, c)
        if cuboid.isEuler
      yield cuboid
    bricks.toVector.sorted
