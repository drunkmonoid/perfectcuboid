package perfectcuboid

import cats.effect.{ExitCode, IO}
import cats.syntax.all.*
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import fs2.Stream
import fs2.io.file.{Files, Path}
import Stats.Counts
import scala.concurrent.ExecutionContext.Implicits.global

/** The lines of the `unbounded` report, shared by its two versions. */
object Unbounded:

  /** The report's first line. */
  def header(maxSmallestEdge: Long): String =
    s"Euler bricks with smallest edge <= $maxSmallestEdge, other edges unbounded (* = primitive)"

  /** One brick's line, marked if it is perfect. */
  def line(brick: Cuboid): String =
    if brick.isPerfect then s"${Cli.describe(brick)}  PERFECT!" else Cli.describe(brick)

  /** The report's last line. */
  def footer(maxSmallestEdge: Long): String = s"Done: every smallest edge up to $maxSmallestEdge searched."

  /** The report as an fs2 stream: the blocks of edges are searched on every core, and each
    * brick is printed as soon as its block, and every block before it, is done.
    */
  def stream(maxSmallestEdge: Long, threads: Int): Stream[IO, String] =
    Stream.emit(header(maxSmallestEdge)) ++
      Blocks.finds(Quarry.eulerBricks, 1, maxSmallestEdge, threads).map(line) ++
      Stream.emit(footer(maxSmallestEdge))

/** The program's entry point. `scala-cli run . -- --help` lists the commands.
  *
  * `CommandIOApp` parses the command line with `CommandLine.opts`, and prints the help
  * instead when that fails. Nothing here does anything itself: `execute` returns an `IO`, a
  * description of the program, and the runtime carries it out. Ctrl+C cancels it cleanly,
  * closing any open file.
  */
