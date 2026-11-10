package perfectcuboid

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class FamiliesTest extends munit.ScalaCheckSuite:

  // Any of the first few thousand triples of the tree.
  val triple: Gen[Triple] = Gen.oneOf(Families.primitiveTriples.take(3000))

  test("the tree starts at (3, 4, 5) with children (5, 12, 13), (21, 20, 29), (15, 8, 17)"):
    assertEquals(
      Families.berggrenLevels.take(2).toList,
      List(
        List(Triple(3, 4, 5)),
        List(Triple(5, 12, 13), Triple(21, 20, 29), Triple(15, 8, 17))
      )
    )

  property("every triple in the tree is a primitive Pythagorean triple"):
    forAll(triple): t =>
      t.a * t.a + t.b * t.b == t.c * t.c && Arith.gcd(t.a, t.b) == 1

  test("the tree holds every primitive triple, each exactly once"):
    val legLimit = 2000L
    val fromTree = Families
      .primitiveTriplesUpTo(2 * legLimit)
      .map(t => (math.min(t.a, t.b), math.max(t.a, t.b)))
      .filter(_._2 <= legLimit)
    val fromEuclid = Pythagorean.legPairs(legLimit).filter((x, y) => Arith.gcd(x, y) == 1)
    assertEquals(fromTree.size, fromTree.distinct.size)
    assertEquals(fromTree.sorted, fromEuclid.sorted.toList)

  test("Saunderson's formula turns (3, 4, 5) into the smallest Euler brick"):
    assertEquals(Families.saunderson(Triple(3, 4, 5)), Cuboid.of(44, 117, 240))

  property("Saunderson bricks are Euler bricks, never perfect, and obey the rules"):
    forAll(triple): t =>
      Families
        .saunderson(t)
        .forall: brick =>
          brick.kind == Kind.Body && Theory.violations(Theory.rulesFor(brick), brick).isEmpty

  test("the search finds every Saunderson brick in its range"):
    val limit = 20_000L
    val found = Search.eulerBricks(limit).toSet
    val expected = Families.saundersonBricksUpTo(limit)
    assert(expected.nonEmpty)
    assert(expected.forall(found.contains), expected.filterNot(found.contains))
