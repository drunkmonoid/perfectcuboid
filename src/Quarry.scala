package perfectcuboid

/** One kind of cuboid a hunt can look for: how to search a block of keys, and how to write
  * what it finds to a results file and read it back.
  *
  * A record of functions, one value per kind. A type class would not do here: Euler bricks
  * and face cuboids are both `Cuboid`s, and a type has only one instance of a type class.
  *
  * @param key
  *   what the search counts up through, as in "smallest edge"
  * @param search
  *   everything found for keys from `first` to `last`, in order, one at a time
  */
final case class Quarry[A](
    name: String,
    key: String,
    search: (Long, Long) => Iterator[A],
    line: A => String,
    parse: String => Option[A],
    summarize: A => Summary
)

object Quarry:

  private def cuboidLine(c: Cuboid): String = s"${c.a} ${c.b} ${c.c}" + (if c.isPerfect then "  PERFECT" else "")

  /** A line "44 117 240", possibly followed by "  PERFECT". */
  private def parseCuboid(line: String): Option[Cuboid] =
    line.split(" +").toList match
      case a :: b :: c :: _ =>
        for
          x <- a.toLongOption
          y <- b.toLongOption
          z <- c.toLongOption
          cuboid <- Cuboid.of(x, y, z)
        yield cuboid
      case _ => None

  val eulerBricks: Quarry[Cuboid] =
    Quarry("Euler bricks", "smallest edge", FastSearch.eulerBricksIterator, cuboidLine, parseCuboid, Summary.of)

  /** Face cuboids, and any perfect cuboid, by the edge their two whole face diagonals share. */
  val faceCuboids: Quarry[Cuboid] =
    Quarry(
      "face cuboids",
      "shared edge",
      (first, last) => (first to last).iterator.flatMap(NearMisses.faceCuboidsWithSharedEdge),
      cuboidLine,
      parseCuboid,
      Summary.of
    )

  /** An edge cuboid's lengths, the irrational one as √n: "√461776 1443 1800". */
  private def squaredLine(c: SquaredCuboid): String =
    List(c.a2, c.b2, c.c2).map(n => Arith.exactSqrt(n).fold(s"√$n")(_.toString)).mkString(" ")

  private def parseSquared(line: String): Option[SquaredCuboid] =
    def square(token: String): Option[Long] =
      if token.startsWith("√") then token.drop(1).toLongOption else token.toLongOption.map(n => n * n)
    line.split(" +").toList match
      case a :: b :: c :: _ =>
        for
          x <- square(a)
          y <- square(b)
          z <- square(c)
          cuboid <- SquaredCuboid.of(x, y, z)
        yield cuboid
      case _ => None

  /** An edge cuboid is primitive when no square k² > 1 divides all three of its squared edges. */
  private def summarizeSquared(c: SquaredCuboid): Summary =
    val g = Arith.gcd(Arith.gcd(c.a2, c.b2), c.c2)
    val primitive = NumberTheory.factorize(g).forall(_._2 == 1)
    Summary.one(primitive, c.kind == Kind.Perfect, Specimen(BigInt(c.c2), squaredLine(c).replace(" ", " × ")))

  /** Edge cuboids, by the square of their irrational edge. */
  val edgeCuboids: Quarry[SquaredCuboid] =
    Quarry(
      "edge cuboids",
      "squared irrational edge",
      (first, last) => (first to last).iterator.flatMap(NearMisses.edgeCuboidsWithSquaredEdge),
      squaredLine,
      parseSquared,
      summarizeSquared
    )

  /** By the names the command line uses. */
  val byName: Map[String, Quarry[?]] = Map("bricks" -> eulerBricks, "face" -> faceCuboids, "edge" -> edgeCuboids)