object Main extends CommandIOApp("perfectcuboid", CommandLine.header):

  /** What `CommandIOApp` runs: the parsed command, carried out. */
  def main: Opts[IO[ExitCode]] = CommandLine.opts.map(execute(_).as(ExitCode.Success))

  /** The program for one command, as an `IO` that does nothing until it is run. */
  def execute(command: Command): IO[Unit] = command match
    case Command.Hunt(file, upTo, quarry, threads, blockSize, from, processes) =>
      if processes == 1 then Processes.stoppedWithParent(Hunt.run(quarry, Path(file), upTo, blockSize, threads, from))
      else
        val each = math.max(1, threads / processes)
        Processes
          .huntOnProcesses(quarry, Path(file), upTo, processes, each, blockSize, from)
          .flatMap(_.traverse_(IO.println))
    case Command.Analyse(file, primitiveOnly, charts) =>
      val quarry = if primitiveOnly then Quarry.primitiveBricks else Quarry.eulerBricks
      Analysis.ofFile(quarry, Path(file), primitiveOnly, charts.map(Path(_))).flatMap(_.traverse_(IO.println))
    case Command.Families(file, maxHypotenuse) =>
      Analysis
        .read(Quarry.eulerBricks, Path(file))
        .flatMap: (bricks, resume) =>
          if resume.from != 1 || resume.searchedUpTo < 1 then
            IO.println(s"$file must be a hunt from 1, with a checkpoint")
          else
            val catalogue = bricks.filter(_.isPrimitive).map(BigCuboid.from).toSet
            IO.blocking(Coverage.lines(catalogue, resume.searchedUpTo, maxHypotenuse)).flatMap(_.traverse_(IO.println))
    case Command.Densities(file) =>
      Analysis
        .read(Quarry.eulerBricks, Path(file))
        .flatMap((bricks, _) => IO.blocking(Local.densityLines(bricks)))
        .flatMap(_.traverse_(IO.println))
    case Command.Records(file, legsUpTo) =>
      for
        (bricks, resume) <- Analysis.read(Quarry.eulerBricks, Path(file))
        _ <- Records
          .lines(bricks, f"with smallest edge from ${resume.from}%,d to ${resume.searchedUpTo}%,d")
          .traverse_(IO.println)
        all <- Blocks.finds(Quarry.eulerBricks, 1, legsUpTo).compile.toVector
        _ <- IO.println("")
        _ <- IO.blocking(Records.legsLines(all, legsUpTo)).flatMap(_.traverse_(IO.println))
      yield ()
    case Command.Graph(file, chart) =>
      for
        (bricks, resume) <- Analysis.read(Quarry.eulerBricks, Path(file))
        primitive = bricks.filter(_.isPrimitive).map(BigCuboid.from)
        which = f"with smallest edge up to ${resume.searchedUpTo}%,d"
        graph <- IO.blocking(BrickGraph.of(primitive))
        _ <- BrickGraph.lines(graph, which).traverse_(IO.println)
        _ <- chart.traverse_ : c =>
          fs2.Stream
            .emit(BrickGraph.chart(graph))
            .through(Files[IO].writeUtf8(Path(c)))
            .compile
            .drain >> IO.println(s"Wrote $c")
      yield ()
    case Command.Fields(limit) => IO.blocking(FiniteFields.lines(limit)).flatMap(_.traverse_(IO.println))
    case Command.BFiles(file, directory, published) =>
      def lines(dir: String, name: String) = Files[IO].readUtf8Lines(Path(dir) / name).compile.toList
      for
        (bricks, resume) <- Analysis.read(Quarry.eulerBricks, Path(file))
        before <- published.fold(IO.pure(List.empty[OeisFiles.Row])): dir =>
          (lines(dir, "b031173.txt"), lines(dir, "b031174.txt"), lines(dir, "b031175.txt")).mapN(OeisFiles.published)
        _ <- OeisFiles.rows(bricks, resume.searchedUpTo, before) match
          case Left(why)  => IO.println(s"No b-files: $why")
          case Right(all) =>
            Files[IO].createDirectories(Path(directory)) >>
              OeisFiles
                .files(all, before.size, resume.searchedUpTo)
                .traverse_ : f =>
                  fs2.Stream
                    .emits(f.lines.map(_ + "\n"))
                    .through(Files[IO].writeUtf8(Path(directory) / f.name))
                    .compile
                    .drain >> IO.println(s"Wrote ${Path(directory) / f.name}: ${all.size} rows")
      yield ()
    case Command.Misses(file) =>
      Analysis
        .read(Quarry.eulerBricks, Path(file))
        .flatMap((bricks, resume) => IO.blocking(Misses.lines(bricks, resume.searchedUpTo)))
        .flatMap(_.traverse_(IO.println))
    case Command.Forecast(file, target) =>
      Analysis
        .read(Quarry.eulerBricks, Path(file))
        .flatMap: (bricks, resume) =>
          if resume.from != 1 || resume.searchedUpTo < 10_000_000 then
            IO.println(s"$file must be a hunt from 1 to at least 10,000,000")
          else if target <= resume.searchedUpTo then IO.println(f"UPTO must be above $file's ${resume.searchedUpTo}%,d")
          else IO.blocking(Forecast.lines(bricks, resume.searchedUpTo, target)).flatMap(_.traverse_(IO.println))
    case Command.NearCalls(file, face)     => Analysis.nearCalls(face, Path(file)).flatMap(_.traverse_(IO.println))
    case Command.Merge(out, files, quarry) =>
      Merge.run(quarry, Path(out), files.map(Path(_))).flatMap(_.traverse_(IO.println))
    case Command.Pictures(directory, exponent) =>
      val dir = Path(directory)
      def write(name: String, svg: String) =
        fs2.Stream.emit(svg).through(Files[IO].writeUtf8(dir / name)).compile.drain >> IO.println(
          s"Wrote ${dir / name}"
        )
      Files[IO].createDirectories(dir) >>
        IO.blocking(Pictures.growth(exponent)).flatMap(write("growth.svg", _)) >>
        IO.blocking(Pictures.bricks(exponent)).flatMap(write("bricks.svg", _))
    case Command.Verify(file, quarry, blocks) => Verify.run(quarry, Path(file), blocks).flatMap(_.traverse_(IO.println))
    case Command.Bench(limit, runs)           => IO.blocking(Bench.run(limit, runs))
    case Command.Scaling(first, edges, blockSize, threads, chart) =>
      Scaling
        .run(first, edges, blockSize, threads)
        .flatMap: rows =>
          chart.traverse_ : file =>
            Stream.emit(Scaling.chart(rows)).through(Files[IO].writeUtf8(Path(file))).compile.drain >>
              IO.println(s"Wrote $file")
    // Each line is computed and printed in turn, so long outputs are never held in memory,
    // and Ctrl+C can stop between any two lines.
    case Command.Print(Report.Unbounded(limit, threads)) =>
      Unbounded.stream(limit, threads).foreach(IO.println).compile.drain
    case Command.Print(report) => Stream.fromIterator[IO](Cli.run(report).iterator, 1).foreach(IO.println).compile.drain

