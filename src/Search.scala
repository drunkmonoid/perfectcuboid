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
