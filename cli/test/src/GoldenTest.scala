package perfectcuboid

import java.nio.file.{Files, Path, Paths}

/** Golden tests: the whole output of each command is saved in `cli/test/resources/golden`, and
  * every run compares against it. A refactor that changes any line of output fails here, with
  * a diff.
  *
  * When a change of output is intended, accept the new output with
  *
  *   UPDATE_GOLDEN=1 ./mill cli.jvm.test
  *
  * and review the changed files in `git diff` before committing them.
  */
class GoldenTest extends munit.FunSuite:

  // Mill runs tests in a folder of their own, and says where the resources are.
  val dir: Path = Paths.get(sys.env.getOrElse("MILL_TEST_RESOURCE_DIR", "cli/test/resources"), "golden")
  val update: Boolean = sys.env.contains("UPDATE_GOLDEN")

  /** What the program would print for these arguments: a report, or the help text. */
  def output(args: List[String]): String =
    CommandLine.parse(args) match
      case Right(Command.Print(report)) => Cli.run(report).mkString("", "\n", "\n")
      case Right(other)                 => fail(s"$other is not a report, so it has no fixed output")
      case Left(help)                   => help.toString + "\n"

  def golden(name: String, args: String*): Unit =
    test(s"$name: perfectcuboid ${args.mkString(" ")}"):
      val file = dir.resolve(s"$name.txt")
      val actual = output(args.toList)
      if update then Files.writeString(file, actual): Unit
      if !Files.exists(file) then fail(s"no $file yet: run with UPDATE_GOLDEN=1 to create it")
      // assertNoDiff shows the lines that differ, instead of two long strings.
      assertNoDiff(actual, Files.readString(file))

  golden("default")
  golden("bricks", "bricks", "2000")
  golden("unbounded", "unbounded", "300")
  golden("rules", "rules", "2000")
  golden("saunderson", "saunderson", "1e7")
  golden("legs", "legs", "240")
  golden("certificates", "certificates", "7")
  golden("legs-prime", "legs", "2305843009213693951") // 2⁶¹ − 1
  golden("legs-big", "legs", "3825123056546413051") // 149491 · 747451 · 34233211
  golden("legs-many", "legs", "614889782588491410") // the product of the first 15 primes
  golden("face", "face", "300")
  golden("edge", "edge", "5e5")
  golden("nearmisses", "nearmisses")
  golden("crosscheck", "crosscheck", "3000")
  golden("parallelepiped", "parallelepiped", "300")
  golden("local", "local", "19")
  golden("derived", "derived", "20000")
  golden("curve", "curve", "44", "117", "240")
  golden("curve-not-euler", "curve", "1", "2", "3")
  golden("origins", "origins", "30000")
  golden("graph", "graph", "3000")
  golden("fourd", "fourd", "10000")
  golden("closest", "closest", "30000")
  golden("stats", "stats", "4")
  golden("check-body", "check", "240", "117", "44")
  golden("check-face", "check", "104", "153", "672")
  golden("check-other", "check", "2", "4", "6")
  // Saunderson's brick from (20, 21, 29), scaled by 10¹⁵: its squares are far beyond a Long.
  golden("check-big", "check", "15939000000000000000", "18460000000000000000", "48720000000000000000")
  golden("help", "--help")
  golden("bad-number", "bricks", "0")
