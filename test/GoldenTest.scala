package perfectcuboid

import java.nio.file.{Files, Path, Paths}

/** Golden tests: the whole output of each command is saved in `test/golden`, and every run
  * compares against it. A refactor that changes any line of output fails here, with a diff.
  *
  * When a change of output is intended, accept the new output with
  *
  *   UPDATE_GOLDEN=1 scala-cli test .
  *
  * and review the changed files in `git diff` before committing them.
  */
class GoldenTest extends munit.FunSuite:

  val dir: Path = Paths.get("test", "golden")
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
  golden("face", "face", "300")
  golden("edge", "edge", "5e5")
  golden("nearmisses", "nearmisses")
  golden("stats", "stats", "4")
  golden("help", "--help")
  golden("bad-number", "bricks", "0")
