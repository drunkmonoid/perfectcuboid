package perfectcuboid

/** Charts as SVG text, written by hand: an SVG file is a list of shapes with coordinates, so a
  * chart is a pure function from data to a string. Axes are logarithmic by default, since
  * most numbers here span many powers of ten.
  *
  * Colours come from a validated palette, with a darker set for dark mode chosen by the
  * viewer's system setting; each mark carries a `<title>` that browsers show on hover.
  */
object Charts:

  /** One series: its name, its palette slot (1 or 2), and its points. */
  final case class Series(name: String, slot: Int, points: List[(Double, Double)])

  private val Width = 720
  private val Height = 440
  private val (left, right, top, bottom) = (72, 150, 70, 56)

  private val style =
    """<style>
      |  svg { --surface: #fcfcfb; --ink: #0b0b0b; --ink-2: #52514e; --grid: #e6e5e0;
      |        --series-1: #2a78d6; --series-2: #eb6834; font-family: system-ui, sans-serif; }
      |  @media (prefers-color-scheme: dark) {
      |    svg { --surface: #1a1a19; --ink: #f0efec; --ink-2: #c3c2b7; --grid: #383835;
      |          --series-1: #3987e5; --series-2: #d95926; }
      |  }
      |  .surface { fill: var(--surface); }
      |  .title { fill: var(--ink); font-size: 16px; font-weight: 600; }
      |  .label { fill: var(--ink-2); font-size: 12px; }
      |  .grid { stroke: var(--grid); stroke-width: 1; }
      |  .s1 { stroke: var(--series-1); fill: var(--series-1); }
      |  .s2 { stroke: var(--series-2); fill: var(--series-2); }
      |  .line { fill: none; stroke-width: 2; }
      |  .dot { stroke: var(--surface); stroke-width: 1; }
      |  .dense { stroke: none; fill-opacity: 0.4; }
      |</style>""".stripMargin

  /** Where values fall along one side of a chart, and the grid lines drawn across it. */
  sealed trait Axis:
    def apply(value: Double): Double

    /** A grid line for each of these values, with its label. */
    def ticks: List[(Double, String)]

  /** Powers of ten from 10^lo to 10^hi, and where a value falls between them on the page. */
  final case class LogAxis(lo: Int, hi: Int, from: Double, to: Double) extends Axis:
    def apply(value: Double): Double = from + (math.log10(value) - lo) / (hi - lo) * (to - from)
    def ticks: List[(Double, String)] = (lo to hi).map(k => (math.pow(10, k), power(k))).toList

  /** Making a log axis to fit some values. */
  object LogAxis:
    /** The decades that hold every value, with a little room. */
    def around(values: Iterable[Double], from: Double, to: Double): LogAxis =
      val lo = math.floor(math.log10(values.min)).toInt
      val hi = math.max(math.ceil(math.log10(values.max)).toInt, lo + 1)
      LogAxis(lo, hi, from, to)

  /** From 0 to `hi` in equal steps, for values that span less than a power of ten or two. */
  final case class LinearAxis(hi: Double, step: Double, from: Double, to: Double) extends Axis:
    def apply(value: Double): Double = from + value / hi * (to - from)
    def ticks: List[(Double, String)] =
      (0 to math.round(hi / step).toInt).map(i => i * step).map(v => (v, f"${v.round}%,d")).toList

  /** Making a linear axis to fit some values. */
  object LinearAxis:
    /** From 0 to just above the largest value, in about five steps of 1, 2 or 5 times a power of ten. */
    def around(values: Iterable[Double], from: Double, to: Double): LinearAxis =
      val rough = values.max / 5
      val unit = math.pow(10, math.floor(math.log10(rough)))
      val step = List(1.0, 2.0, 5.0, 10.0).map(_ * unit).find(_ >= rough).getOrElse(10 * unit)
      LinearAxis(math.ceil(values.max / step) * step, step, from, to)

  private def power(k: Int): String =
    if k == 0 then "1" else if k == 1 then "10" else s"10<tspan dy=\"-6\" font-size=\"9\">$k</tspan>"

  private def escape(text: String): String = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

  /** The frame every chart shares: surface, title, axis labels, a grid line per tick. */
  private def frame(
      title: String,
      xLabel: String,
      yLabel: String,
      x: Axis,
      y: Axis,
      marks: List[String]
  ): String =
    val verticals = x.ticks.map: (value, label) =>
      val px = x(value)
      s"""<line class="grid" x1="$px" y1="$top" x2="$px" y2="${Height - bottom}"/>""" +
        s"""<text class="label" x="$px" y="${Height - bottom + 18}" text-anchor="middle">$label</text>"""
    val horizontals = y.ticks.map: (value, label) =>
      val py = y(value)
      s"""<line class="grid" x1="$left" y1="$py" x2="${Width - right}" y2="$py"/>""" +
        s"""<text class="label" x="${left - 8}" y="${py + 4}" text-anchor="end">$label</text>"""
    (List(
      s"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $Width $Height" width="$Width" height="$Height" role="img">""",
      s"<title>${escape(title)}</title>",
      style,
      s"""<rect class="surface" width="$Width" height="$Height" rx="8"/>""",
      s"""<text class="title" x="$left" y="30">${escape(title)}</text>""",
      s"""<text class="label" x="${(left + Width - right) / 2}" y="${Height - 16}" text-anchor="middle">${escape(
          xLabel
        )}</text>""",
      s"""<text class="label" transform="translate(18 ${(top + Height - bottom) / 2}) rotate(-90)" text-anchor="middle">${escape(
          yLabel
        )}</text>"""
    ) ++ verticals ++ horizontals ++ marks :+ "</svg>").mkString("\n") + "\n"

  /** A legend row under the title: a swatch and a name for each series. */
  private def legend(series: List[Series]): List[String] =
    series.zipWithIndex.flatMap: (s, i) =>
      val x = left + i * 180
      List(
        s"""<rect class="s${s.slot}" x="$x" y="40" width="12" height="12" rx="3"/>""",
        s"""<text class="label" x="${x + 18}" y="50">${escape(s.name)}</text>"""
      )

  /** Lines through each series' points, with a dot at each point and the series named at its
    * end. Both axes are logarithmic, unless `linear`; `tip` is the text shown on hovering a dot.
    */
  def lines(
      title: String,
      xLabel: String,
      yLabel: String,
      series: List[Series],
      linear: Boolean = false,
      tip: (String, Double, Double) => String = (name, x, y) => f"$name: ${y.toLong}%,d up to ${x.toLong}%,d"
  ): String =
    val all = series.flatMap(_.points)
    val (x, y) =
      if linear then
        (LinearAxis.around(all.map(_._1), left, Width - right), LinearAxis.around(all.map(_._2), Height - bottom, top))
      else (LogAxis.around(all.map(_._1), left, Width - right), LogAxis.around(all.map(_._2), Height - bottom, top))
    val marks = series.flatMap: s =>
      val path = s.points.map((a, b) => f"${x(a)}%.1f,${y(b)}%.1f").mkString("M", " L", "")
      val name = escape(s.name)
      val dots = s.points.map: (a, b) =>
        f"""<circle class="s${s.slot} dot" cx="${x(a)}%.1f" cy="${y(b)}%.1f" r="4"><title>${escape(
            tip(s.name, a, b)
          )}</title></circle>"""
      val (lastX, lastY) = s.points.last
      val label = f"""<text class="label" x="${x(lastX) + 10}%.1f" y="${y(lastY) + 4}%.1f">$name</text>"""
      (s"""<path class="s${s.slot} line" d="$path"/>""" :: dots) :+ label
    // One series needs no legend: the title names it, and so does the label at its end.
    frame(title, xLabel, yLabel, x, y, (if series.size > 1 then legend(series) else Nil) ++ marks)

  /** One dot per point, each titled with its own text. Thousands of dots would hide each other,
    * so then they are smaller and partly transparent, and where they crowd the colour deepens.
    */
  def scatter(title: String, xLabel: String, yLabel: String, points: List[(Double, Double, String)]): String =
    val x = LogAxis.around(points.map(_._1), left, Width - right)
    val y = LogAxis.around(points.map(_._2), Height - bottom, top)
    val (kind, radius) = if points.size > 2000 then ("dense", 2.0) else ("dot", 3.5)
    val marks = points.map: (a, b, text) =>
      f"""<circle class="s1 $kind" cx="${x(a)}%.1f" cy="${y(b)}%.1f" r="$radius"><title>${escape(
          text
        )}</title></circle>"""
    frame(title, xLabel, yLabel, x, y, marks)
