package perfectcuboid.web

import org.scalajs.dom
import perfectcuboid.*

/** What the page shows for a box: the same facts as the `check` and `curve` commands. Pure,
  * so it can be tested without a browser.
  */
object Check:

  final case class Result(
      box: String,
      faces: List[String],
      space: String,
      verdict: String,
      primitive: Option[String],
      derived: Option[String],
      curves: List[String]
  )

  private def length(square: BigInt): String =
    summon[WholeNumber[BigInt]].exactSqrt(square).fold(s"√$square")(_.toString)

  private def show(box: BigCuboid): String = s"${box.a} × ${box.b} × ${box.c}"

  /** The box with these edges, as typed, or why there is none. */
  def apply(edges: List[String]): Either[String, Result] =
    val numbers = edges
      .map(_.trim.replace("_", "").replace(" ", ""))
      .map(e => e.toIntOption.map(BigInt(_)).orElse(scala.util.Try(BigInt(e)).toOption))
    numbers match
      case List(Some(a), Some(b), Some(c)) if a > 0 && b > 0 && c > 0 =>
        if List(a, b, c).exists(_.bitLength > 200) then Left("Edges of up to 60 digits, please.")
        else Right(result(BigCuboid.of(a, b, c).get))
      case _ => Left("Give three positive whole numbers.")

  def result(box: BigCuboid): Result =
    val verdict = box.kind match
      case Kind.Perfect => "A PERFECT CUBOID: all seven lengths are whole!"
      case Kind.Body    => "An Euler brick: only the space diagonal is irrational."
      case Kind.Face    => "A face cuboid: only one face diagonal is irrational."
      case _            =>
        val irrational = box.faceDiagonals.count(_.isEmpty) + (if box.spaceDiagonal.isEmpty then 1 else 0)
        s"$irrational of the seven lengths are irrational."
    val curves =
      if !box.isEuler then Nil
      else
        List((box.a, box.b, box.c), (box.a, box.c, box.b), (box.b, box.c, box.a)).flatMap: (p, q, third) =>
          val curve = EulerCurve(p, q)
          curve
            .pointOf(third)
            .toList
            .flatMap: point =>
              curve.brickOf(curve.add(point, point)).map(brick => s"keeping $p and $q: ${show(brick)}")
    Result(
      show(box),
      Box.faceDiagonalSquares(box.a, box.b, box.c).map(length),
      length(Box.spaceDiagonalSquare(box.a, box.b, box.c)),
      verdict,
      Option.when(!box.isPrimitive)(show(box.primitive)),
      Option.when(box.isEuler)(show(Derived.derived(box))),
      curves
    )

/** The page itself: reads the three edges whenever they change, and shows the result. */
object Page:

  private def escape(text: String): String = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

  def render(outcome: Either[String, Check.Result]): String = outcome match
    case Left(problem) => s"""<p class="problem">${escape(problem)}</p>"""
    case Right(r)      =>
      val rows = List(
        Some("box" -> r.box),
        Some("face diagonals" -> r.faces.mkString(", ")),
        Some("space diagonal" -> r.space),
        r.primitive.map("a copy of" -> _),
        r.derived.map("derived brick" -> _)
      ).flatten.map((k, v) => s"<dt>${escape(k)}</dt><dd>${escape(v)}</dd>").mkString
      val curves =
        if r.curves.isEmpty then ""
        else
          s"<h2>New bricks from its elliptic curves (2P)</h2><ul>${r.curves.map(c => s"<li>${escape(c)}</li>").mkString}</ul>"
      s"""<p class="verdict">${escape(r.verdict)}</p><dl>$rows</dl>$curves"""

  def main(args: Array[String]): Unit =
    val inputs = List("a", "b", "c").map(id => dom.document.getElementById(id).asInstanceOf[dom.html.Input])
    val output = dom.document.getElementById("result")
    def update(): Unit = output.innerHTML = render(Check(inputs.map(_.value)))
    inputs.foreach(_.addEventListener("input", (_: dom.Event) => update()))
    dom.document
      .querySelectorAll("[data-box]")
      .foreach: button =>
        button.addEventListener(
          "click",
          (_: dom.Event) =>
            val edges = button.getAttribute("data-box").split(" ")
            inputs.zip(edges).foreach((input, edge) => input.value = edge)
            update()
        )
    update()
