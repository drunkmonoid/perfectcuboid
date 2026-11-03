package perfectcuboid

import cats.kernel.laws.discipline.CommutativeMonoidTests
import cats.syntax.all.*
import munit.DisciplineSuite
import org.scalacheck.{Arbitrary, Gen, Prop}

/** Checks that the monoids really are monoids. `CommutativeMonoidTests` is a ready-made list
  * of laws (x |+| empty == x, (x |+| y) |+| z == x |+| (y |+| z), x |+| y == y |+| x, …) that
  * discipline runs as ScalaCheck properties on random values.
  */
class LawsTest extends DisciplineSuite:

  /** Random summaries, built the way real ones are: from some bricks. */
  given Arbitrary[Summary] =
    Arbitrary(Gen.listOf(Gen.oneOf(Generators.eulerBrick, Generators.smallCuboid)).map(Summary.of))

  given Arbitrary[Stats.Counts] =
    val count = Gen.chooseNum(0L, 1_000_000L)
    Arbitrary(Gen.zip(count, count, count, count).map(Stats.Counts.apply.tupled))

  checkAll("Summary", CommutativeMonoidTests[Summary].commutativeMonoid)
  checkAll("Stats.Counts", CommutativeMonoidTests[Stats.Counts].commutativeMonoid)

  property("summarising two lists one by one and combining = summarising them together"):
    val bricks = Gen.listOf(Generators.eulerBrick)
    Prop.forAll(bricks, bricks)((xs, ys) => Summary.of(xs ++ ys) == (Summary.of(xs) |+| Summary.of(ys)))

  test("a summary of the bricks with smallest edge up to 1000"):
    val found = Summary.of(FastSearch.eulerBricksBetween(1, 1000))
    assertEquals(found.bricks, 69L) // RESULTS.md: 69 bricks, 14 primitive
    assertEquals(found.primitive, 14L)
    assertEquals(found.perfect, 0L)
    assertEquals(found.byDigits.values.sum, 69L)
