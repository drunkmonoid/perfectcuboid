package perfectcuboid

class FourDTest extends munit.FunSuite:

  def wholePairs(edges: List[BigInt]): Int =
    edges.combinations(2).count { case List(x, y) => Arith.isPerfectSquareBig(x * x + y * y); case _ => false }

  test("85 × 132 × 351 × 720 has five whole pairs: a brick, and 351 pairs with 132 and 720"):
    val found = FourD.extensions(Cuboid.of(85, 132, 720).get)
    assert(found.contains(FourD.Quadruple(List(85, 132, 351, 720).map(BigInt(_)), 5)), found)

  test("every quadruple found has the number of whole pairs it claims"):
    for q <- FourD.search(FastSearch.eulerBricksBetween(1, 3000)) do
      assertEquals(wholePairs(q.edges), q.wholePairs, q)
      assertEquals(q.edges.distinct.size, 4)

  test("with every edge up to 3000, the search finds what brute force over the graph finds"):
    val graph = PythagoreanGraph.upTo(3000)
    val bruteForce =
      for
        (x, y, z) <- graph.triangles
        d <- 1L to 3000L
        if !List(x, y, z).contains(d) && List(x, y, z).count(graph.neighbours.getOrElse(d, Set.empty)) >= 2
      yield List(x, y, z, d).map(BigInt(_)).sorted
    val searched = FourD.search(FastSearch.eulerBricksBetween(1, 3000)).map(_.edges).filter(_.last <= 3000)
    assertEquals(searched.toSet, bruteForce.toSet)
