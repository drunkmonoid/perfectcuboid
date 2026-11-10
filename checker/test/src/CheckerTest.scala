package checker

class CheckerTest extends munit.FunSuite:

  /** What `perfectcuboid certificates 7` writes, as kept by its golden test. */
  val small: List[String] =
    val source =
      scala.io.Source.fromFile(s"${sys.env.getOrElse("MILL_TEST_RESOURCE_DIR", "checker/test/resources")}/small.txt")
    try source.getLines().toList
    finally source.close()

  test("the certificate for the primes up to 7 checks"):
    val result = Checker.problems(small)
    assertEquals(result.problems, Nil)
    assertEquals(result.cases, 8)

  test("a pattern left out is noticed"):
    val i = small.indexWhere(_.startsWith("pattern "))
    assert(Checker.problems(small.patch(i, Nil, 1)).problems.nonEmpty)

  test("a pattern that cannot occur is noticed"):
    val i = small.indexWhere(_.startsWith("pattern "))
    assert(Checker.problems(small.patch(i, List("pattern 9 9 9 | 9 9 9 | 9"), 0)).problems.nonEmpty)

  test("a rule stronger than the patterns allow is noticed"):
    val strong = small.map(l => if l == "rule edges 4 2" then "rule edges 5 2" else l)
    assert(strong != small)
    assert(Checker.problems(strong).problems.exists(_.contains("rule edges 5 2 does not hold")))

  test("valuations of remainders"):
    assertEquals(Checker.times(48, 2, 8), 4)
    assertEquals(Checker.times(0, 3, 4), 4)
    assertEquals(Checker.times(5, 5, 2), 1)
