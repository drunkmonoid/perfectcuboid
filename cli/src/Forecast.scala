package perfectcuboid

/** What the primitive bricks found so far predict for a longer hunt, written down before it
  * runs, so that the hunt can test the models rather than be explained by them afterwards.
  *
  * Three models, each as simple as the data allows:
  *   - the count grows like N^k, with k falling slowly; k's fall is fitted to the last few
  *     ranges and carried on;
  *   - a brick's chance of being perfect (`Chance.byRemainder`) falls like 1 / a, a its smallest
  *     edge, which the table by decade shows; so the chance added by the new bricks is an
  *     integral over the count's growth;
  *   - the space diagonals miss whole numbers by amounts spread evenly between 0 and ½, so the
  *     chance that a new brick beats the closest call so far follows from how many there are.
  */
object Forecast:

  /** A count of bricks with smallest edge up to a limit. */
  type Count = (limit: Long, count: Int)

  /** The number of smallest edges up to each power of ten from 10⁶ below `searchedUpTo`, and up
    * to `searchedUpTo` itself.
    */
  def counts(smallest: Seq[Long], searchedUpTo: Long): List[Count] =
    val limits = Iterator.iterate(1_000_000L)(_ * 10).takeWhile(_ < searchedUpTo).toList :+ searchedUpTo
    limits.map(n => (limit = n, count = smallest.count(_ <= n)))

  /** A growth exponent k between two counts, placed at the middle of its range in log10. */
  type Exponent = (middle: Double, k: Double)

  /** The growth exponent between each count and the next. */
  def exponents(counts: List[Count]): List[Exponent] =
    counts
      .zip(counts.drop(1))
      .map: (c1, c2) =>
        val k = math.log(c2.count.toDouble / c1.count) / math.log(c2.limit.toDouble / c1.limit)
        (middle = (math.log10(c1.limit.toDouble) + math.log10(c2.limit.toDouble)) / 2, k = k)

  /** The least-squares slope of k against log10 N: how fast the growth slows, per decade. */
  def slope(exponents: List[Exponent]): Double =
    val n = exponents.size.toDouble
    val mx = exponents.map(_.middle).sum / n
    val my = exponents.map(_.k).sum / n
    val sxy = exponents.map(e => (e.middle - mx) * (e.k - my)).sum
    val sxx = exponents.map(e => (e.middle - mx) * (e.middle - mx)).sum
    if sxx == 0 then 0 else sxy / sxx

  /** The count forecast at a target: expected, and the range from k not falling at all to k
    * falling twice as fast; and the k used.
    */
  final case class Growth(expected: Double, low: Double, high: Double, k: Double, slope: Double)

  /** The count at `target`, carrying on from the last count with k fitted to the last three
    * exponents.
    */
  def growth(counts: List[Count], target: Long): Growth =
    val ks = exponents(counts)
    val last = counts.last
    val fall = slope(ks.takeRight(3))
    val step = (math.log10(last.limit.toDouble) + math.log10(target.toDouble)) / 2 - ks.last.middle
    def at(k: Double) = last.count * math.pow(target.toDouble / last.limit, k)
    val k = ks.last.k + fall * step
    val ends = List(at(ks.last.k), at(ks.last.k + 2 * fall * step))
    Growth(at(k), ends.min, ends.max, k, fall)

  /** The average of a brick's chance times its smallest edge, over the bricks given: the c in
    * chance ≈ c / a.
    */
  def chanceScale(bricks: Seq[Cuboid]): Double =
    if bricks.isEmpty then 0 else bricks.map(b => Chance.byRemainder(b) * b.a).sum / bricks.size

  /** The chance added by the bricks with smallest edge from s to t, when there are `count` up
    * to s and the count grows like N^k: the integral of c / a over dCount = count·k·a^(k−1) / s^k.
    */
  def chance(scale: Double, k: Double, count: Double, s: Double, t: Double): Double =
    scale * k * count / math.pow(s, k) * (math.pow(s, k - 1) - math.pow(t, k - 1)) / (1 - k)

  /** The chance that one of n new misses, spread evenly over [0, ½], is below `closest`. */
  def beats(closest: Double, n: Double): Double = 1 - math.exp(-2 * closest * n)

  /** How each pattern's share moved: in the bricks up to `split`, in those above it, and the
    * range two standard errors either side of the latter for a sample of `n` new bricks.
    */
  final case class Share(
      pattern: List[Int],
      density: Double,
      below: Double,
      above: Double,
      low: Double,
      high: Double
  )

  /** The shares of each prime's patterns below and above `split`, as `Local.densityCases`
    * counts them, with the range a sample of `n` more would fall in if the shares above held.
    */
  def shares(bricks: Seq[Cuboid], split: Long, n: Double): List[(p: Int, shares: List[Share])] =
    val (below, above) = bricks.partition(_.a <= split)
    Local.densityCases.map: (p, k, cap) =>
      val densities = Local.densities(p, k, cap)
      def share(of: Seq[Cuboid]) =
        of.groupBy(Local.patternOf(_, p, cap)).view.mapValues(_.size.toDouble / of.size).toMap
      val (b, a) = (share(below), share(above))
      val patterns = (densities.keySet ++ b.keySet ++ a.keySet).toList.sortBy(-densities.getOrElse(_, 0.0))
      (
        p = p,
        shares = patterns.map: pattern =>
          val x = a.getOrElse(pattern, 0.0)
          val error = 2 * math.sqrt(x * (1 - x) / n)
          Share(
            pattern,
            densities.getOrElse(pattern, 0.0),
            b.getOrElse(pattern, 0.0),
            x,
            x - error max 0,
            x + error min 1
          )
      )

  /** The forecast for a hunt carried on from `searchedUpTo` to `target`, as lines to print. */
  def lines(bricks: Seq[Cuboid], searchedUpTo: Long, target: Long): List[String] =
    val primitive = bricks.filter(_.isPrimitive)
    val cs = counts(primitive.map(_.a: Long), searchedUpTo)
    val g = growth(cs, target)
    val now = primitive.size
    val fresh = g.expected - now
    val split = Iterator.iterate(1L)(_ * 10).takeWhile(_ < searchedUpTo).toList.last
    val scale = chanceScale(primitive.filter(_.a > split))
    val added = chance(scale, g.k, now, searchedUpTo.toDouble, target.toDouble)
    val closest = ClosestCalls.ranked(primitive).headOption
    def pct(x: Double) = f"${100 * x}%.1f%%"
    def show(pattern: List[Int], cap: Int) =
      pattern.map(v => if v == cap then s"$v+" else v.toString).mkString("(", ", ", ")")
    List(
      f"Forecast for the primitive bricks with smallest edge from ${searchedUpTo + 1}%,d to $target%,d,",
      f"from the $now%,d up to $searchedUpTo%,d:",
      "",
      "Growth exponent k of the count, between " + cs.map(c => f"${c.limit}%,d").mkString(", ") + ": " +
        exponents(cs).map(e => f"${e.k}%.3f").mkString(", "),
      f"k falls by ${-g.slope}%.3f a decade over the last three, so k ≈ ${g.k}%.3f on the way to $target%,d.",
      f"Primitive bricks up to $target%,d: about ${g.expected}%,.0f (${g.low}%,.0f to ${g.high}%,.0f), " +
        f"so about $fresh%,.0f new.",
      "",
      f"Chance × smallest edge, averaged over the ${primitive.count(_.a > split)}%,d bricks above $split%,d: $scale%.2f.",
      f"Chance of a perfect cuboid among the new bricks: about $added%.1e.",
      ""
    ) ++ closest.toList.map: (brick, miss) =>
      f"The closest call so far, ${brick.a} × ${brick.b} × ${brick.c}, misses by $miss%.2e;" +
        s" a new brick comes closer with a chance of ${pct(beats(miss, fresh))}."
    ++ ("" :: f"Shares of each pattern, up to $split%,d, above it, and the range for the new bricks:" :: Nil)
    ++ shares(primitive, split, fresh).flatMap: (p, rows) =>
      val cap = Local.densityCases.find(_._1 == p).fold(1)(_._3)
      s"p = $p" :: rows.map: s =>
        f"  ${show(s.pattern, cap)}%-14s density ${pct(s.density)}%6s   up to $split%,d ${pct(s.below)}%6s" +
          f"   above ${pct(s.above)}%6s   new: ${pct(s.low)} to ${pct(s.high)}"
