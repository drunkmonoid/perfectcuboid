package perfectcuboid

/** The primitive bricks found, joined into a graph by the two ways one brick makes another.
  *
  * A brick a × b × c and its derived brick ab × ac × bc (`Derived`) are joined, and so are a
  * brick and the bricks at 2P to 4P on its three elliptic curves (`Origins.curveMultiples`),
  * whenever both ends are among the bricks found. The pieces the graph falls into are the
  * families that these maps make: how many there are, how big, and how far the largest reaches.
  */
object BrickGraph:

  /** How two bricks are joined. */
  enum Join:
    case Derivation, Curve

  /** A join between two bricks, the smaller first. */
  final case class Link(small: BigCuboid, large: BigCuboid, join: Join)

  /** Every join between two of these bricks. */
  def links(bricks: Vector[BigCuboid], maxMultiple: Int = 4): Vector[Link] =
    val found = bricks.toSet
    def link(x: BigCuboid, y: BigCuboid, join: Join): Option[Link] =
      Option.when(x != y && found(y)):
        if Ordering[BigCuboid].lt(x, y) then Link(x, y, join) else Link(y, x, join)
    val derivations = bricks.flatMap(b => link(b, Derived.derived(b), Join.Derivation))
    val curves = bricks.flatMap(b => Origins.curveMultiples(b, maxMultiple).flatMap(link(b, _, Join.Curve)))
    (derivations ++ curves).distinct

  /** The pieces of the graph, each a set of bricks joined to each other, largest first. */
  def components(bricks: Vector[BigCuboid], links: Vector[Link]): Vector[Vector[BigCuboid]] =
    val index = bricks.zipWithIndex.toMap
    val parent = Array.tabulate(bricks.size)(identity)
    def root(i: Int): Int =
      var r = i
      while parent(r) != r do r = parent(r)
      var j = i
      while parent(j) != r do
        val next = parent(j)
        parent(j) = r
        j = next
      r
    links.foreach: l =>
      val (x, y) = (root(index(l.small)), root(index(l.large)))
      if x != y then parent(math.max(x, y)) = math.min(x, y)
    bricks.indices
      .groupBy(root)
      .values
      .map(_.map(bricks).toVector.sorted)
      .toVector
      .sortBy(c => (-c.size, c.head))

  private def show(b: BigCuboid) = s"${b.a} × ${b.b} × ${b.c}"

  /** The graph of these bricks: its joins and its pieces, worked out once. */
  final case class Graph(bricks: Vector[BigCuboid], links: Vector[Link], pieces: Vector[Vector[BigCuboid]])

  def of(bricks: Vector[BigCuboid]): Graph =
    val ls = links(bricks)
    Graph(bricks, ls, components(bricks, ls))

  /** The graph in numbers, and its largest pieces, as lines to print. */
  def lines(graph: Graph, which: String): List[String] =
    val Graph(bricks, ls, pieces) = graph
    val sizes = pieces.groupBy(_.size).view.mapValues(_.size).toList.sortBy(_._1)
    val joined = pieces.filter(_.size > 1)
    List(
      s"The ${bricks.size} primitive Euler bricks $which, joined by derivation and by 2P–4P on their curves:",
      s"  ${ls.count(_.join == Join.Derivation)} derivations and ${ls.count(_.join == Join.Curve)} curve multiples join them",
      s"  into ${pieces.size} pieces: ${pieces.count(_.size == 1)} bricks alone, " +
        s"and ${joined.map(_.size).sum} in ${joined.size} pieces of two or more",
      "| bricks in a piece | pieces |",
      "| ---: | ---: |"
    ) ++ sizes.map((size, count) => f"| $size | $count%,d |") ++
      joined
        .take(3)
        .zipWithIndex
        .flatMap: (piece, k) =>
          s"Piece ${k + 1}, ${piece.size} bricks:" :: piece.map(b => s"  ${show(b)}").toList

  /** Pieces by size: how many pieces of each size, as an SVG chart. */
  def chart(graph: Graph): String =
    val sizes = graph.pieces.groupBy(_.size).view.mapValues(_.size).toList.sortBy(_._1)
    Charts.scatter(
      "Families of primitive bricks, joined by derivation and curves",
      "bricks in the piece",
      "pieces of that size",
      sizes.map((size, count) =>
        (
          size.toDouble,
          count.toDouble,
          s"$count piece${if count == 1 then "" else "s"} of $size brick${if size == 1 then "" else "s"}"
        )
      )
    )
