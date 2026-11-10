package perfectcuboid

import cats.syntax.all.*
import com.monovore.decline
import com.monovore.decline.{Argument, Opts}

/** The commands that only compute lines of text. Pure, so `Cli.run` can be tested directly. */
enum Report:
  case Bricks(limit: Long)
  case Unbounded(maxSmallestEdge: Long, threads: Int)
  case Rules(limit: Long)
  case Saunderson(limit: Long)
  case Legs(leg: Long)
  case FaceCuboids(maxSharedEdge: Long)
  case CrossCheck(maxSpaceDiagonal: Long)
  case Parallelepipeds(maxEdge: Long)
  case EdgeCuboids(maxSquaredEdge: Long)
  case NearMisses
  case Stats(maxExponent: Int)
  case Check(a: BigInt, b: BigInt, c: BigInt)
  case LocalRules(maxPrime: Int)
  case GaussianCuboids(size: Int)
  case Certificates(maxPrime: Int)
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
  case Hunt(
      file: String,
      upTo: Long,
      quarry: Quarry[?],
      threads: Int,
      blockSize: Long = perfectcuboid.Hunt.DefaultBlockSize,
      from: Option[Long] = None,
      processes: Int = 1
  )
  case Merge(out: String, files: List[String], quarry: Quarry[?])
  case Analyse(file: String, primitiveOnly: Boolean, charts: Option[String])
  case Families(file: String, maxHypotenuse: Long)
  case Densities(file: String)
  case Records(file: String, legsUpTo: Long)
  case Misses(file: String)
  case BFiles(file: String, directory: String, published: Option[String])
  case Fields(limit: Int)
  case Graph(file: String, chart: Option[String])
  case Forecast(file: String, target: Long)
  case NearCalls(file: String, face: Boolean)
  case Verify(file: String, quarry: Quarry[?], blocks: Int)
  case Pictures(directory: String, maxExponent: Int)
  case Bench(maxSmallestEdge: Long, runs: Int)
  case Scaling(first: Long, edges: Long, blockSize: Long, threads: List[Int], chart: Option[String])

/** The command line, described with the decline library: every subcommand, its arguments,
  * their defaults and their help. Parsing turns a list of words into a `Command`, or into
  * the help text with the reasons it failed.
  */
