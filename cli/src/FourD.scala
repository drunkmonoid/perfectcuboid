package perfectcuboid

/** Euler bricks in four dimensions: four edges whose six pairs all have whole diagonals, or in
  * the Pythagorean graph, four numbers all joined to each other.
  *
  * Any such four contain Euler bricks, so they can be found by trying to add a fourth edge to
  * each brick. The same goes for the near misses with five whole pairs out of six: four numbers
  * with all links but one still contain a triangle, so they too are a brick plus a fourth edge,
  * one joined to two of the brick's three edges.
  */
object FourD:

  /** Four edges, smallest first, and how many of their six pairs have whole diagonals. */
  final case class Quadruple(edges: List[BigInt], wholePairs: Int):
    def isPrimitive: Boolean = edges.reduce(_ gcd _) == 1

  private def joined(x: BigInt, y: BigInt): Boolean = Arith.isPerfectSquareBig(x * x + y * y)

  /** Every fourth edge d joined to at least two edges of the brick.
    *
    * If d is joined to a, it is one of the finitely many legs that pair with a. Otherwise it is
    * joined to b and c, so it is a leg that pairs with b, and also with c.
    */
  def extensions(brick: Cuboid): List[Quadruple] =
    val edges = List(BigInt(brick.a), BigInt(brick.b), BigInt(brick.c))
    val withA = Pythagorean.legsPairedWith(brick.a).map(BigInt(_))
    val withBAndC = Pythagorean.legsPairedWithAny(brick.b, NumberTheory.factorize(brick.b)).filter(joined(_, edges(2)))
    // The brick's own edges are among the candidates (b pairs with a), and are no fourth edge.
    (withA ++ withBAndC).distinct
      .filterNot(edges.contains)
      .flatMap: d =>
        val pairs = edges.count(joined(d, _))
        Option.when(pairs >= 2)(Quadruple((d :: edges).sorted, 3 + pairs))

  /** The quadruples from every brick with smallest edge up to the limit, each once. */
  def search(bricks: Iterable[Cuboid]): List[Quadruple] =
    bricks.iterator.flatMap(extensions).toList.distinct.sortBy(q => (-q.wholePairs, q.edges.last))
