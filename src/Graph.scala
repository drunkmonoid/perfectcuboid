package perfectcuboid

import scala.annotation.tailrec

/** The Pythagorean graph on 1, …, limit: x and y are joined when x² + y² is a square.
  *
  * An Euler brick is three numbers that are all joined to each other: a triangle in this graph.
  * A perfect cuboid would be a triangle whose three squares also add up to a square. Looking at
  * the graph as a whole shows how the bricks sit among all the Pythagorean pairs.
  */
final case class PythagoreanGraph(limit: Long, neighbours: Map[Long, Set[Long]]):

  def degree(x: Long): Int = neighbours.get(x).fold(0)(_.size)

  def links: Long = neighbours.values.map(_.size.toLong).sum / 2 // each link is seen from both ends

  /** How many numbers have each degree, for the numbers with at least one neighbour. */
  def degreeCounts: Map[Int, Int] = neighbours.values.groupMapReduce(_.size)(_ => 1)(_ + _)

  /** Every triangle x < y < z once: for each link x–y, the common neighbours above y. */
  def triangles: Vector[(Long, Long, Long)] =
    for
      (x, nx) <- neighbours.toVector
      y <- nx.toVector if y > x
      z <- (nx intersect neighbours(y)).toVector if z > y
    yield (x, y, z)

  /** The connected pieces of the graph, largest first, each a set of numbers.
    *
    * Grown one at a time by breadth-first search: start from a number not yet in any piece,
    * and keep adding the neighbours of the newest numbers until no new ones appear.
    */
  def components: List[Set[Long]] =
    @tailrec def grow(piece: Set[Long], frontier: Set[Long]): Set[Long] =
      if frontier.isEmpty then piece
      else
        val next = frontier.flatMap(neighbours) -- piece
        grow(piece ++ next, next)
    @tailrec def all(left: Set[Long], found: List[Set[Long]]): List[Set[Long]] =
      left.headOption match
        case None        => found
        case Some(start) =>
          val piece = grow(Set(start), Set(start))
          all(left -- piece, piece :: found)
    all(neighbours.keySet, Nil).sortBy(-_.size)

object PythagoreanGraph:

  def upTo(limit: Long): PythagoreanGraph = PythagoreanGraph(limit, Pythagorean.partners(limit))