object CommandLine:

  /** The first line of `--help`. */
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

  private val threads: Opts[Int] =
    Opts
      .option[Int]("threads", "How many threads to search on (default: every core, but 1 on Scala Native).")
      .withDefault(Platform.defaultThreads)
      .validate("--threads must be at least 1")(_ >= 1)
      .validate(Platform.tooManyThreads)(Platform.threadsAllowed(_))

  private def report(name: String, help: String)(opts: Opts[Report]): Opts[Command] =
    Opts.subcommand(name, help)(opts.map(Command.Print(_)))

  private val reports: List[Opts[Command]] = List(
    report("bricks", "Euler bricks with every edge up to LIMIT (default 1000).")(
      number("LIMIT", 1000).map(Report.Bricks(_))
    ),
    report("unbounded", "Euler bricks by smallest edge up to LIMIT (default 1000), other edges unbounded.")(
      (number("LIMIT", 1000), threads).mapN(Report.Unbounded(_, _))
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
    report("crosscheck", "Face cuboids with space diagonal up to LIMIT (default 10000), found two ways, compared.")(
      number("LIMIT", 10_000, SquaredCuboid.MaxEdge).map(Report.CrossCheck(_))
    ),
    report(
      "parallelepiped",
      "Perfect parallelepipeds, slanted boxes with 13 whole lengths, with edges up to LIMIT (default 300)."
    )(
      number("LIMIT", 300, 100_000).map(Report.Parallelepipeds(_))
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
    report("gaussian", "Perfect cuboids with Gaussian integer edges, real and imaginary parts up to N (default 12).")(
      Opts
        .argument[Int]("N")
        .withDefault(12)
        .validate("N must be from 1 to 200")(n => n >= 1 && n <= 200)
        .map(Report.GaussianCuboids(_))
    ),
    report("certificates", "The rules of local for primes up to P (default 37), with the evidence, for checker.")(
      Opts
        .argument[Int]("P")
        .withDefault(37)
        .validate("P must be from 2 to 43")(p => p >= 2 && p <= 43)
        .map(Report.Certificates(_))
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

  private val kind: Opts[Quarry[?]] =
    Opts
      .option[String](
        "kind",
        "bricks (by smallest edge, the default), primitive (the same, primitive only), odd (primitive, by odd edge), face (by shared edge), diagonal (face cuboids by space diagonal) or edge (by squared edge)."
      )
      .withDefault("bricks")
      .mapValidated(kind =>
        Quarry.byName.get(kind).toValidNel(s"--kind must be one of ${Quarry.byName.keys.mkString(", ")}")
      )

  private val verify: Opts[Command] =
    Opts.subcommand("verify", "Check a hunt's FILE: every find again with BigInt, the checkpoints, a few blocks.")(
      (
        Opts.argument[String]("FILE"),
        kind,
        Opts
          .option[Int]("blocks", "How many blocks to search again (default 3).")
          .withDefault(3)
          .validate("--blocks must be at least 0")(_ >= 0)
      ).mapN(Command.Verify(_, _, _))
    )

  private val pictures: Opts[Command] =
    Opts.subcommand("pictures", "Write SVG charts to DIRECTORY, from the bricks up to 10^EXPONENT (default 6).")(
      (
        Opts.argument[String]("DIRECTORY"),
        Opts.argument[Int]("EXPONENT").withDefault(6).validate("EXPONENT must be from 3 to 8")(e => e >= 3 && e <= 8)
      ).mapN(Command.Pictures(_, _))
    )

  private val blockSize: Opts[Long] =
    Opts
      .option[Long]("block-size", s"How many edges each thread searches at a time (default ${Hunt.DefaultBlockSize}).")(
        using wholeNumber
      )
      .withDefault(Hunt.DefaultBlockSize)
      .validate("--block-size must be at least 1")(_ >= 1)

  private val hunt: Opts[Command] =
    Opts.subcommand("hunt", "Long search to FILE, up to UPTO. Ctrl+C stops, rerun resumes.")(
      (
        Opts.argument[String]("FILE"),
        number("UPTO", Search.MaxSmallestEdge, FastSearch.MaxSmallestEdge),
        kind,
        threads,
        blockSize,
        Opts
          .option[Long]("from", "Start a new FILE at this edge instead of 1, to search one range of several.")(using
            wholeNumber
          )
          .validate(s"--from must be from 1 to ${FastSearch.MaxSmallestEdge}")(n =>
            n >= 1 && n <= FastSearch.MaxSmallestEdge
          )
          .orNone,
        Opts
          .option[Int](
            "processes",
            "Split the rest of the range between this many processes, sharing --threads, and merge (default 1)."
          )
          .withDefault(1)
          .validate("--processes must be from 1 to 64")(n => n >= 1 && n <= 64)
      ).mapN(Command.Hunt(_, _, _, _, _, _, _))
    )

  private val analyse: Opts[Command] =
    Opts.subcommand("analyse", "Counts, rules, origins and closest calls of the Euler bricks in a hunt's FILE.")(
      (
        Opts.argument[String]("FILE"),
        Opts
          .option[String]("kind", "bricks (the default) or primitive, as the hunt was run.")
          .withDefault("bricks")
          .validate("--kind must be bricks or primitive")(Set("bricks", "primitive"))
          .map(_ == "primitive"),
        Opts.option[String]("charts", "Also draw the growth and the bricks as SVG charts in this directory.").orNone
      ).mapN(Command.Analyse(_, _, _))
    )

  private val families: Opts[Command] =
    Opts.subcommand("families", "Saunderson's family and those built from it, against the bricks in a hunt's FILE.")(
      (
        Opts.argument[String]("FILE"),
        Opts
          .option[Long]("hypotenuse", "Build from the triples with hypotenuse up to this (default 3000).")(using
            wholeNumber
          )
          .withDefault(3000L)
          .validate("--hypotenuse must be from 5 to 100000")(h => h >= 5 && h <= 100_000)
      ).mapN(Command.Families(_, _))
    )

  private val densities: Opts[Command] =
    Opts.subcommand(
      "densities",
      "How often small primes divide the edges of the bricks in a hunt's FILE, against prediction."
    )(
      Opts.argument[String]("FILE").map(Command.Densities(_))
    )

  private val brickGraph: Opts[Command] =
    Opts.subcommand("bricks-graph", "The primitive bricks in a hunt's FILE, joined by derivation and curve multiples.")(
      (
        Opts.argument[String]("FILE"),
        Opts.option[String]("chart", "Also write a chart of the pieces' sizes to this SVG file.").orNone
      ).mapN(Command.Graph(_, _))
    )

  private val fields: Opts[Command] =
    Opts.subcommand(
      "fields",
      "The perfect cuboid equations mod every odd prime up to P (default 100): how many solutions."
    )(
      Opts
        .argument[Int]("P")
        .withDefault(100)
        .validate("P must be from 3 to 1000")(p => p >= 3 && p <= 1000)
        .map(Command.Fields(_))
    )

  private val bfiles: Opts[Command] =
    Opts.subcommand(
      "bfiles",
      "The primitive bricks in a hunt's FILE as OEIS b-files for A031173-5, written to DIRECTORY."
    )(
      (
        Opts.argument[String]("FILE"),
        Opts.argument[String]("DIRECTORY"),
        Opts
          .option[String](
            "published",
            "A directory with the published b031173-5.txt, to keep their rows and go on from them."
          )
          .orNone
      ).mapN(Command.BFiles(_, _, _))
    )

  private val misses: Opts[Command] =
    Opts.subcommand("misses", "Where the space diagonals of the bricks in a hunt's FILE fall between whole numbers.")(
      Opts.argument[String]("FILE").map(Command.Misses(_))
    )

  private val records: Opts[Command] =
    Opts.subcommand("records", "Records among the bricks in a hunt's FILE, and bricks per edge against its legs.")(
      (
        Opts.argument[String]("FILE"),
        Opts
          .option[Long]("legs", "Count bricks and legs on every smallest edge up to this (default 1e6).")(using
            wholeNumber
          )
          .withDefault(1_000_000L)
          .validate("--legs must be from 1 to 1e8")(n => n >= 1 && n <= 100_000_000)
      ).mapN(Command.Records(_, _))
    )

  private val forecast: Opts[Command] =
    Opts.subcommand("forecast", "What the primitive bricks in a hunt's FILE predict for carrying it on to UPTO.")(
      (Opts.argument[String]("FILE"), Opts.argument[Long]("UPTO")(using wholeNumber)).mapN(Command.Forecast(_, _))
    )

  private val nearCalls: Opts[Command] =
    Opts.subcommand(
      "nearcalls",
      "The face or edge cuboids in a hunt's FILE whose irrational length is nearest a whole number."
    )(
      (
        Opts.argument[String]("FILE"),
        Opts
          .option[String]("kind", "face or edge, as the hunt was run.")
          .validate("--kind must be face or edge")(Set("face", "edge"))
          .map(_ == "face")
      ).mapN(Command.NearCalls(_, _))
    )

  private val merge: Opts[Command] =
    Opts.subcommand("merge", "Join hunts' FILEs over ranges that follow on from each other into a new file OUT.")(
      (Opts.argument[String]("OUT"), Opts.arguments[String]("FILE").map(_.toList), kind).mapN(Command.Merge(_, _, _))
    )

  private val scaling: Opts[Command] =
    Opts.subcommand(
      "scaling",
      "Time the search of EDGES edges (default 1e6) from FIRST (default 999000001) on 1 thread, 2, and so on."
    )(
      (
        number("FIRST", 999_000_001L, Search.MaxSmallestEdge),
        number("EDGES", 1_000_000L, Search.MaxSmallestEdge),
        blockSize,
        Opts
          .options[Int]("threads", "A number of threads to time; repeat it for more (default: 1 to every core).")
          .map(_.toList)
          .withDefault((1 to Platform.defaultThreads).toList)
          .validate("--threads must be at least 1")(_.forall(_ >= 1))
          .validate(Platform.tooManyThreads)(_.forall(Platform.threadsAllowed(_))),
        Opts.option[String]("chart", "Also draw the speeds as an SVG chart in this file.").orNone
      ).mapN(Command.Scaling(_, _, _, _, _))
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
    (reports :+ hunt :+ merge :+ verify :+ analyse :+ families :+ densities :+ records :+ brickGraph :+ fields :+ bfiles :+ misses :+ forecast :+ nearCalls :+ pictures :+ bench :+ scaling)
      .reduce(_ orElse _) orElse Opts(
      Command.Print(Report.Bricks(1000))
    )

  /** The command these words ask for, or the help with the reasons they don't make one. */
  def parse(args: List[String]): Either[decline.Help, Command] =
    decline.Command("perfectcuboid", header)(opts).parse(args)
