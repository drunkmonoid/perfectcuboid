package perfectcuboid

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

class BigCuboidTest extends ScalaCheckSuite:

  property("a BigCuboid gives the same answers as the Cuboid with the same edges"):
    forAll(Generators.cuboid): cuboid =>
      val big = BigCuboid.from(cuboid)
      big.faceDiagonals == cuboid.faceDiagonals.map(_.map(BigInt(_))) &&
      big.spaceDiagonal == cuboid.spaceDiagonal.map(BigInt(_)) &&
      big.isEuler == cuboid.isEuler && big.isPrimitive == cuboid.isPrimitive && big.kind == cuboid.kind

  property("the same for Euler bricks, whose answers are more interesting"):
    forAll(Generators.eulerBrick): brick =>
      val big = BigCuboid.from(brick)
      big.isEuler && big.kind == Kind.Body && big.toCuboid == Right(brick)

  test("Saunderson's formula gives an Euler brick for every triple, however large"):
    val bricks = Families.primitiveTriples.take(10_000).map(Families.saundersonBig).toList
    bricks.foreach(brick => assert(brick.isEuler, brick))
    // Far too large for a Cuboid: even their edges, not just their squares, pass Long.MaxValue.
    assert(bricks.map(_.c).max > Long.MaxValue)

  test("scaling far beyond a Long keeps an Euler brick an Euler brick"):
    val k = BigInt(10).pow(30)
    val big = BigCuboid.of(44 * k, 117 * k, 240 * k).get
    assert(big.isEuler && !big.isPrimitive)
    assertEquals(big.primitive, BigCuboid.of(44, 117, 240).get)
    assert(big.toCuboid.isLeft)

  test("only positive edges make a box"):
    assertEquals(BigCuboid.of(0, 1, 2), None)
    assertEquals(BigCuboid.of(3, 1, 2).map(b => (b.a, b.b, b.c)), Some((BigInt(1), BigInt(2), BigInt(3))))

  test("Long arithmetic in the shared checks throws on overflow instead of being wrong"):
    val _ = intercept[ArithmeticException](Box.spaceDiagonalSquare(Long.MaxValue / 2, 2L, 3L))
