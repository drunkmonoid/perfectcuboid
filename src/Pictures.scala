package perfectcuboid

import scala.concurrent.ExecutionContext.Implicits.global

/** The charts in `docs/`, from search results. */
object Pictures:

  /** How the number of Euler bricks grows with the smallest edge: all of them, and the primitive ones. */
  def growth(maxExponent: Int): String =
    val rows = Stats.growth(maxExponent, FastSearch.allEulerBricksParallel())
    Charts.lines(
      "Euler bricks with smallest edge up to N",
      "N",
      "bricks",
      List(
        Charts.Series("all bricks", 1, rows.map(r => (r.limit.toDouble, r.counts.bySmallest.toDouble))),
        Charts.Series("primitive bricks", 2, rows.map(r => (r.limit.toDouble, r.counts.bySmallestPrimitive.toDouble)))
      )
    )

  /** 7 as ⁷, for exponents in titles. */
  def superscript(n: Int): String = n.toString.map(d => "⁰¹²³⁴⁵⁶⁷⁸⁹" (d - '0'))

  /** Each primitive brick as a dot: its smallest edge across, its longest edge up. */
  def bricks(maxExponent: Int): String =
    val limit = BigInt(10).pow(maxExponent).toLong
    val primitive = FastSearch.allEulerBricksParallel().takeWhile(_.a <= limit).filter(_.isPrimitive).toList
    Charts.scatter(
      s"The ${primitive.size} primitive Euler bricks with smallest edge up to 10${superscript(maxExponent)}",
      "smallest edge",
      "longest edge",
      primitive.map(c => (c.a.toDouble, c.c.toDouble, s"${c.a} × ${c.b} × ${c.c}"))
    )
