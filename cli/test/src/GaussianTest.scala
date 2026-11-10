package perfectcuboid

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import Gaussian.G

class GaussianTest extends munit.ScalaCheckSuite:

  val small: Gen[G] = for re <- Gen.chooseNum(-1000L, 1000L); im <- Gen.chooseNum(-1000L, 1000L) yield G(re, im)

  property("the square root of a square is one of its two roots"):
    forAll(small): w =>
      Gaussian.sqrt(w.squared).exists(r => r == w || r == w.negate)

  property("a number with no root has none"):
    forAll(small): z =>
      Gaussian.sqrt(z).forall(_.squared == z)

  test("roots of a few numbers"):
    assertEquals(Gaussian.sqrt(G(3, 4)), Some(G(2, 1)))
    assertEquals(Gaussian.sqrt(G(-1, 0)), Some(G(0, 1)))
    assertEquals(Gaussian.sqrt(G(2, 0)), None)
    assertEquals(Gaussian.sqrt(G(0, 2)), Some(G(1, 1)))

  test("a box and its variants by sign, order, i and conjugation have one name"):
    val box = List(G(5, 0), G(0, 3), G(0, 4))
    val variants = List(
      box.reverse,
      box.map(_.negate),
      box.map(_.timesI),
      box.map(_.conjugate),
      List(G(0, -4), G(-5, 0), G(0, 3))
    )
    variants.foreach(v => assertEquals(Gaussian.canonical(v), Gaussian.canonical(box), v))

  test("with a diagonal 0 allowed there are perfect boxes, like 3i × 4i × 5; with none, not up to 12"):
    val degenerate = Gaussian.perfect(12, zeroAllowed = true)
    assert(degenerate.contains(Gaussian.canonical(List(G(0, 3), G(0, 4), G(5, 0)))), degenerate)
    assertEquals(Gaussian.perfect(12), Nil)
    degenerate.foreach: edges =>
      val List(a, b, c) = edges.map(_.squared): @unchecked
      List(a + b, a + c, b + c, a + b + c).foreach(s => assert(Gaussian.isSquare(s), edges))
      assert((a + b + c).isZero, edges)
