package perfectcuboid

class ChanceTest extends munit.FunSuite:

  test("every x mod m is counted once among the square roots of its square"):
    assertEquals(Chance.roots.map(_.toLong).sum, Chance.Modulus.toLong)

  test("1 has 4 square roots mod 64 and 2 mod each odd prime power, so 4 · 2⁵ in all"):
    assertEquals(Chance.roots(1), 128)

  test("a box whose space diagonal is whole has a chance, and the first Euler bricks have none"):
    val box = Cuboid(1, 2, 2) // space diagonal 3
    assertEqualsDouble(Chance.byRemainder(box), Chance.roots(9) / 6.0, 1e-12)
    assertEquals(Chance.byRemainder(Cuboid(44, 117, 240)), 0.0)
    assertEquals(Chance.byRemainder(Cuboid(85, 132, 720)), 0.0)

  test("the rows count each brick once, in the decade of its smallest edge"):
    val bricks = FastSearch.eulerBricksBetween(1, 20_000).filter(_.isPrimitive)
    val rows = Chance.byDecade(bricks, 20_000)
    assertEquals(rows.map(_.upTo), List(100L, 1000, 10_000, 20_000))
    assertEquals(rows.map(_.bricks).sum, bricks.size)
    assertEqualsDouble(rows.map(_.byRemainder).sum, bricks.map(Chance.byRemainder).sum, 1e-12)
