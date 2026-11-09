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
  * @param keyOf
  *   the key under which the search finds a find
  * @param check
  *   whether a find really is of this kind, worked out again with BigInt, independently of
  *   the search, for `verify`
  * @param maxKey
  *   how far the search can go
  */
final case class Quarry[A](
    name: String,
    key: String,
    search: (Long, Long) => Iterator[A],
    line: A => String,
    parse: String => Option[A],
    summarize: A => Summary,
    keyOf: A => Long,
    check: A => Boolean,
    maxKey: Long = Search.MaxSmallestEdge
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
    Quarry(
      "Euler bricks",
      "smallest edge",
      FastSearch.eulerBricksIterator,
      cuboidLine,
      parseCuboid,
      Summary.of,
      _.a,
      cuboid => BigCuboid.from(cuboid).isEuler,
      FastSearch.MaxSmallestEdge
    )

  /** The primitive Euler bricks only: the same search, but only primitive bricks are kept.
    *
    * For the perfect cuboid question these are enough. Divide a perfect cuboid's edges by
    * their greatest common divisor and the result is a perfect cuboid too, primitive, with a
    * smaller smallest edge. So if no primitive brick up to some smallest edge is perfect, no
    * brick at all is. And there are far fewer of them: about 1 in 300 bricks up to 10⁷.
    */
  val primitiveBricks: Quarry[Cuboid] =
    eulerBricks.copy(
      name = "primitive Euler bricks",
      search = (first, last) => FastSearch.eulerBricksIterator(first, last).filter(_.isPrimitive),
      check = cuboid => eulerBricks.check(cuboid) && cuboid.isPrimitive
    )

  /** The edge of a face cuboid where its two whole face diagonals meet. */
  private def sharedEdge(c: Cuboid): Long =
    val List(ab, ac, bc) = BigCuboid.from(c).faceDiagonals.map(_.isDefined): @unchecked
    if ab && ac then c.a else if ab && bc then c.b else c.c

  /** Face cuboids, and any perfect cuboid, by the edge their two whole face diagonals share. */
  val faceCuboids: Quarry[Cuboid] =
    Quarry(
      "face cuboids",
      "shared edge",
      (first, last) => (first to last).iterator.flatMap(NearMisses.faceCuboidsWithSharedEdge),
      cuboidLine,
      parseCuboid,
      Summary.of,
      sharedEdge,
      cuboid => Set(Kind.Face, Kind.Perfect)(BigCuboid.from(cuboid).kind)
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

  /** One irrational edge, and the other six lengths whole, worked out with BigInt. */
  private def checkSquared(c: SquaredCuboid): Boolean =
    val squares = List(c.a2, c.b2, c.c2).map(BigInt(_))
    val List(a, b, d) = squares: @unchecked
    def whole(n: BigInt) = Arith.isPerfectSquareBig(n)
    squares.count(!whole(_)) == 1 && List(a + b, a + d, b + d, a + b + d).forall(whole)

  /** Edge cuboids, by the square of their irrational edge. */
  val edgeCuboids: Quarry[SquaredCuboid] =
    Quarry(
      "edge cuboids",
      "squared irrational edge",
      (first, last) => (first to last).iterator.flatMap(NearMisses.edgeCuboidsWithSquaredEdge),
      squaredLine,
      parseSquared,
      summarizeSquared,
      c => List(c.a2, c.b2, c.c2).find(n => Arith.exactSqrt(n).isEmpty).getOrElse(0L),
      checkSquared
    )

  /** By the names the command line uses. */
  val byName: Map[String, Quarry[?]] =
    Map("bricks" -> eulerBricks, "primitive" -> primitiveBricks, "face" -> faceCuboids, "edge" -> edgeCuboids)
