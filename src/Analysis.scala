package perfectcuboid

import cats.effect.IO
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}

/** The reports that only need a list of Euler bricks, whether a search has just found them or
  * a hunt's file holds them. Each takes the bricks and a phrase saying which they are, like
  * "with smallest edge <= 30000", and is pure; only `ofFile` reads a file.
  */
object Analysis:

  def rules(bricks: Seq[Cuboid], which: String): List[String] =
    val broken =
      for
        brick <- bricks.toList
        rule <- Theory.violations(Theory.rulesFor(brick), brick)
      yield s"  ${Cli.describe(brick)} breaks: ${rule.name}"
    val ruleNames = (Theory.eulerBrickRules ++ Theory.primitiveEulerBrickRules).map(r => s"  - ${r.name}")
    val summary =
      if broken.isEmpty then s"All rules hold for all ${bricks.size} Euler bricks $which."
      else s"${broken.size} violations:"
    ("Rules checked:" +: ruleNames :+ summary) ++ broken

  /** The 12 primitive bricks whose space diagonals come nearest to whole numbers. */
  def closest(bricks: Seq[Cuboid], which: String): List[String] =
    s"The primitive Euler bricks $which whose space diagonal is nearest a whole number:" ::
      ClosestCalls
        .ranked(bricks)
        .take(12)
        .toList
        .map: (brick, miss) =>
          val diagonal = math.sqrt(brick.a.toDouble * brick.a + brick.b.toDouble * brick.b + brick.c.toDouble * brick.c)
          f"  ${s"${brick.a} × ${brick.b} × ${brick.c}"}%-42s space diagonal ${math.round(diagonal)}%,d off by $miss%.2e"

  private def show(box: BigCuboid) = s"${box.a} × ${box.b} × ${box.c}"

  def origins(primitive: Vector[BigCuboid], which: String): List[String] =
    val origins = Origins.classify(primitive)
    def count(of: Origins.Origin => Boolean) = origins.count((_, origin) => of(origin))
    List(
      s"Primitive Euler bricks $which: ${primitive.size}",
      s"  Saunderson bricks:                      ${count(_.saunderson)}",
      s"  derived bricks of smaller bricks:       ${count(_.derivedFromSmaller)}",
      s"  2P, 3P or 4P on another brick's curve:  ${count(_.curveMultiple)}",
      s"  at least one of these:                  ${count(_.any)}",
      s"  none of these, found only by searching: ${count(!_.any)}"
    ) ++ origins.collect { case (brick, origin) if origin.curveMultiple => s"  curve multiple: ${show(brick)}" }

  def derived(primitive: Vector[BigCuboid], which: String): List[String] =
    val inRange = primitive.toSet
    val partners = primitive.map(box => box -> Derived.derived(box))
    val bothInRange = partners.filter((box, partner) => inRange(partner) && Ordering[BigCuboid].lt(box, partner))
    List(
      s"${primitive.size} primitive Euler bricks $which.",
      s"${bothInRange.size} pairs have both partners among them, ${partners.count((box, _) => Derived.isSelfDerived(box))} bricks are their own partner:"
    ) ++ bothInRange.map((box, partner) => f"  ${show(box)}%-34s ↔  ${show(partner)}")

  /** How many bricks have smallest edge up to each power of ten that the bricks cover, all and
    * primitive (only primitive when `primitiveOnly`), and the growth exponents between rows.
    */
  def growth(bricks: Seq[Cuboid], searchedUpTo: Long, primitiveOnly: Boolean): List[String] =
    val limits = Iterator.iterate(1000L)(_ * 10).takeWhile(_ <= searchedUpTo).toList
    val rows = limits.map(n => (n, bricks.count(_.a <= n).toLong, bricks.count(b => b.a <= n && b.isPrimitive).toLong))
    val exponents = rows
      .zip(rows.drop(1))
      .map((r1, r2) => f"${Stats.exponent(r1._1, r1._3, r2._1, r2._3)}%.2f")
    if rows.isEmpty then Nil
    else
      List(
        if primitiveOnly then "| smallest edge <= N | primitive |" else "| smallest edge <= N | all | primitive |",
        if primitiveOnly then "| --- | ---: |" else "| --- | ---: | ---: |"
      ) ++ rows.map: (n, all, primitive) =>
        val power = s"10^${math.log10(n.toDouble).round}"
        if primitiveOnly then f"| $power | $primitive%,d |" else f"| $power | $all%,d | $primitive%,d |"
      ++ Option.when(exponents.nonEmpty)(s"Growth exponent k of the primitive count: ${exponents.mkString(", ")}")

  /** Every report above, over the bricks in a hunt's file. Growth needs the range to start at 1. */
  def report(bricks: Vector[Cuboid], from: Long, searchedUpTo: Long, primitiveOnly: Boolean): List[String] =
    val which = if from == 1 then f"with smallest edge <= $searchedUpTo%,d"
    else f"with smallest edge from $from%,d to $searchedUpTo%,d"
    val primitive = bricks.filter(_.isPrimitive).map(BigCuboid.from)
    List(
      List(f"${bricks.size}%,d ${if primitiveOnly then "primitive " else ""}Euler bricks $which"),
      if from == 1 then growth(bricks, searchedUpTo, primitiveOnly) else Nil,
      rules(bricks, which),
      derived(primitive, which).take(2),
      origins(primitive, which),
      closest(bricks, which),
      if from == 1 then Chance.lines(bricks, searchedUpTo) else Nil
    ).filter(_.nonEmpty).flatMap(section => "" :: section).drop(1)

  /** The bricks a hunt's file holds up to its last checkpoint, and its range. */
  def read(quarry: Quarry[Cuboid], path: Path): IO[(Vector[Cuboid], HuntFile.Resume)] =
    for
      resume <- Hunt.readResume(quarry, path)
      bricks <- Files[IO]
        .readUtf8Lines(path)
        .filterNot(_.startsWith("#"))
        .map(quarry.parse)
        .unNone
        .takeWhile(_.a <= resume.searchedUpTo)
        .compile
        .toVector
    yield (bricks, resume)

  /** `report` for the file at `path`, and the two charts of `Pictures`, if `charts` names a directory. */
  def ofFile(quarry: Quarry[Cuboid], path: Path, primitiveOnly: Boolean, charts: Option[Path]): IO[List[String]] =
    read(quarry, path).flatMap: (bricks, resume) =>
      if resume.searchedUpTo < resume.from then IO.pure(List(s"$path has no checkpoint yet, so nothing to analyse"))
      else
        val lines = report(bricks, resume.from, resume.searchedUpTo, primitiveOnly)
        charts.fold(IO.pure(lines)): dir =>
          val primitive = bricks.filter(_.isPrimitive).toList
          val files = List(
            "growth.svg" -> Option.when(resume.from == 1 && resume.searchedUpTo >= 10_000)(
              Pictures.growthChart(bricks, resume.searchedUpTo, primitiveOnly)
            ),
            "bricks.svg" -> Some(
              Pictures.bricksChart(primitive, s"with smallest edge up to ${Pictures.power(resume.searchedUpTo)}")
            )
          ).collect { case (name, Some(svg)) => name -> svg }
          Files[IO].createDirectories(dir) >>
            files
              .traverse((name, svg) =>
                Stream.emit(svg).through(Files[IO].writeUtf8(dir / name)).compile.drain.as(s"Wrote ${dir / name}")
              )
              .map(written => lines ++ ("" :: written))
