package perfectcuboid

import cats.syntax.all.*
import com.monovore.decline
import com.monovore.decline.{Argument, Opts}

/** The commands that only compute lines of text. Pure, so `Cli.run` can be tested directly. */
enum Report:
  case Bricks(limit: Long)
  case Unbounded(maxSmallestEdge: Long)
  case Rules(limit: Long)
  case Saunderson(limit: Long)
  case Legs(leg: Long)
  case FaceCuboids(maxSharedEdge: Long)
  case EdgeCuboids(maxSquaredEdge: Long)
  case NearMisses
  case Stats(maxExponent: Int)
  case Check(a: BigInt, b: BigInt, c: BigInt)
  case LocalRules(maxPrime: Int)
  case DerivedPairs(maxSmallestEdge: Long)
  case Curve(a: BigInt, b: BigInt, c: BigInt, multiples: Int)
  case BrickOrigins(maxSmallestEdge: Long)
  case Graph(limit: Long)
  case FourDimensions(maxSmallestEdge: Long)
  case Closest(maxSmallestEdge: Long)

/** Everything the program can do, with its arguments already checked. Hunts and benchmarks
  * touch files and clocks, so `Main` runs them; reports are only printed.
  */
enum Command:
  case Print(report: Report)
  case Hunt(file: String, upTo: Long)
  case Bench(maxSmallestEdge: Long, runs: Int)

/** The command line, described with the decline library: every subcommand, its arguments,
  * their defaults and their help. Parsing turns a list of words into a `Command`, or into
  * the help text with the reasons it failed.
  */
