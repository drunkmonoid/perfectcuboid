package perfectcuboid

/** The primitive Euler bricks of a hunt's file as OEIS b-files: A031173, A031174 and A031175,
  * the longest, middle and shortest edges of the primitive bricks in order of longest edge.
  *
  * A hunt of every smallest edge up to N holds every primitive brick with longest edge up to N,
  * since the smallest edge is no longer than the longest. So the tables can be carried on to N.
  * The published rows are kept exactly as they are, their order of ties included, once the
  * file is checked to hold exactly the same bricks over their range; the rows after them come
  * in order of longest edge, ties in order of middle edge. Writing the files is all this does:
  * offering them to the OEIS is for a person to decide.
  */
object OeisFiles:

  /** A row: the longest, middle and shortest edge. */
  type Row = (longest: Long, middle: Long, shortest: Long)

  def row(brick: Cuboid): Row = (longest = brick.c, middle = brick.b, shortest = brick.a)

  /** The published rows, then every other primitive brick with longest edge up to `upTo`; or why
    * not, if the bricks disagree with the published rows over the range those cover.
    */
  def rows(bricks: Seq[Cuboid], upTo: Long, published: List[Row]): Either[String, List[Row]] =
    val primitive = bricks.filter(_.isPrimitive).map(row)
    val covered = published.lastOption.fold(0L)(_.longest)
    val ours = primitive.filter(_.longest <= covered).toSet
    if published.toSet != ours then
      Left(
        s"the file and the published rows disagree up to longest edge $covered: " +
          s"${(published.toSet -- ours).size} published rows are not in the file, and ${(ours -- published.toSet).size} " +
          "bricks in the file are not published"
      )
    else if published.map(_.longest) != published.map(_.longest).sorted then Left("the published rows are out of order")
    else
      Right(
        published ++ primitive.filter(r => r.longest > covered && r.longest <= upTo).sortBy(r => (r.longest, r.middle))
      )

  /** One b-file: comment lines, then "n a(n)". */
  def bfile(number: String, what: String, values: List[Long], notes: List[String]): List[String] =
    (s"# A$number: $what, in order of longest edge, for n = 1..${values.size}." :: notes.map("# " + _)) ++
      values.zipWithIndex.map((v, i) => s"${i + 1} $v")

  /** The three files' names and lines. */
  def files(all: List[Row], publishedCount: Int, upTo: Long): List[(name: String, lines: List[String])] =
    val notes = List(
      s"Rows 1 to $publishedCount as published; rows ${publishedCount + 1} to ${all.size} from a search of every",
      f"primitive Euler brick with smallest edge up to $upTo%,d, checked again with exact arithmetic, which",
      "holds every brick with longest edge up to that. Later rows break ties by the middle edge.",
      "https://github.com/drunkmonoid/perfectcuboid"
    )
    List(
      (
        name = "b031173.txt",
        lines = bfile("031173", "longest edges of the primitive Euler bricks", all.map(_.longest), notes)
      ),
      (
        name = "b031174.txt",
        lines = bfile("031174", "middle edges of the primitive Euler bricks", all.map(_.middle), notes)
      ),
      (
        name = "b031175.txt",
        lines = bfile("031175", "shortest edges of the primitive Euler bricks", all.map(_.shortest), notes)
      )
    )

  /** The rows of the three published b-files, read from their lines. */
  def published(longest: List[String], middle: List[String], shortest: List[String]): List[Row] =
    def values(lines: List[String]) =
      lines.map(_.trim).filter(l => l.nonEmpty && !l.startsWith("#")).map(_.split("\\s+")(1).toLong)
    values(longest)
      .lazyZip(values(middle))
      .lazyZip(values(shortest))
      .toList
      .map((l, m, s) => (longest = l, middle = m, shortest = s))
