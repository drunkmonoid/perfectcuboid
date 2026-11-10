package perfectcuboid

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class OddSearchTest extends munit.ScalaCheckSuite:

  property("the legs from both sides are every leg that pairs with the edge"):
    forAll(Gen.chooseNum(1L, 10_000_000L)): a =>
      FastSearch.legsOf(a, NumberTheory.factorize(a)).toList == Pythagorean.legsPairedWith(a)

  test("searching by odd edge finds exactly the primitive bricks with that odd edge"):
    val limit = 5000L
    // Every brick with odd edge up to the limit has its smallest edge up to the limit too.
    val expected = Search.allEulerBricks
      .takeWhile(_.a <= limit)
      .filter(b => b.isPrimitive && OddSearch.oddEdge(b) <= limit)
      .toSet
    val found = OddSearch.primitiveBricksIterator(1, limit).toList
    assertEquals(found.size, found.toSet.size, "a brick found twice")
    assertEquals(found.toSet, expected)

  test("each brick is found at its odd edge, in order of odd edge"):
    val found = OddSearch.primitiveBricksIterator(1, 20_000).toList
    assert(found.nonEmpty)
    found.foreach(b => assertEquals(List(b.a: Long, b.b, b.c).count(_ % 2 != 0), 1, b))
    assertEquals(found.map(OddSearch.oddEdge), found.map(OddSearch.oddEdge).sorted)

  test("an odd edge can be the shortest, the middle or the longest"):
    val positions = OddSearch
      .primitiveBricksIterator(1, 20_000)
      .toList
      .map: b =>
        List(b.a: Long, b.b, b.c).indexOf(OddSearch.oddEdge(b))
    assertEquals(positions.toSet, Set(0, 1, 2))

  test("an even edge is refused"):
    intercept[IllegalArgumentException](OddSearch.primitiveBricksWithOddEdge(240))
