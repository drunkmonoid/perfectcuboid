package perfectcuboid

class LocalTest extends munit.FunSuite:

  def names(p: Int, perfect: Boolean): List[String] =
    val (q, k) = Local.modulus(p)
    Local.rules(q, k, perfect).map(_.name)

  test("valuations count factors of p, and 0 counts as the whole power"):
    assertEquals(Local.valuation(12, 2, 6), 2)
    assertEquals(Local.valuation(0, 2, 6), 6)
    assertEquals(Local.valuation(7, 2, 6), 0)

  test("the computation finds the rules in Theory.scala, and proves them for every Euler brick"):
    assertEquals(names(2, perfect = false), List("one edge divisible by 16, another by 4"))
    assertEquals(names(3, perfect = false), List("one edge divisible by 9, another by 3"))
    assertEquals(names(5, perfect = false), List("one edge divisible by 5"))
    assertEquals(names(11, perfect = false), List("one edge divisible by 11"))
    assertEquals(names(7, perfect = false), Nil)

  test("and Kraitchik's rules for perfect cuboids (1945)"):
    assertEquals(names(7, perfect = true), List("one edge divisible by 7"))
    assertEquals(names(19, perfect = true), List("one edge divisible by 19"))
    assertEquals(names(13, perfect = true), List("some edge or the space diagonal divisible by 13"))
    assertEquals(names(17, perfect = true), List("some of the seven lengths divisible by 17"))
    assertEquals(names(23, perfect = true), Nil)

  test("lower powers are not enough"):
    assertEquals(Local.rules(2, 7, perfect = false).map(_.name), List("one edge divisible by 8, another by 4"))
    assertEquals(Local.rules(17, 1, perfect = true), Nil)

  test("every Euler brick found obeys every rule derived for Euler bricks"):
    val rules = List(2, 3, 5, 7, 11, 13).flatMap: p =>
      val (q, k) = Local.modulus(p)
      Local.rules(q, k, perfect = false)
    val bricks = FastSearch.eulerBricksBetween(1, 20_000)
    for brick <- bricks do assertEquals(Theory.violations(rules, brick), Nil, brick)
