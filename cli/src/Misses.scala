package perfectcuboid

/** Where the space diagonals of the bricks fall between whole numbers: a test of the chance
  * heuristic (`Chance`), which supposes they fall anywhere, evenly.
  *
  * If √N, N the square of a brick's space diagonal, lands at a random point between two whole
  * numbers, its fractional part is spread evenly over [0, 1). The Kolmogorov–Smirnov test says
  * how far the fractional parts found are from that: D is the largest gap between their
  * cumulative share and the even one, and p the chance of a gap at least that large from
  * evenly spread numbers. A small p would mean the diagonals are drawn somewhere.
  */
object Misses:

  /** The fractional part of √n, accurately even for n far beyond a Double: with r = ⌊√n⌋,
    * √n − r = (n − r²) / (√n + r), whose numerator is exact and whose denominator needs only a
    * Double's relative precision.
    */
  def fraction(n: BigInt): Double =
    val r = Arith.isqrtBig(n)
    val rest = n - r * r
    rest.toDouble / (math.sqrt(n.toDouble) + r.toDouble)

  /** The fractional part of a brick's space diagonal. */
  def fraction(brick: Cuboid): Double =
    fraction(BigInt(brick.a).pow(2) + BigInt(brick.b).pow(2) + BigInt(brick.c).pow(2))

  /** The Kolmogorov–Smirnov statistic D of these numbers against the even spread on [0, 1). */
  def ksStatistic(xs: Seq[Double]): Double =
    val sorted = xs.sorted.toArray
    val n = sorted.length.toDouble
    sorted.indices.foldLeft(0.0): (d, i) =>
      val x = sorted(i)
      math.max(d, math.max((i + 1) / n - x, x - i / n))

  /** The chance of a D at least this large from n evenly spread numbers, by Kolmogorov's limit
    * Q(λ) = 2 Σ (−1)^(k−1) e^(−2k²λ²), with Stephens's correction for finite n.
    */
  def ksPValue(d: Double, n: Int): Double =
    val sn = math.sqrt(n.toDouble)
    val lambda = (sn + 0.12 + 0.11 / sn) * d
    if lambda < 0.2 then 1.0
    else
      val sum = (1 to 100).map(k => (if k % 2 == 1 then 1.0 else -1.0) * math.exp(-2.0 * k * k * lambda * lambda)).sum
      math.min(1.0, math.max(0.0, 2 * sum))

  /** One row: the bricks in a range, D and p. */
  final case class Row(which: String, bricks: Int, d: Double, p: Double)

  def row(which: String, bricks: Seq[Cuboid]): Row =
    val d = ksStatistic(bricks.map(fraction))
    Row(which, bricks.size, d, ksPValue(d, bricks.size))

  /** The test over all the primitive bricks, by decade of smallest edge, and over the bricks
    * that remainders rule out and those they do not, as lines to print.
    */
  def lines(bricks: Seq[Cuboid], searchedUpTo: Long): List[String] =
    val primitive = bricks.filter(_.isPrimitive)
    val decades = Iterator.iterate(1000L)(_ * 10).takeWhile(_ < searchedUpTo).toList :+ searchedUpTo
    val byDecade = decades
      .zip(0L :: decades)
      .map((upTo, above) => (upTo, primitive.filter(b => b.a > above && b.a <= upTo)))
      .filter(_._2.size >= 20)
      .map((upTo, these) => row(f"smallest edge to $upTo%,d", these))
    val (ruledOut, open) = primitive.partition(b => Chance.byRemainder(b) == 0)
    val rows = (row("all", primitive) :: byDecade) ++ List(
      row("ruled out by remainder", ruledOut),
      row("not ruled out", open)
    )
    val nearest = primitive.map(b => math.min(fraction(b), 1 - fraction(b))).minOption
    List(
      s"Fractional parts of the space diagonals of ${primitive.size} primitive bricks, against an even spread:",
      "| bricks | how many | Kolmogorov–Smirnov D | p |",
      "| --- | ---: | ---: | ---: |"
    ) ++ rows.map(r => f"| ${r.which} | ${r.bricks}%,d | ${r.d}%.4f | ${r.p}%.3f |") ++
      nearest.toList.map(m => f"Nearest a whole number: $m%.2e, against ${0.5 / (primitive.size + 1)}%.2e expected.")
