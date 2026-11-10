package perfectcuboid

import scala.concurrent.ExecutionContext.Implicits.global

/** The charts in `docs/`, from search results. */
object Pictures:

  /** How the number of Euler bricks grows with the smallest edge: all of them, and the primitive ones. */
  def growth(maxExponent: Int): String =
    val rows = Stats.growth(maxExponent, FastSearch.allEulerBricksParallel())
    growthLines(
      rows.map(r =>
        (
          limit = r.limit.toDouble,
          all = r.counts.bySmallest.toDouble,
          primitive = r.counts.bySmallestPrimitive.toDouble
        )
      ),
      all = true
    )

  /** The same, from bricks already found with smallest edge up to `searchedUpTo`, such as a hunt's. */
  def growthChart(bricks: Seq[Cuboid], searchedUpTo: Long, primitiveOnly: Boolean): String =
    val limits = Iterator.iterate(1000L)(_ * 10).takeWhile(_ <= searchedUpTo).toList
    val rows = limits.map(n =>
      (
        limit = n.toDouble,
        all = bricks.count(_.a <= n).toDouble,
        primitive = bricks.count(b => b.a <= n && b.isPrimitive).toDouble
      )
    )
    growthLines(rows, all = !primitiveOnly)

  /** Rows of counts as a chart; the series of all bricks only if `all`. */
  private def growthLines(rows: List[(limit: Double, all: Double, primitive: Double)], all: Boolean): String =
    Charts.lines(
      if all then "Euler bricks with smallest edge up to N" else "Primitive Euler bricks with smallest edge up to N",
      "N",
      "bricks",
      Option.when(all)(Charts.Series("all bricks", 1, rows.map(r => (r.limit, r.all)))).toList :+
        Charts.Series("primitive bricks", 2, rows.map(r => (r.limit, r.primitive)))
    )

  /** 7 as ⁷, for exponents in titles. */
  def superscript(n: Int): String = n.toString.map(d => "⁰¹²³⁴⁵⁶⁷⁸⁹" (d - '0'))

  /** 10⁹ for a power of ten, and 1,234,567 for any other number. */
  def power(n: Long): String =
    val exponent = math.log10(n.toDouble).round.toInt
    if n >= 10 && BigInt(10).pow(exponent) == n then s"10${superscript(exponent)}" else f"$n%,d"

  /** Each primitive brick as a dot: its smallest edge across, its longest edge up. */
  def bricks(maxExponent: Int): String =
    val limit = BigInt(10).pow(maxExponent).toLong
    val primitive = FastSearch.allEulerBricksParallel().takeWhile(_.a <= limit).filter(_.isPrimitive).toList
    bricksChart(primitive, s"with smallest edge up to 10${superscript(maxExponent)}")

  /** The same for these primitive bricks, described by `which`. */
  def bricksChart(primitive: List[Cuboid], which: String): String =
    Charts.scatter(
      s"The ${primitive.size} primitive Euler bricks $which",
      "smallest edge",
      "longest edge",
      primitive.map(c => (c.a.toDouble, c.c.toDouble, s"${c.a} × ${c.b} × ${c.c}"))
    )
