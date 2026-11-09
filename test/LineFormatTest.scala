package perfectcuboid

import munit.ScalaCheckSuite
import org.scalacheck.{Gen, Prop}

class LineFormatTest extends ScalaCheckSuite:

  /** A format made of other derived formats: they combine field by field. */
  final case class Pair(first: EdgesLine, weight: Long) derives LineFormat

  test("an edges line is the three edges, and anything after them is left alone"):
    assertEquals(LineFormat.write(EdgesLine(44, 117, 240)), "44 117 240")
    assertEquals(LineFormat.parse[EdgesLine]("44 117 240  PERFECT"), Some(EdgesLine(44, 117, 240)))

  test("a squared line writes irrational edges as square roots"):
    val line = SquaredLine(Length.of(461776), Length.of(1443L * 1443), Length.of(1800L * 1800))
    assertEquals(LineFormat.write(line), "√461776 1443 1800")
    assertEquals(LineFormat.parse[SquaredLine]("√461776 1443 1800"), Some(line))

  test("too few words, or words that are not numbers, read as nothing"):
    assertEquals(LineFormat.parse[EdgesLine]("44 117"), None)
    assertEquals(LineFormat.parse[EdgesLine]("44 x 240"), None)
    assertEquals(LineFormat.parse[SquaredLine]("√ 1443 1800"), None)

  test("derived formats nest"):
    assertEquals(LineFormat.write(Pair(EdgesLine(1, 2, 3), 4)), "1 2 3 4")
    assertEquals(LineFormat.parse[Pair]("1 2 3 4"), Some(Pair(EdgesLine(1, 2, 3), 4)))

  property("writing and reading back gives the same line"):
    val length = Gen.oneOf(Gen.chooseNum(0L, 3_000_000_000L).map(Length.Whole(_)), Gen.posNum[Long].map(Length.Root(_)))
    Prop.forAll(length, length, length): (a, b, c) =>
      LineFormat.parse[SquaredLine](LineFormat.write(SquaredLine(a, b, c))) == Some(SquaredLine(a, b, c))