object CommandLine:

  val header: String = "Search for perfect cuboids, Euler bricks and their near misses."

  /** "1000000", "1_000_000" or "1e6". */
  def parseNumber(s: String): Option[Long] =
    s.replace("_", "").toLongOption.orElse(s.toDoubleOption.filter(_.isWhole).map(_.toLong))

  /** How decline reads a whole number. Without it, decline's own reader would refuse "1e6". */
  private val wholeNumber: Argument[Long] =
    Argument.from("number")(s => parseNumber(s).toValidNel(s"not a whole number: $s"))

  /** An optional number from 1 to `max`, standing for `default` when left out. */
  private def number(metavar: String, default: Long, max: Long = Long.MaxValue): Opts[Long] =
    val range = if max == Long.MaxValue then "at least 1" else s"from 1 to $max"
    Opts
      .argument[Long](metavar)(using wholeNumber)
      .withDefault(default)
      .validate(s"$metavar must be $range")(n => n >= 1 && n <= max)

  /** A positive whole number of any size. */
  private def edge(metavar: String): Opts[BigInt] =
    Opts.argument[BigInt](metavar).validate(s"$metavar must be at least 1")(_ >= 1)

  private def report(name: String, help: String)(opts: Opts[Report]): Opts[Command] =
    Opts.subcommand(name, help)(opts.map(Command.Print(_)))

  private val reports: List[Opts[Command]] = List(
    report("bricks", "Euler bricks with every edge up to LIMIT (default 1000).")(
      number("LIMIT", 1000).map(Report.Bricks(_))
    ),
    report("unbounded", "Euler bricks by smallest edge up to LIMIT (default 1000), other edges unbounded.")(
      number("LIMIT", 1000).map(Report.Unbounded(_))
    ),
    report("rules", "Check the known Euler brick rules on every brick with edges up to LIMIT (default 1000).")(
      number("LIMIT", 1000).map(Report.Rules(_))
    ),
    report("saunderson", "Saunderson's Euler bricks with every edge up to LIMIT (default 1000).")(
      number("LIMIT", 1000).map(Report.Saunderson(_))
    ),
    report("legs", "Factorise N (default 1000) and list every leg it pairs with.")(
      number("N", 1000).map(Report.Legs(_))
    ),
    report("face", "Face cuboids whose whole face diagonals meet at an edge up to LIMIT (default 1000).")(
      number("LIMIT", 1000).map(Report.FaceCuboids(_))
    ),
    report("edge", "Edge cuboids whose irrational edge is at most √LIMIT (default 1000).")(
      number("LIMIT", 1000).map(Report.EdgeCuboids(_))
    ),
    report("check", "The seven lengths of the box A × B × C, of any size, and how near perfect it is.")(
      (edge("A"), edge("B"), edge("C")).mapN(Report.Check(_, _, _))
    ),
    report("local", "Rules every Euler brick and perfect cuboid obeys mod primes up to P (default 37), by computer.")(
      Opts
        .argument[Int]("P")
        .withDefault(37)
        .validate("P must be from 2 to 43")(p => p >= 2 && p <= 43)
        .map(Report.LocalRules(_))
    ),
    report("derived", "Primitive Euler bricks with smallest edge up to LIMIT (default 100000), paired by derivation.")(
      number("LIMIT", 100_000, Search.MaxSmallestEdge).map(Report.DerivedPairs(_))
    ),
    report("curve", "New Euler bricks from the brick A × B × C, as multiples of points on elliptic curves.")(
      (
        edge("A"),
        edge("B"),
        edge("C"),
        Opts
          .option[Int]("multiples", "How many multiples of each point (default 4).")
          .withDefault(4)
          .validate("--multiples must be from 1 to 8")(n => n >= 1 && n <= 8)
      ).mapN(Report.Curve(_, _, _, _))
    ),
    report("origins", "Which primitive bricks, smallest edge up to LIMIT (default 100000), come from formulas.")(
      number("LIMIT", 100_000, Search.MaxSmallestEdge).map(Report.BrickOrigins(_))
    ),
    report("graph", "The graph on 1 to LIMIT (default 10000) joining x and y when x² + y² is a square.")(
      number("LIMIT", 10_000, 2_000_000).map(Report.Graph(_))
    ),
    report("fourd", "Four-dimensional Euler bricks and near misses from bricks with smallest edge up to LIMIT.")(
      number("LIMIT", 10_000, Search.MaxSmallestEdge).map(Report.FourDimensions(_))
    ),
    report("closest", "The primitive bricks, smallest edge up to LIMIT (default 100000), nearest to perfect.")(
      number("LIMIT", 100_000, Search.MaxSmallestEdge).map(Report.Closest(_))
    ),
    report("nearmisses", "The first body, face and edge cuboid.")(Opts(Report.NearMisses)),
    report("stats", "How the number of Euler bricks grows, up to 10^EXPONENT (3 to 9, default 6).")(
      Opts
        .argument[Int]("EXPONENT")
        .withDefault(6)
        .validate("EXPONENT must be from 3 to 9")(e => e >= 3 && e <= 9)
        .map(Report.Stats(_))
    )
  )

  private val hunt: Opts[Command] =
    Opts.subcommand("hunt", "Long search to FILE up to smallest edge UPTO. Ctrl+C stops, rerun resumes.")(
      (Opts.argument[String]("FILE"), number("UPTO", Search.MaxSmallestEdge, Search.MaxSmallestEdge))
        .mapN(Command.Hunt(_, _))
    )

  private val bench: Opts[Command] =
    Opts.subcommand("bench", "Time the readable, fast and parallel searches up to LIMIT (default 100000).")(
      (
        number("LIMIT", 100_000, Search.MaxSmallestEdge),
        Opts
          .option[Int]("runs", "How many timed runs of each search (default 5).")
          .withDefault(5)
          .validate("--runs must be at least 1")(_ >= 1)
      ).mapN(Command.Bench(_, _))
    )

  /** With no subcommand at all, list the Euler bricks with edges up to 1000. */
  val opts: Opts[Command] =
    (reports :+ hunt :+ bench).reduce(_ orElse _) orElse Opts(Command.Print(Report.Bricks(1000)))

  def parse(args: List[String]): Either[decline.Help, Command] =
    decline.Command("perfectcuboid", header)(opts).parse(args)
