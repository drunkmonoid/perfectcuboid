package perfectcuboid

import Arith.*

object Search:

  /** The searches by smallest edge go up to here: the largest a whose square fits in a Long.
    * Every leg that pairs with such an a is below a²/2, so within `Edge.Max`: no brick is
    * left out for being too long.
    */
  val MaxSmallestEdge: Long = isqrt(Long.MaxValue)

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
    * so this list is finite. The legs can be up to a²/2, so b² + c² can pass the range of a
    * Long; `Wide.isSumOfSquaresSquare` computes it with 128 bits.
    */
  def eulerBricksWithSmallestEdge(a: Long): List[Cuboid] =
    val legs = Pythagorean.legsPairedWith(a).filter(_ > a)
    for
      // `tails` gives the list, then the list without its first element, and so on, so
      // this pairs each leg b with every bigger leg c exactly once.
      case b :: bigger <- legs.tails.toList
      c <- bigger
      if Wide.isSumOfSquaresSquare(b, c)
      cuboid <- Cuboid.of(a, b, c)
    yield cuboid

  /** Every Euler brick, ordered by smallest edge.
    *
    * An infinite list in principle (it stops at MaxSmallestEdge), computed only as far as it is read.
    * A `def`, so nothing holds on to its start and memory stays flat however far it is read.
    */
  def allEulerBricks: LazyList[Cuboid] =
    LazyList.iterate(1L)(_ + 1).takeWhile(_ <= MaxSmallestEdge).flatMap(eulerBricksWithSmallestEdge)

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