/** The reports that only print lines, and the descriptions they share. */
object Cli:

  /** Turns a report into the lines to print. Pure, so it can be tested.
    *
    * The lines form a LazyList: each one is only computed when it is about to be printed,
    * so long searches show their results as they go.
    */
  def run(report: Report): LazyList[String] = report match
    case Report.Bricks(limit)               => bricksReport(limit)
    case Report.Unbounded(limit, threads)   => unboundedReport(limit, threads)
    case Report.Rules(limit)                => rulesReport(limit)
    case Report.Saunderson(limit)           => saundersonReport(limit)
    case Report.Legs(leg)                   => legsReport(leg)
    case Report.FaceCuboids(maxSharedEdge)  => faceReport(maxSharedEdge)
    case Report.CrossCheck(limit)           => crossCheckReport(limit)
    case Report.Parallelepipeds(limit)      => parallelepipedReport(limit)
    case Report.EdgeCuboids(maxSquaredEdge) => edgeReport(maxSquaredEdge)
    case Report.NearMisses                  => nearMissesReport
    case Report.Stats(maxExponent)          => statsReport(maxExponent)
    case Report.Check(a, b, c)              => checkReport(a, b, c)
    case Report.LocalRules(maxPrime)        => localReport(maxPrime)
    case Report.Certificates(maxPrime)      => LazyList.from(Local.certificate(maxPrime))
    case Report.DerivedPairs(limit)         => derivedReport(limit)
    case Report.Curve(a, b, c, multiples)   => curveReport(a, b, c, multiples)
    case Report.BrickOrigins(limit)         => originsReport(limit)
    case Report.Graph(limit)                => graphReport(limit)
    case Report.FourDimensions(limit)       => fourDReport(limit)
    case Report.Closest(limit)              => closestReport(limit)

  /** Every Euler brick with every edge up to `limit`. */
  def bricksReport(limit: Long): LazyList[String] =
    val bricks = Search.eulerBricks(limit)
    s"Euler bricks with every edge <= $limit: ${bricks.size} (* = primitive)"
      #:: bricks.map(describe).to(LazyList)
      :+ verdict(bricks.filter(_.isPerfect))

  /** Nothing here keeps the bricks once they are described, so memory use stays flat. `Main`
    * prints the same lines from a stream instead, `Unbounded.stream`.
    */
  def unboundedReport(maxSmallestEdge: Long, threads: Int): LazyList[String] =
    val bricks = FastSearch.allEulerBricksParallel(cores = threads).takeWhile(_.a <= maxSmallestEdge)
    Unbounded.header(maxSmallestEdge) #:: bricks.map(Unbounded.line) :+ Unbounded.footer(maxSmallestEdge)

  /** The known rules, checked on every Euler brick with every edge up to `limit`. */
  def rulesReport(limit: Long): LazyList[String] =
    Analysis.rules(Search.eulerBricks(limit), s"with every edge <= $limit").to(LazyList)

  /** Saunderson's bricks with every edge up to `limit`. */
  def saundersonReport(limit: Long): LazyList[String] =
    val bricks = Families.saundersonBricksUpTo(limit)
    s"Saunderson bricks with every edge <= $limit: ${bricks.size} (* = primitive)"
      #:: bricks.map(describe).to(LazyList)
      :+ verdict(bricks.filter(_.isPerfect).toVector)

  /** Beyond this many divisors of a², the legs are counted but not listed. */
  val MaxDivisorsToList: BigInt = 2_000_000

  /** The factorisation of a, and every leg it pairs with. */
  def legsReport(a: Long): LazyList[String] =
    val factors = NumberTheory.factorize(a)
    val shown = factors.map((p, k) => if k == 1 then s"$p" else s"$p^$k").mkString(" · ")
    def triples(n: Long) = if n == 1 then "1 Pythagorean triple" else s"$n Pythagorean triples"
    val counts =
      s"$a is a leg of ${triples(Counting.asLeg(factors))} (${Counting.asPrimitiveLeg(a, factors)} primitive), " +
        s"and the hypotenuse of ${Counting.asHypotenuse(factors)} (${Counting.asPrimitiveHypotenuse(a, factors)} primitive)."
    val divisorsOfSquare = NumberTheory.divisorCount(factors.map((p, k) => p -> 2 * k))
    if divisorsOfSquare > MaxDivisorsToList then LazyList(s"$a = $shown", counts, "Too many legs to list.")
    else
      val legs = Pythagorean.legsPairedWithAny(a, factors)
      // y can be up to a²/2, so y² needs BigInt.
      def hypotenuse(y: BigInt) = Arith.isqrtBig(BigInt(a).pow(2) + y.pow(2))
      s"$a = $shown" #:: counts #:: legs.map(y => s"  $a² + $y² = ${hypotenuse(y)}²").to(LazyList)

  /** The face cuboids, by the edge their whole face diagonals share. */
  def faceReport(maxSharedEdge: Long): LazyList[String] =
    val cuboids =
      LazyList.iterate(1L)(_ + 1).takeWhile(_ <= maxSharedEdge).flatMap(NearMisses.faceCuboidsWithSharedEdge)
    s"Face cuboids whose whole face diagonals meet at an edge <= $maxSharedEdge"
      #:: cuboids.map(c => describeSquared(c.squared.get)) // face cuboids keep to SquaredCuboid.MaxEdge
      :+ "Done."

  /** The face cuboids found two independent ways, compared. */
  def crossCheckReport(limit: Long): LazyList[String] =
    val (fromDiagonals, fromSharedEdges) = NearMisses.bothWays(limit)
    val missing = fromSharedEdges.diff(fromDiagonals) ++ fromDiagonals.diff(fromSharedEdges)
    LazyList(
      s"Face cuboids with space diagonal <= $limit, found two independent ways:",
      s"  from the space diagonal, split twice into two legs: ${fromDiagonals.size}",
      s"  from the shared edge, pairing its legs:             ${fromSharedEdges.size}",
      if missing.isEmpty then "  the same cuboids both ways" else s"  ${missing.size} found only one way:"
    ) ++ missing.map(c => "    " + describeSquared(c.squared.get)) ++
      LazyList("The first few:") ++ fromDiagonals.take(5).map(c => "  " + describeSquared(c.squared.get))

  /** Sawyer and Reiter's perfect parallelepiped checked, and a search for more. */
  def parallelepipedReport(maxEdge: Long): LazyList[String] =
    val example = Parallelepiped.sawyerReiter
    val found = Parallelepiped.search(maxEdge)
    LazyList(
      "Sawyer and Reiter's perfect parallelepiped (2009):",
      s"  ${example.describe}",
      s"  every length whole: ${example.isPerfect}, a solid box: ${example.isSolid}",
      s"Perfect parallelepipeds with every edge <= $maxEdge: ${found.size}" +
        (if found.exists(_.isCuboid) then ", A PERFECT CUBOID AMONG THEM!" else ", none of them a cuboid")
    ) ++ found.map: shape =>
      s"  ${shape.describe}" + shape.scaledFrom.fold("")((k, from) =>
        s"  ($k × the one with edges ${from.a}, ${from.b}, ${from.c})"
      )

  /** The edge cuboids, by the square of their irrational edge. */
  def edgeReport(maxSquaredEdge: Long): LazyList[String] =
    val cuboids =
      LazyList.iterate(2L)(_ + 1).takeWhile(_ <= maxSquaredEdge).flatMap(NearMisses.edgeCuboidsWithSquaredEdge)
    s"Edge cuboids whose irrational edge is at most √$maxSquaredEdge"
      #:: cuboids.map(describeSquared)
      :+ "Done."

  /** The first cuboid of each kind with exactly one irrational length. */
  def nearMissesReport: LazyList[String] =
    LazyList(
      "The first cuboid of each kind, in the order each search finds them:",
      "  Perfect   none known",
      s"  Body      ${describeSquared(Search.allEulerBricks.head.squared.get)}",
      s"  Face      ${describeSquared(NearMisses.allFaceCuboids.head.squared.get)}",
      s"  Edge      ${describeSquared(NearMisses.allEdgeCuboids.head)}"
    )

  /** How the number of Euler bricks grows, up to 10^maxExponent. */
  def statsReport(maxExponent: Int): LazyList[String] =
    val rows = Stats.growth(maxExponent, FastSearch.allEulerBricksParallel())
    def growth(counts: Counts => Long) =
      rows
        .zip(rows.tail)
        .map((r1, r2) => f"${Stats.exponent(r1.limit, counts(r1.counts), r2.limit, counts(r2.counts))}%.2f")
    val table = rows.map: r =>
      f"| 10^${math.log10(r.limit.toDouble).round} | ${r.counts.byLargest}%,d | ${r.counts.byLargestPrimitive}%,d | ${r.counts.bySmallest}%,d | ${r.counts.bySmallestPrimitive}%,d |"
    val (saunderson, primitive) =
      Stats.saundersonShare(rows.last.limit min 1_000_000L, FastSearch.allEulerBricksParallel())
    LazyList(
      "| N | all edges <= N | primitive | smallest edge <= N | primitive |",
      "| --- | --- | --- | --- | --- |"
    ) ++ table ++ LazyList(
      "",
      s"Growth exponent k (count ≈ C·N^k) between successive rows:",
      s"  all edges <= N, primitive:     ${growth(_.byLargestPrimitive).mkString(", ")}",
      s"  smallest edge <= N, primitive: ${growth(_.bySmallestPrimitive).mkString(", ")}",
      s"Saunderson's formula gives $saunderson of the $primitive primitive bricks with every edge <= ${rows.last.limit min 1_000_000L}."
    )

  /** The primitive bricks whose space diagonal is nearest a whole number. */
  def closestReport(maxSmallestEdge: Long): LazyList[String] =
    val bricks = FastSearch.allEulerBricksParallel().takeWhile(_.a <= maxSmallestEdge).toVector
    Analysis.closest(bricks, s"with smallest edge <= $maxSmallestEdge").to(LazyList)

  /** Four edges with all six pairs whole, or five of six. */
  def fourDReport(maxSmallestEdge: Long): LazyList[String] =
    val bricks = FastSearch.allEulerBricksParallel().takeWhile(_.a <= maxSmallestEdge).toVector
    val found = FourD.search(bricks)
    val (full, near) = found.partition(_.wholePairs == 6)
    val primitive = near.filter(_.isPrimitive)
    LazyList(
      s"From the ${bricks.size} Euler bricks with smallest edge <= $maxSmallestEdge:",
      s"  four edges with all six pairs whole (a 4D Euler brick): ${full.size}",
      s"  four edges with five of six pairs whole: ${near.size}, ${primitive.size} of them primitive",
      "The first primitive ones, by longest edge:"
    ) ++ (full ++ primitive.sortBy(_.edges.last)).take(10).map(q => s"  ${q.edges.mkString(" × ")}")

  /** The Pythagorean graph: degrees, pieces and triangles. */
  def graphReport(limit: Long): LazyList[String] =
    val graph = PythagoreanGraph.upTo(limit)
    val pieces = graph.components
    val (busiest, most) = graph.neighbours.view.mapValues(_.size).maxBy((x, d) => (d, -x))
    val degrees = graph.degreeCounts.toList.sorted
    val triangles = graph.triangles
    LazyList(
      s"The Pythagorean graph on 1 to $limit: x and y joined when x² + y² is a square.",
      s"  numbers with a neighbour: ${graph.neighbours.size} of $limit",
      s"  links:                    ${graph.links}",
      s"  most neighbours:          $busiest, with $most",
      s"  numbers by neighbours:    ${degrees.take(8).map((d, n) => s"$d: $n").mkString(", ")}, …",
      s"  connected pieces:         ${pieces.size}, the largest with ${pieces.headOption.fold(0)(_.size)} numbers",
      s"  triangles (Euler bricks): ${triangles.size}",
      s"  numbers in a triangle:    ${triangles.flatMap((x, y, z) => List(x, y, z)).distinct.size}"
    )

  /** Which primitive bricks a formula or a map explains. */
  def originsReport(maxSmallestEdge: Long): LazyList[String] =
    Analysis.origins(primitiveUpTo(maxSmallestEdge), s"with smallest edge <= $maxSmallestEdge").to(LazyList)

  /** For each pair of edges of the brick, the bricks from the multiples of its point. */
  def curveReport(a: BigInt, b: BigInt, c: BigInt, multiples: Int): LazyList[String] =
    val brick = BigCuboid.of(a, b, c).get // the command line only lets positive edges through
    def show(n: BigInt) = if n.toString.length <= 40 then n.toString else s"(${n.toString.length} digits)"
    if !brick.isEuler then LazyList(s"$a × $b × $c is not an Euler brick.")
    else
      val (x, y, z) = (brick.a, brick.b, brick.c)
      LazyList((x, y, z), (x, z, y), (y, z, x)).flatMap: (p, q, third) =>
        val curve = EulerCurve(p, q)
        val point = curve.pointOf(third).get // an Euler brick's third edge always gives a point
        s"On y² = x(x + $p²)(x + $q²), the point of edge $third gives:" #::
          curve
            .multiples(point)
            .take(multiples)
            .zipWithIndex
            .map: (m, i) =>
              val found = curve.brickOf(m).get // every multiple of a brick's point has a square x
              s"  ${i + 1}P: ${show(found.a)} × ${show(found.b)} × ${show(found.c)}"

  /** Each primitive brick in range with its derived partner, each pair once. */
  def derivedReport(maxSmallestEdge: Long): LazyList[String] =
    Analysis.derived(primitiveUpTo(maxSmallestEdge), s"with smallest edge <= $maxSmallestEdge").to(LazyList)

  private def primitiveUpTo(maxSmallestEdge: Long): Vector[BigCuboid] =
    FastSearch
      .allEulerBricksParallel()
      .takeWhile(_.a <= maxSmallestEdge)
      .filter(_.isPrimitive)
      .map(BigCuboid.from)
      .toVector

  /** Each prime's rules are only worked out when its line is about to be printed. */
  def localReport(maxPrime: Int): LazyList[String] =
    def describe(rules: List[Rule]) = if rules.isEmpty then "nothing" else rules.map(_.name).mkString("; ")
    "What every primitive brick must look like, from trying every remainder mod a prime power:" #::
      NumberTheory.smallPrimes.iterator
        .map(_.toInt)
        .takeWhile(_ <= maxPrime)
        .to(LazyList)
        .flatMap: p =>
          val (q, k) = Local.modulus(p)
          val euler = Local.rules(q, k, perfect = false)
          val perfect = Local.rules(q, k, perfect = true)
          LazyList(
            s"  mod $q^$k",
            s"    Euler bricks:    ${describe(euler)}",
            s"    perfect cuboids: ${
                if perfect.map(_.name) == euler.map(_.name) then "the same" else describe(perfect)
              }"
          )

  /** All seven lengths of one box of any size, and what kind it is. */
  def checkReport(a: BigInt, b: BigInt, c: BigInt): LazyList[String] =
    val box = BigCuboid.of(a, b, c).get // the command line only lets positive edges through
    def length(square: BigInt) = summon[WholeNumber[BigInt]].exactSqrt(square).fold(s"√$square")(_.toString)
    val faces = Box.faceDiagonalSquares(box.a, box.b, box.c).map(length).mkString(", ")
    val verdict = box.kind match
      case Kind.Perfect => "A PERFECT CUBOID: all seven lengths are whole!"
      case Kind.Body    =>
        val partner = Derived.derived(box)
        s"An Euler brick (body cuboid): only the space diagonal is irrational. Derived brick: ${partner.a} × ${partner.b} × ${partner.c}."
      case Kind.Face => "A face cuboid: only one face diagonal is irrational."
      case _         =>
        val irrational = box.faceDiagonals.count(_.isEmpty) + (if box.spaceDiagonal.isEmpty then 1 else 0)
        s"$irrational of the seven lengths are irrational."
    LazyList(
      s"${box.a} × ${box.b} × ${box.c}" + (if box.isPrimitive then ""
                                           else
                                             s"  (${box.primitive.a} × ${box.primitive.b} × ${box.primitive.c} scaled up)"),
      s"  face diagonals $faces",
      s"  space diagonal ${length(Box.spaceDiagonalSquare(box.a, box.b, box.c))}",
      verdict
    )

  /** All 7 lengths, irrational ones written as √n. */
  def describeSquared(c: SquaredCuboid): String =
    def length(square: Long) = Arith.exactSqrt(square).fold(s"√$square")(_.toString)
    val edges = List(c.a2, c.b2, c.c2).map(length).mkString(" × ")
    val faces = List(c.a2 + c.b2, c.a2 + c.c2, c.b2 + c.c2).map(length).mkString(", ")
    s"$edges   face diagonals $faces   space diagonal ${length(c.a2 + c.b2 + c.c2)}"

  /** A brick's edges and face diagonals, starred if it is primitive. */
  def describe(cuboid: Cuboid): String =
    val edges = s"${cuboid.a} × ${cuboid.b} × ${cuboid.c}"
    val faces = cuboid.faceDiagonals.flatten.mkString(", ")
    val mark = if cuboid.isPrimitive then "*" else " "
    f"$mark $edges%-20s face diagonals $faces"

  /** The last line of a search: whether it found a perfect cuboid. */
  def verdict(perfect: Vector[Cuboid]): String =
    if perfect.isEmpty then "No perfect cuboid (as expected)."
    else s"PERFECT CUBOID FOUND: ${perfect.mkString(", ")}"
