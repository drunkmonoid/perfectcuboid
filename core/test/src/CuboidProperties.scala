package perfectcuboid

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import Generators.*

class CuboidProperties extends munit.ScalaCheckSuite:

  property("the order of the edges does not matter"):
    forAll(edge, edge, edge): (x, y, z) =>
      val all = List(x, y, z).permutations.map(p => Cuboid.of(p(0), p(1), p(2))).toSet
      all.size == 1

  property("of succeeds exactly when every edge is positive"):
    val anyEdge = Gen.chooseNum(-10L, 10L)
    forAll(anyEdge, anyEdge, anyEdge)((x, y, z) => Cuboid.of(x, y, z).isDefined == (x > 0 && y > 0 && z > 0))

  property("edges come out sorted"):
    forAll(cuboid)(c => c.a <= c.b && c.b <= c.c)

  property("scaling a cuboid does not change its kind"):
    forAll(smallCuboid, Gen.chooseNum(2L, 1000L)): (c, k) =>
      c.scaled(k).map(_.kind) == Right(c.kind)

  property("scaling fails, without overflowing, exactly when an edge would pass Edge.Max"):
    forAll(cuboid, Gen.chooseNum(1L, Long.MaxValue)): (c, k) =>
      c.scaled(k).isRight == (BigInt(c.c) * k <= Edge.Max)

  property("a perfect cuboid is also an Euler brick"):
    forAll(cuboid)(c => !c.isPerfect || c.isEuler)

  property("multiples of Euler bricks are Euler bricks, never perfect"):
    forAll(eulerBrick)(c => c.isEuler && !c.isPerfect && c.kind == Kind.Body)

  property("dividing by the common factor gives a primitive cuboid"):
    forAll(cuboid): c =>
      val g = Arith.gcd(Arith.gcd(c.a, c.b), c.c)
      Cuboid.of(c.a / g, c.b / g, c.c / g).get.isPrimitive
