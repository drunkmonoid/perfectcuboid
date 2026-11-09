package perfectcuboid.web

import org.scalajs.dom
import scala.scalajs.js
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

/** The search that runs in the browser: the same `FastSearch` as on the JVM, a block of edges
  * at a time, and what the page says about it. Pure, so it can be tested without a browser.
  */
object Hunting:

  val BlockSize = 500L

  /** What one block of edges adds: how many bricks, and the primitive ones, as text. */
  final case class Block(last: Long, bricks: Int, primitive: List[String])

  def block(first: Long, last: Long): Block =
    val found = FastSearch.eulerBricksBetween(first, last)
    Block(last, found.size, found.filter(_.isPrimitive).map(c => s"${c.a} × ${c.b} × ${c.c}"))

  /** The progress line: how far, how many, how fast, and how long it may take yet. */
  def progress(searchedUpTo: Long, upTo: Long, bricks: Int, primitive: Int, seconds: Double): String =
    val speed = if seconds > 0 then searchedUpTo / seconds else 0.0
    val left = if speed > 0 && searchedUpTo < upTo then f", about ${(upTo - searchedUpTo) / speed}%.0f s left" else ""
    f"Searched up to $searchedUpTo%,d of $upTo%,d: $bricks%,d Euler bricks, $primitive%,d primitive, " +
      f"${speed}%,.0f edges a second$left."

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

  /** The same script runs on the page and, started from it, as a web worker, a separate thread
    * that has no page: there it searches, so that the page stays responsive meanwhile.
    */
  def main(args: Array[String]): Unit =
    if js.typeOf(js.Dynamic.global.document) == "undefined" then worker() else page()

  /** The worker: given an edge to search up to, posts a message after every block. The page
    * stops it by terminating it, so it needs no way to be told to stop.
    */
  private def worker(): Unit =
    js.Dynamic.global.onmessage = { (event: js.Dynamic) =>
      val upTo = event.data.upTo.asInstanceOf[Double].toLong
      var first = 1L
      while first <= upTo do
        val found = Hunting.block(first, math.min(first + Hunting.BlockSize - 1, upTo))
        val _ = js.Dynamic.global.postMessage(
          js.Dynamic.literal(last = found.last.toDouble, bricks = found.bricks, primitive = js.Array(found.primitive*))
        )
        first = found.last + 1
    }: js.Function1[js.Dynamic, Unit]

  private def page(): Unit =
    checker()
    searcher()

  /** The search section: Start runs a worker up to the edge typed, Stop ends it. */
  private def searcher(): Unit =
    def element[A](id: String) = dom.document.getElementById(id).asInstanceOf[A]
    val (upTo, start, stop) =
      (element[dom.html.Input]("up-to"), element[dom.html.Button]("start"), element[dom.html.Button]("stop"))
    val (status, list) = (element[dom.html.Element]("search-status"), element[dom.html.Element]("search-bricks"))
    var running: Option[dom.Worker] = None
    def finish(): Unit =
      running.foreach(_.terminate())
      running = None
      start.disabled = false
      stop.disabled = true
    start.addEventListener(
      "click",
      (_: dom.Event) =>
        upTo.value.replace(",", "").replace("_", "").toLongOption.filter(n => n >= 1 && n <= 100_000_000) match
          case None        => status.textContent = "Give a whole number from 1 to 100,000,000."
          case Some(limit) =>
            finish()
            list.innerHTML = ""
            val begun = js.Date.now()
            var (bricks, primitive) = (0, 0)
            val worker = dom.Worker("main.js")
            worker.onmessage = (event: dom.MessageEvent) =>
              val message = event.data.asInstanceOf[js.Dynamic]
              val last = message.last.asInstanceOf[Double].toLong
              val found = message.primitive.asInstanceOf[js.Array[String]]
              bricks += message.bricks.asInstanceOf[Int]
              primitive += found.length
              found.foreach(text => list.insertAdjacentHTML("beforeend", s"<li>${escape(text)}</li>"))
              status.textContent = Hunting.progress(last, limit, bricks, primitive, (js.Date.now() - begun) / 1000)
              if last >= limit then finish()
            worker.postMessage(js.Dynamic.literal(upTo = limit.toDouble))
            running = Some(worker)
            start.disabled = true
            stop.disabled = false
    )
    stop.addEventListener(
      "click",
      (_: dom.Event) =>
        finish(); status.textContent += " Stopped."
    )

  /** The checker: reads the three edges whenever they change, and shows the result. */
  private def checker(): Unit =
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
