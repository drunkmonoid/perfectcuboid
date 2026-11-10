package perfectcuboid.web

import perfectcuboid.*

/** The box drawn in 3D, as SVG text: its twelve edges, a diagonal of each kind of face from one
  * corner, and the space diagonal, each labelled with its length, whole lengths and irrational
  * ones in different colours. Pure, so it can be tested without a browser; the page redraws it
  * as the box is turned.
  */
object Drawing:

  /** A point in space, and one on the drawing. */
  final case class P3(x: Double, y: Double, z: Double)
  final case class P2(x: Double, y: Double)

  val Width = 480
  val Height = 340

  /** The box's proportions, its longest edge 1. No edge is drawn shorter than a twentieth, so
    * that a very flat brick still shows as a box; the labels say how long each edge really is.
    */
  def proportions(box: BigCuboid): P3 =
    val longest = List(box.a, box.b, box.c).max.toDouble
    def size(e: BigInt) = math.max(e.toDouble / longest, 0.05)
    P3(size(box.a), size(box.b), size(box.c))

  /** Turned by `yaw` about the vertical and tilted by `pitch`, then seen from the front. */
  def project(p: P3, yaw: Double, pitch: Double): P2 =
    val (cy, sy) = (math.cos(yaw), math.sin(yaw))
    val x1 = p.x * cy - p.z * sy
    val z1 = p.x * sy + p.z * cy
    val (cp, sp) = (math.cos(pitch), math.sin(pitch))
    val y1 = p.y * cp - z1 * sp
    P2(x1, y1)

  /** The eight corners of a box of these sizes, centred. */
  def corners(size: P3): List[P3] =
    for x <- List(0.0, 1.0); y <- List(0.0, 1.0); z <- List(0.0, 1.0)
    yield P3((x - 0.5) * size.x, (y - 0.5) * size.y, (z - 0.5) * size.z)

  /** Pairs of corners an edge joins: those that differ in one coordinate. */
  val edges: List[(Int, Int)] =
    for i <- (0 until 8).toList; j <- (i + 1 until 8).toList if Integer.bitCount(i ^ j) == 1 yield (i, j)

  private def length(square: BigInt): (text: String, whole: Boolean) =
    summon[WholeNumber[BigInt]]
      .exactSqrt(square)
      .fold((text = s"√$square", whole = false))(r => (text = r.toString, whole = true))

  private def escape(text: String) = text.replace("&", "&amp;").replace("<", "&lt;")

  /** The drawing of the box, turned by `yaw` and `pitch`, in radians. */
  def svg(box: BigCuboid, yaw: Double, pitch: Double): String =
    val size = proportions(box)
    val points = corners(size).map(project(_, yaw, pitch))
    // Fit the drawing to the frame, whatever the turn.
    val (minX, maxX) = (points.map(_.x).min, points.map(_.x).max)
    val (minY, maxY) = (points.map(_.y).min, points.map(_.y).max)
    val scale = math.min((Width - 80) / math.max(maxX - minX, 1e-9), (Height - 60) / math.max(maxY - minY, 1e-9))
    // Centred, whichever of width and height decided the scale.
    val (left, bottom) = ((Width - (maxX - minX) * scale) / 2, (Height + (maxY - minY) * scale) / 2)
    def at(p: P2) = P2(left + (p.x - minX) * scale, bottom - (p.y - minY) * scale)
    val screen = points.map(at)
    def line(i: Int, j: Int, kind: String) =
      val (p, q) = (screen(i), screen(j))
      f"""<line class="$kind" x1="${p.x}%.1f" y1="${p.y}%.1f" x2="${q.x}%.1f" y2="${q.y}%.1f"/>"""
    val centre = P2(screen.map(_.x).sum / 8, screen.map(_.y).sum / 8)
    // A label a fraction of the way along a line, pushed off it to the side away from the
    // centre, so that the labels of the lines from one corner do not sit on each other.
    def label(i: Int, j: Int, along: Double, text: String, kind: String) =
      val (p, q) = (screen(i), screen(j))
      val (mx, my) = (p.x + (q.x - p.x) * along, p.y + (q.y - p.y) * along)
      val norm = math.max(math.hypot(q.x - p.x, q.y - p.y), 1e-9)
      val (nx, ny) = (-(q.y - p.y) / norm, (q.x - p.x) / norm)
      val away = if (mx - centre.x) * nx + (my - centre.y) * ny >= 0 then 1.0 else -1.0
      val (x, y) = (mx + away * nx * 11, my + away * ny * 11 + 4)
      val words = escape(text)
      f"""<text class="$kind" x="$x%.1f" y="$y%.1f" text-anchor="middle">$words</text>"""
    // Corner 0 is (0, 0, 0); 1, 2 and 4 are along z, y and x; the edges along x are a, along y b, along z c.
    val (a, b, c) = (box.a, box.b, box.c)
    val ab = length(a * a + b * b)
    val ac = length(a * a + c * c)
    val bc = length(b * b + c * c)
    val space = length(a * a + b * b + c * c)
    def kind(whole: Boolean) = if whole then "whole" else "irrational"
    val diagonals = List(
      (0, 6, ab, "face"), // across the face with edges a and b
      (0, 5, ac, "face"), // a and c
      (0, 3, bc, "face"), // b and c
      (0, 7, space, "space")
    )
    val body =
      edges.map((i, j) => line(i, j, "edge")) ++
        diagonals.map((i, j, len, what) => line(i, j, s"$what ${kind(len.whole)}")) ++
        List(
          label(0, 4, 0.5, a.toString, "length whole"),
          label(0, 2, 0.5, b.toString, "length whole"),
          label(0, 1, 0.5, c.toString, "length whole")
        ) ++
        diagonals.map((i, j, len, what) =>
          label(i, j, if what == "space" then 0.4 else 0.62, len.text, s"length ${kind(len.whole)}")
        )
    s"""<svg viewBox="0 0 $Width $Height" role="img" aria-label="The box ${box.a} × ${box.b} × ${box.c}, its diagonals and their lengths">""" +
      body.mkString + "</svg>"
