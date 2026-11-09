package perfectcuboid

/** Records among the bricks found, and how the number of bricks on an edge follows from the
  * number of legs it has. All pure.
  */
object Records:

  private def show(brick: Cuboid) = s"${brick.a} × ${brick.b} × ${brick.c}"

  /** The edges that the most of these bricks share, as any of their three edges, most first. */
  def sharedEdges(bricks: Seq[Cuboid], count: Int): List[(Long, Seq[Cuboid])] =
    bricks
      .flatMap(brick => List(brick.a: Long, brick.b, brick.c).distinct.map(_ -> brick))
      .groupMap(_._1)(_._2)
      .toList
      .sortBy((edge, shared) => (-shared.size, edge))
      .take(count)

  /** Longest edge over smallest edge: large for flat bricks, small for bricks like a cube. */
  def flatness(brick: Cuboid): Double = brick.c.toDouble / brick.a

  /** The records among these bricks, as lines to print. */
  def lines(bricks: Seq[Cuboid], which: String): List[String] =
    val primitive = bricks.filter(_.isPrimitive)
    List(s"Records among the ${primitive.size} primitive Euler bricks $which:", "Edges shared by the most bricks:") ++
      sharedEdges(primitive, 5).map((edge, shared) =>
        f"  $edge%,d: ${shared.size} bricks, ${shared.map(show).mkString(", ")}"
      ) ++
      List(
        s"Longest: ${show(primitive.maxBy(_.c: Long))}",
        f"Flattest, longest edge over smallest: ${show(primitive.maxBy(flatness))} (${flatness(primitive.maxBy(flatness))}%,.0f)",
        f"Most like a cube: ${show(primitive.minBy(flatness))} (${flatness(primitive.minBy(flatness))}%.3f)"
      )

  /** For the smallest edges a with the same number of legs above them: how many there are, and
    * on average how many pairs of legs and how many bricks each has.
    */
  final case class LegsRow(legs: Range, edges: Int, pairs: Double, bricks: Double)

  /** Every brick with smallest edge from 1 to `limit` must be among `bricks`. */
  def byLegs(bricks: Seq[Cuboid], limit: Long): List[LegsRow] =
    val bricksOn = bricks.groupMapReduce(_.a: Long)(_ => 1)(_ + _)
    val legs = (1L to limit).map(a => a -> FastSearch.legsAbove(a).length)
    val bands =
      List(0 until 2, 2 until 4, 4 until 8, 8 until 16, 16 until 32, 32 until 64, 64 until 128, 128 until Int.MaxValue)
    bands.flatMap: band =>
      val edges = legs.filter((_, n) => band.contains(n))
      Option.when(edges.nonEmpty):
        val pairs = edges.map((_, n) => n.toLong * (n - 1) / 2).sum
        val found = edges.map((a, _) => bricksOn.getOrElse(a, 0)).sum
        LegsRow(band, edges.size, pairs.toDouble / edges.size, found.toDouble / edges.size)

  /** The table of bricks against legs, and the edge with the most bricks, as lines to print. */
  def legsLines(bricks: Seq[Cuboid], limit: Long): List[String] =
    val rows = byLegs(bricks, limit)
    val bricksOn = bricks.groupMapReduce(_.a: Long)(_ => 1)(_ + _)
    val (top, count) = bricksOn.maxBy((a, n) => (n, -a))
    List(
      f"Smallest edges up to $limit%,d, by how many legs pair with them above themselves:",
      "| legs | edges | pairs of legs, on average | bricks, on average | bricks per 1,000 pairs |",
      "| --- | ---: | ---: | ---: | ---: |"
    ) ++ rows.map: r =>
      val legs = if r.legs.end == Int.MaxValue then s"${r.legs.start}+" else s"${r.legs.start}–${r.legs.end - 1}"
      val perPairs = if r.pairs > 0 then f"${1000 * r.bricks / r.pairs}%.2f" else "–"
      f"| $legs | ${r.edges}%,d | ${r.pairs}%,.1f | ${r.bricks}%.3f | $perPairs |"
    :+ f"The most bricks on one smallest edge: $top%,d, with $count, and ${FastSearch.legsAbove(top).length} legs above it."
