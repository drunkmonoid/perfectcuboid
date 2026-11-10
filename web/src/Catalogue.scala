package perfectcuboid.web

import org.scalajs.dom
import perfectcuboid.*

/** The catalogue page: every primitive brick of a hunt, from its file, sorted and filtered.
  * Everything but `show` is pure, so it can be tested without a browser.
  */
object Catalogue:

  /** One brick, with what the table sorts by worked out once. */
  final case class Row(brick: Cuboid, flatness: Double, miss: Double)

  /** The bricks in a hunt's file, without its checkpoints, in the file's order. */
  def rows(text: String): Vector[Row] =
    text.linesIterator
      .filterNot(_.startsWith("#"))
      .flatMap(line => LineFormat.parse[EdgesLine](line).flatMap(e => Cuboid.of(e.a, e.b, e.c)))
      .map(b => Row(b, b.c.toDouble / b.a, ClosestCalls.miss(b)))
      .toVector

  enum Order(val label: String, val key: Row => Double):
    case Smallest extends Order("smallest edge", _.brick.a.toDouble)
    case Longest extends Order("longest edge", _.brick.c.toDouble)
    case Flattest extends Order("flattest first", -_.flatness)
    case Squarest extends Order("most like a cube", _.flatness)
    case Closest extends Order("nearest to perfect", _.miss)

  /** The rows with an edge equal to `edge`, if given, and smallest edge from `from` to `to`. */
  def filter(rows: Vector[Row], edge: Option[Long], from: Option[Long], to: Option[Long]): Vector[Row] =
    rows.filter: r =>
      val b = r.brick
      edge.forall(e => b.a == e || b.b == e || b.c == e) && from.forall(b.a >= _) && to.forall(b.a <= _)

  val PageSize = 50

  /** Page `page` (from 0) of the rows in this order, and how many pages there are. */
  def page(rows: Vector[Row], order: Order, page: Int): (rows: Vector[Row], pages: Int) =
    val pages = math.max(1, (rows.size + PageSize - 1) / PageSize)
    (rows = rows.sortBy(order.key).slice(page * PageSize, (page + 1) * PageSize), pages = pages)

  def tableRow(r: Row): String =
    val b = r.brick
    val box = s"${b.a},${b.b},${b.c}"
    f"""<tr><td><a href="../check/?box=$box">${b.a}%,d</a></td><td>${b.b}%,d</td><td>${b.c}%,d</td>""" +
      f"""<td>${r.flatness}%,.1f</td><td>${r.miss}%.2e</td></tr>"""

  private def number(input: dom.html.Input): Option[Long] =
    input.value.replace(",", "").replace("_", "").trim.toLongOption

  /** Loads the file named on the table, then redraws the table whenever a control changes. */
  def show(table: dom.html.Element): Unit =
    def element[A](id: String) = dom.document.getElementById(id).asInstanceOf[A]
    val (status, body) = (element[dom.html.Element]("catalogue-status"), element[dom.html.Element]("catalogue-rows"))
    val (edge, from, to) =
      (element[dom.html.Input]("edge"), element[dom.html.Input]("from"), element[dom.html.Input]("to"))
    val order = element[dom.html.Select]("order")
    val (previous, next) = (element[dom.html.Button]("previous"), element[dom.html.Button]("next"))
    Order.values.foreach(o =>
      order.insertAdjacentHTML("beforeend", s"""<option value="${o.ordinal}">${o.label}</option>""")
    )
    var all = Vector.empty[Row]
    var current = 0
    def draw(): Unit =
      val chosen = filter(all, number(edge), number(from), number(to))
      val shown = page(chosen, Order.fromOrdinal(order.value.toInt), current)
      current = current.min(shown.pages - 1)
      body.innerHTML = shown.rows.map(tableRow).mkString
      status.textContent =
        f"${chosen.size}%,d of ${all.size}%,d primitive bricks; page ${current + 1} of ${shown.pages}"
      previous.disabled = current == 0
      next.disabled = current >= shown.pages - 1
    def restart(): Unit =
      current = 0; draw()
    List(edge, from, to).foreach(_.addEventListener("input", (_: dom.Event) => restart()))
    order.addEventListener("change", (_: dom.Event) => restart())
    previous.addEventListener(
      "click",
      (_: dom.Event) =>
        current -= 1; draw()
    )
    next.addEventListener(
      "click",
      (_: dom.Event) =>
        current += 1; draw()
    )
    // Filters given in the address, as in ?from=23760&to=23760 from the chart.
    val params = dom.URLSearchParams(dom.window.location.search)
    List("edge" -> edge, "from" -> from, "to" -> to).foreach: (name, input) =>
      Option(params.get(name)).foreach(v => input.value = v)
    status.textContent = "Loading…"
    val _ = dom
      .fetch(table.getAttribute("data-file"))
      .`then`[String](_.text())
      .`then`[Unit]: text =>
        all = rows(text)
        draw()
