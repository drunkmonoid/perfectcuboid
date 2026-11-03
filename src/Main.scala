package perfectcuboid

import cats.effect.{ExitCode, IO}
import cats.syntax.all.*
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import fs2.Stream
import fs2.io.file.Path
import Stats.Counts
import scala.concurrent.ExecutionContext.Implicits.global

/** The lines of the `unbounded` report, shared by its two versions. */
object Unbounded:

  def header(maxSmallestEdge: Long): String =
    s"Euler bricks with smallest edge <= $maxSmallestEdge, other edges unbounded (* = primitive)"

  def line(brick: Cuboid): String =
    if brick.isPerfect then s"${Cli.describe(brick)}  PERFECT!" else Cli.describe(brick)

  def footer(maxSmallestEdge: Long): String = s"Done: every smallest edge up to $maxSmallestEdge searched."

  /** The report as an fs2 stream: the blocks of edges are searched on every core, and each
    * brick is printed as soon as its block, and every block before it, is done.
    */
  def stream(maxSmallestEdge: Long): Stream[IO, String] =
    Stream.emit(header(maxSmallestEdge)) ++
      Blocks.finds(Quarry.eulerBricks, 1, maxSmallestEdge).map(line) ++
      Stream.emit(footer(maxSmallestEdge))

/** The program's entry point. `scala-cli run . -- --help` lists the commands.
  *
  * `CommandIOApp` parses the command line with `CommandLine.opts`, and prints the help
  * instead when that fails. Nothing here does anything itself: `execute` returns an `IO`, a
  * description of the program, and the runtime carries it out. Ctrl+C cancels it cleanly,
  * closing any open file.
  */
object Main extends CommandIOApp("perfectcuboid", CommandLine.header):

  def main: Opts[IO[ExitCode]] = CommandLine.opts.map(execute(_).as(ExitCode.Success))

  def execute(command: Command): IO[Unit] = command match
    case Command.Hunt(file, upTo, quarry)     => Hunt.run(quarry, Path(file), upTo)
    case Command.Verify(file, quarry, blocks) => Verify.run(quarry, Path(file), blocks).flatMap(_.traverse_(IO.println))
    case Command.Bench(limit, runs)           => IO.blocking(Bench.run(limit, runs))
    // Each line is computed and printed in turn, so long outputs are never held in memory,
    // and Ctrl+C can stop between any two lines.
    case Command.Print(Report.Unbounded(limit)) => Unbounded.stream(limit).foreach(IO.println).compile.drain
    case Command.Print(report) => Stream.fromIterator[IO](Cli.run(report).iterator, 1).foreach(IO.println).compile.drain

