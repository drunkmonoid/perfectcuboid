package perfectcuboid

class CommandLineTest extends munit.FunSuite:

  def parse(args: String*) = CommandLine.parse(args.toList)

  def errors(args: String*): List[String] = parse(args*).left.toOption.map(_.errors).getOrElse(Nil)

  test("no arguments lists the Euler bricks with edges up to 1000"):
    assertEquals(parse(), Right(Command.Print(Report.Bricks(1000))))

  test("numbers can be written with underscores or as powers of ten"):
    assertEquals(parse("legs", "1_000"), Right(Command.Print(Report.Legs(1000))))
    assertEquals(parse("unbounded", "1e6"), Right(Command.Print(Report.Unbounded(1_000_000))))
    assertEquals(parse("hunt", "bricks.txt", "2.5e7"), Right(Command.Hunt("bricks.txt", 25_000_000)))

  test("left-out numbers take their defaults"):
    assertEquals(parse("stats"), Right(Command.Print(Report.Stats(6))))
    assertEquals(parse("hunt", "bricks.txt"), Right(Command.Hunt("bricks.txt", Edge.Max)))
    assertEquals(parse("nearmisses"), Right(Command.Print(Report.NearMisses)))

  test("bad numbers are errors, not silently replaced by a default"):
    assert(errors("bricks", "lots").exists(_.contains("not a whole number")), errors("bricks", "lots"))
    assert(errors("bricks", "0").exists(_.contains("LIMIT must be at least 1")))
    assert(errors("legs", (Edge.Max + 1).toString).exists(_.contains(s"must be from 1 to ${Edge.Max}")))
    assert(errors("stats", "12").exists(_.contains("from 3 to 9")))
    assert(errors("unbounded", "1.5").nonEmpty)

  test("unknown commands and missing arguments are errors"):
    assert(errors("frobnicate").nonEmpty)
    assert(errors("hunt").nonEmpty)
    assert(errors("bricks", "10", "20").nonEmpty)

  test("--help gives the help, with no errors"):
    val help = parse("--help").left.toOption.get
    assertEquals(help.errors, Nil)
    List("bricks", "hunt", "legs", "stats").foreach(name => assert(help.toString.contains(name), name))
