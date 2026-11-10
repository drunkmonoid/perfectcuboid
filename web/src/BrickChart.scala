package perfectcuboid.web

import org.scalajs.dom

/** The catalogue's bricks as a chart that answers back: smallest edge against longest, both on
  * log axes. Drag across it to zoom in, double-click or Reset to see everything again, narrow
  * the smallest edges with the two boxes, and hover a dot to read its brick; a click opens it in
  * the catalogue. The arithmetic is here and pure, the drawing below it.
  */
object BrickChart:

  /** What the chart shows: smallest edges from x0 to x1 and longest from y0 to y1. */
  final case class View(x0: Double, x1: Double, y0: Double, y1: Double)

  /** The frame: the plot inside its margins, in pixels. */
  final case class Frame(left: Double, top: Double, width: Double, height: Double)

  /** A position on a log axis from lo to hi, as a share of the way along, 0 to 1. */
  def share(value: Double, lo: Double, hi: Double): Double =
    (math.log10(value) - math.log10(lo)) / (math.log10(hi) - math.log10(lo))

  /** The value a share of the way along a log axis from lo to hi. */
  def valueAt(s: Double, lo: Double, hi: Double): Double =
    math.pow(10, math.log10(lo) + s * (math.log10(hi) - math.log10(lo)))

  /** Where a brick lands in the frame, pixels from its top left corner. */
  def place(r: Catalogue.Row, view: View, frame: Frame): (x: Double, y: Double) =
    (
      x = frame.left + share(r.brick.a.toDouble, view.x0, view.x1) * frame.width,
      y = frame.top + (1 - share(r.brick.c.toDouble, view.y0, view.y1)) * frame.height
    )

  /** Everything, with a little room at each end. */
  def whole(rows: Vector[Catalogue.Row]): View =
    if rows.isEmpty then View(1, 10, 1, 10)
    else
      View(
        rows.map(_.brick.a).min * 0.8,
        rows.map(_.brick.a).max * 1.25,
        rows.map(_.brick.c).min * 0.8,
        rows.map(_.brick.c).max * 1.25
      )

  /** The view a rectangle dragged across the frame zooms to: from one corner to the other. */
  def zoom(view: View, frame: Frame, px0: Double, py0: Double, px1: Double, py1: Double): View =
    def x(px: Double) = valueAt((px - frame.left) / frame.width, view.x0, view.x1)
    def y(py: Double) = valueAt(1 - (py - frame.top) / frame.height, view.y0, view.y1)
    View(x(math.min(px0, px1)), x(math.max(px0, px1)), y(math.max(py0, py1)), y(math.min(py0, py1)))

  /** The powers of ten from lo to hi, for the grid. */
  def ticks(lo: Double, hi: Double): List[Double] =
    (math.ceil(math.log10(lo)).toInt to math.floor(math.log10(hi)).toInt).toList.map(math.pow(10, _))

  /** The brick drawn nearest the pointer, if one is within `reach` pixels. */
  def nearest(
      rows: Vector[Catalogue.Row],
      view: View,
      frame: Frame,
      px: Double,
      py: Double,
      reach: Double
  ): Option[Catalogue.Row] =
    val inView =
      rows.filter(r => r.brick.a >= view.x0 && r.brick.a <= view.x1 && r.brick.c >= view.y0 && r.brick.c <= view.y1)
    inView
      .minByOption(r => math.hypot(place(r, view, frame).x - px, place(r, view, frame).y - py))
      .filter(r => math.hypot(place(r, view, frame).x - px, place(r, view, frame).y - py) <= reach)

  private def label(v: Double): String =
    val k = math.round(math.log10(v)).toInt
    if k < 4 then f"${v.round}%,d" else s"10${k.toString.map(d => "⁰¹²³⁴⁵⁶⁷⁸⁹" (d - '0'))}"

  /** The chart on the page: loads the bricks, then draws and redraws them. */
  def show(canvas: dom.html.Canvas): Unit =
    def element[A](id: String) = dom.document.getElementById(id).asInstanceOf[A]
    val (status, from, to) =
      (element[dom.html.Element]("chart-status"), element[dom.html.Input]("from"), element[dom.html.Input]("to"))
    val reset = element[dom.html.Button]("reset")
    val context = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    val style = dom.window.getComputedStyle(dom.document.documentElement)
    def colour(name: String) = style.getPropertyValue(name).trim
    var all = Vector.empty[Catalogue.Row]
    var view = View(1, 10, 1, 10)
    var drag: Option[(Double, Double, Double, Double)] = None
    var hover: Option[Catalogue.Row] = None
    def number(input: dom.html.Input) = input.value.replace(",", "").replace("_", "").trim.toLongOption
    def chosen = Catalogue.filter(all, None, number(from), number(to))
    def frame = Frame(64, 16, canvas.clientWidth - 80, canvas.clientHeight - 56)
    def draw(): Unit =
      // Sharp on screens with more than one pixel to the point.
      val scale = dom.window.devicePixelRatio
      canvas.width = (canvas.clientWidth * scale).toInt
      canvas.height = (canvas.clientHeight * scale).toInt
      context.setTransform(scale, 0, 0, scale, 0, 0)
      context.clearRect(0, 0, canvas.clientWidth, canvas.clientHeight)
      val f = frame
      context.font = "12px system-ui, sans-serif"
      context.lineWidth = 1
      context.strokeStyle = colour("--line")
      context.fillStyle = colour("--ink-2")
      for t <- ticks(view.x0, view.x1) do
        val x = f.left + share(t, view.x0, view.x1) * f.width
        context.beginPath(); context.moveTo(x, f.top); context.lineTo(x, f.top + f.height); context.stroke()
        context.textAlign = "center"; context.fillText(label(t), x, f.top + f.height + 18)
      for t <- ticks(view.y0, view.y1) do
        val y = f.top + (1 - share(t, view.y0, view.y1)) * f.height
        context.beginPath(); context.moveTo(f.left, y); context.lineTo(f.left + f.width, y); context.stroke()
        context.textAlign = "right"; context.fillText(label(t), f.left - 6, y + 4)
      context.textAlign = "center"
      context.fillText("smallest edge", f.left + f.width / 2, f.top + f.height + 38)
      context.save()
      context.translate(14, f.top + f.height / 2)
      context.rotate(-math.Pi / 2)
      context.fillText("longest edge", 0, 0)
      context.restore()
      val shown =
        chosen.filter(r => r.brick.a >= view.x0 && r.brick.a <= view.x1 && r.brick.c >= view.y0 && r.brick.c <= view.y1)
      context.fillStyle = colour("--accent")
      context.globalAlpha = if shown.size > 3000 then 0.45 else 0.8
      val dot = if shown.size > 3000 then 2.5 else 4.0
      shown.foreach: r =>
        val p = place(r, view, f)
        context.fillRect(p.x - dot / 2, p.y - dot / 2, dot, dot)
      context.globalAlpha = 1
      hover.foreach: r =>
        val p = place(r, view, f)
        context.strokeStyle = colour("--ink"); context.lineWidth = 2
        context.beginPath(); context.arc(p.x, p.y, 6, 0, 2 * math.Pi); context.stroke()
      drag.foreach: (x0, y0, x1, y1) =>
        context.strokeStyle = colour("--accent"); context.lineWidth = 1
        context.strokeRect(math.min(x0, x1), math.min(y0, y1), math.abs(x1 - x0), math.abs(y1 - y0))
      status.textContent = hover.fold(f"${shown.size}%,d of ${all.size}%,d primitive bricks shown"): r =>
        f"${r.brick.a} × ${r.brick.b} × ${r.brick.c}: longest edge ${r.brick.c.toDouble / r.brick.a}%,.1f times the smallest; click to open it"
    def local(e: dom.MouseEvent) =
      val box = canvas.getBoundingClientRect()
      (e.clientX - box.left, e.clientY - box.top)
    canvas.addEventListener(
      "mousedown",
      (e: dom.MouseEvent) =>
        val (x, y) = local(e)
        drag = Some((x, y, x, y))
    )
    canvas.addEventListener(
      "mousemove",
      (e: dom.MouseEvent) =>
        val (x, y) = local(e)
        drag = drag.map((x0, y0, _, _) => (x0, y0, x, y))
        if drag.isEmpty then hover = nearest(chosen, view, frame, x, y, 8)
        draw()
    )
    canvas.addEventListener(
      "mouseup",
      (e: dom.MouseEvent) =>
        val (x, y) = local(e)
        drag.foreach: (x0, y0, _, _) =>
          if math.abs(x - x0) > 6 && math.abs(y - y0) > 6 then view = zoom(view, frame, x0, y0, x, y)
          else
            // A click, not a drag: open the brick under the pointer in the catalogue.
            nearest(chosen, view, frame, x, y, 8).foreach: r =>
              dom.window.open(s"../catalogue/?from=${r.brick.a}&to=${r.brick.a}", "_top")
        drag = None
        draw()
    )
    canvas.addEventListener(
      "mouseleave",
      (_: dom.MouseEvent) =>
        hover = None; drag = None; draw()
    )
    canvas.addEventListener(
      "dblclick",
      (_: dom.MouseEvent) =>
        view = whole(chosen); draw()
    )
    reset.addEventListener(
      "click",
      (_: dom.Event) =>
        view = whole(chosen); draw()
    )
    List(from, to).foreach(
      _.addEventListener(
        "input",
        (_: dom.Event) =>
          view = whole(chosen); draw()
      )
    )
    dom.window.addEventListener("resize", (_: dom.Event) => draw())
    status.textContent = "Loading…"
    val _ = dom
      .fetch(canvas.getAttribute("data-file"))
      .`then`[String](_.text())
      .`then`[Unit]: text =>
        all = Catalogue.rows(text)
        view = whole(all)
        draw()
