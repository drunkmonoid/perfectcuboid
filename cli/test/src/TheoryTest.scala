package perfectcuboid

import org.scalacheck.Prop.forAll
import Generators.*

class TheoryTest extends munit.ScalaCheckSuite:

  val bricks: Vector[Cuboid] = Search.eulerBricks(20_000)

  test("every Euler brick found obeys every rule that applies to it"):
    for brick <- bricks do assertEquals(Theory.violations(Theory.rulesFor(brick), brick).map(_.name), Nil, brick)

  test("the primitive rules are really about primitive bricks"):
    val doubled = Cuboid(88, 234, 480)
    assert(Theory.violations(Theory.primitiveEulerBrickRules, doubled).nonEmpty)

  test("the rules are not trivially true"):
    val box = Cuboid(1, 2, 3)
    assertEquals(Theory.violations(Theory.eulerBrickRules, box).size, Theory.eulerBrickRules.size)

  property("scaling an Euler brick keeps the general rules"):
    forAll(eulerBrick)(c => Theory.violations(Theory.eulerBrickRules, c).isEmpty)

  test("the command line reports that all rules hold"):
    assert(Cli.run(Report.Rules(5000)).exists(_.contains("All rules hold")))
