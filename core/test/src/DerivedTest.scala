package perfectcuboid

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

class DerivedTest extends ScalaCheckSuite:

  val bricks: Vector[BigCuboid] = FastSearch.eulerBricksBetween(1, 20_000).map(BigCuboid.from).toVector

  test("the derived brick of 44 × 117 × 240 is 429 × 880 × 2340"):
    assertEquals(Derived.derived(BigCuboid.of(44, 117, 240).get), BigCuboid.of(429, 880, 2340).get)

  test("every Euler brick's derived brick is a primitive Euler brick"):
    bricks.foreach: brick =>
      val partner = Derived.derived(brick)
      assert(partner.isEuler && partner.isPrimitive, brick)

  test("deriving twice gives back the primitive brick of the same shape"):
    bricks.foreach(brick => assertEquals(Derived.derived(Derived.derived(brick)), brick.primitive))

  property("for a box that is not an Euler brick, derived and original agree on how many faces are whole"):
    // ab, ac: a²(b² + c²) is a square exactly when b² + c² is. So derivation keeps each face's verdict,
    // moved to another face.
    forAll(Generators.smallCuboid): cuboid =>
      val box = BigCuboid.from(cuboid)
      Derived.derived(box).faceDiagonals.count(_.isDefined) == box.faceDiagonals.count(_.isDefined)

  test("no brick found is its own partner"):
    assert(bricks.forall(brick => !Derived.isSelfDerived(brick)))