object Cli:

  /** Turns a report into the lines to print. Pure, so it can be tested.
    *
    * The lines form a LazyList: each one is only computed when it is about to be printed,
    * so long searches show their results as they go.
    */
  def run(report: Report): LazyList[String] = report match
    case Report.Bricks(limit)               => bricksReport(limit)
    case Report.Unbounded(maxSmallestEdge)  => unboundedReport(maxSmallestEdge)
    case Report.Rules(limit)                => rulesReport(limit)
    case Report.Saunderson(limit)           => saundersonReport(limit)
    case Report.Legs(leg)                   => legsReport(leg)
    case Report.FaceCuboids(maxSharedEdge)  => faceReport(maxSharedEdge)
    case Report.EdgeCuboids(maxSquaredEdge) => edgeReport(maxSquaredEdge)
    case Report.NearMisses                  => nearMissesReport
    case Report.Stats(maxExponent)          => statsReport(maxExponent)
    case Report.Check(a, b, c)              => checkReport(a, b, c)
    case Report.LocalRules(maxPrime)        => localReport(maxPrime)
    case Report.DerivedPairs(limit)         => derivedReport(limit)
    case Report.Curve(a, b, c, multiples)   => curveReport(a, b, c, multiples)
    case Report.BrickOrigins(limit)         => originsReport(limit)
    case Report.Graph(limit)                => graphReport(limit)
    case Report.FourDimensions(limit)       => fourDReport(limit)
    case Report.Closest(limit)              => closestReport(limit)

  def bricksReport(limit: Long): LazyList[String] =
    val bricks = Search.eulerBricks(limit)
    s"Euler bricks with every edge <= $limit: ${bricks.size} (* = primitive)"
      #:: bricks.map(describe).to(LazyList)
      :+ verdict(bricks.filter(_.isPerfect))

  /** Nothing here keeps the bricks once they are described, so memory use stays flat. `Main`
    * prints the same lines from a stream instead, `Unbounded.stream`.
    */
  def unboundedReport(maxSmallestEdge: Long): LazyList[String] =
    val bricks = FastSearch.allEulerBricksParallel().takeWhile(_.a <= maxSmallestEdge)
    Unbounded.header(maxSmallestEdge) #:: bricks.map(Unbounded.line) :+ Unbounded.footer(maxSmallestEdge)

  def rulesReport(limit: Long): LazyList[String] =
    val bricks = Search.eulerBricks(limit)
    val broken =
      for
        brick <- bricks
        rule <- Theory.violations(Theory.rulesFor(brick), brick)
      yield s"  ${describe(brick)} breaks: ${rule.name}"
    val ruleNames = (Theory.eulerBrickRules ++ Theory.primitiveEulerBrickRules).map(r => s"  - ${r.name}")
    val summary =
      if broken.isEmpty then s"All rules hold for all ${bricks.size} Euler bricks with every edge <= $limit."
      else s"${broken.size} violations:"
    ("Rules checked:" +: ruleNames :+ summary :++ broken).to(LazyList)

  def saundersonReport(limit: Long): LazyList[String] =
    val bricks = Families.saundersonBricksUpTo(limit)
    s"Saunderson bricks with every edge <= $limit: ${bricks.size} (* = primitive)"
      #:: bricks.map(describe).to(LazyList)
      :+ verdict(bricks.filter(_.isPerfect).toVector)

  /** Beyond this many divisors of a², the legs are counted but not listed. */
  val MaxDivisorsToList: BigInt = 2_000_000

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

  def faceReport(maxSharedEdge: Long): LazyList[String] =
    val cuboids =
      LazyList.iterate(1L)(_ + 1).takeWhile(_ <= maxSharedEdge).flatMap(NearMisses.faceCuboidsWithSharedEdge)
    s"Face cuboids whose whole face diagonals meet at an edge <= $maxSharedEdge"
      #:: cuboids.map(c => describeSquared(c.squared.get)) // face cuboids keep to SquaredCuboid.MaxEdge
      :+ "Done."

  def edgeReport(maxSquaredEdge: Long): LazyList[String] =
    val cuboids =
      LazyList.iterate(2L)(_ + 1).takeWhile(_ <= maxSquaredEdge).flatMap(NearMisses.edgeCuboidsWithSquaredEdge)
    s"Edge cuboids whose irrational edge is at most √$maxSquaredEdge"
      #:: cuboids.map(describeSquared)
      :+ "Done."

  def nearMissesReport: LazyList[String] =
    LazyList(
      "The first cuboid of each kind, in the order each search finds them:",
      "  Perfect   none known",
      s"  Body      ${describeSquared(Search.allEulerBricks.head.squared.get)}",
      s"  Face      ${describeSquared(NearMisses.allFaceCuboids.head.squared.get)}",
      s"  Edge      ${describeSquared(NearMisses.allEdgeCuboids.head)}"
    )

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

  def closestReport(maxSmallestEdge: Long): LazyList[String] =
    val ranked = ClosestCalls.ranked(FastSearch.allEulerBricksParallel().takeWhile(_.a <= maxSmallestEdge).toVector)
    s"The primitive Euler bricks with smallest edge <= $maxSmallestEdge whose space diagonal is nearest a whole number:" #::
      ranked
        .take(12)
        .to(LazyList)
        .map: (brick, miss) =>
          val diagonal = math.sqrt(brick.a.toDouble * brick.a + brick.b.toDouble * brick.b + brick.c.toDouble * brick.c)
          f"  ${s"${brick.a} × ${brick.b} × ${brick.c}"}%-42s space diagonal ${math.round(diagonal)}%,d off by $miss%.2e"

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

  def originsReport(maxSmallestEdge: Long): LazyList[String] =
    val primitive = FastSearch
      .allEulerBricksParallel()
      .takeWhile(_.a <= maxSmallestEdge)
      .filter(_.isPrimitive)
      .map(BigCuboid.from)
      .toVector
    val origins = Origins.classify(primitive)
    def count(of: Origins.Origin => Boolean) = origins.count((_, origin) => of(origin))
    def show(box: BigCuboid) = s"${box.a} × ${box.b} × ${box.c}"
    LazyList(
      s"Primitive Euler bricks with smallest edge <= $maxSmallestEdge: ${primitive.size}",
      s"  Saunderson bricks:                      ${count(_.saunderson)}",
      s"  derived bricks of smaller bricks:       ${count(_.derivedFromSmaller)}",
      s"  2P, 3P or 4P on another brick's curve:  ${count(_.curveMultiple)}",
      s"  at least one of these:                  ${count(_.any)}",
      s"  none of these, found only by searching: ${count(!_.any)}"
    ) ++ origins.collect { case (brick, origin) if origin.curveMultiple => s"  curve multiple: ${show(brick)}" }

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
    val primitive = FastSearch
      .allEulerBricksParallel()
      .takeWhile(_.a <= maxSmallestEdge)
      .filter(_.isPrimitive)
      .map(BigCuboid.from)
      .toVector
    val inRange = primitive.toSet
    def show(box: BigCuboid) = s"${box.a} × ${box.b} × ${box.c}"
    val partners = primitive.map(box => box -> Derived.derived(box))
    val bothInRange = partners.filter((box, partner) => inRange(partner) && Ordering[BigCuboid].lt(box, partner))
    LazyList(
      s"${primitive.size} primitive Euler bricks with smallest edge <= $maxSmallestEdge.",
      s"${bothInRange.size} pairs have both partners among them, ${partners.count((box, _) => Derived.isSelfDerived(box))} bricks are their own partner:"
    ) ++ bothInRange.map((box, partner) => f"  ${show(box)}%-34s ↔  ${show(partner)}")

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

  def describe(cuboid: Cuboid): String =
    val edges = s"${cuboid.a} × ${cuboid.b} × ${cuboid.c}"
    val faces = cuboid.faceDiagonals.flatten.mkString(", ")
    val mark = if cuboid.isPrimitive then "*" else " "
    f"$mark $edges%-20s face diagonals $faces"

  def verdict(perfect: Vector[Cuboid]): String =
    if perfect.isEmpty then "No perfect cuboid (as expected)."
    else s"PERFECT CUBOID FOUND: ${perfect.mkString(", ")}"
