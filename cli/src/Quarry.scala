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

  /** "44 117 240", followed by "  PERFECT" for a perfect cuboid. */
  private def cuboidLine(c: Cuboid): String =
    LineFormat.write(EdgesLine(c.a, c.b, c.c)) + (if c.isPerfect then "  PERFECT" else "")

  private def parseCuboid(line: String): Option[Cuboid] =
    LineFormat.parse[EdgesLine](line).flatMap(edges => Cuboid.of(edges.a, edges.b, edges.c))

  /** Euler bricks, by smallest edge, found by `FastSearch` and checked again by `BigCuboid`. */
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

  /** The primitive Euler bricks by their odd edge (`OddSearch`): the region of bricks whose
    * odd edge is at most the limit, wherever their smallest edge is.
    */
  val oddEdge: Quarry[Cuboid] =
    primitiveBricks.copy(
      name = "primitive Euler bricks by odd edge",
      key = "odd edge",
      search = OddSearch.primitiveBricksIterator,
      keyOf = OddSearch.oddEdge,
      check = cuboid => primitiveBricks.check(cuboid) && OddSearch.oddEdge(cuboid) != 0
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

  private def spaceDiagonalOf(c: Cuboid): Long = c.spaceDiagonal.fold(0L)(d => d: Long)

  /** Face cuboids, and so any perfect cuboid, by their space diagonal d (phase 83): d² split as
    * p² + y², then p² as s² + x², as `NearMisses.faceCuboidsWithSpaceDiagonal` does. A perfect
    * cuboid with space diagonal up to D has smallest edge below D / √3, so the brick hunt to N
    * already covers every space diagonal up to √3·N; this covers them from the other end.
    */
  val faceByDiagonal: Quarry[Cuboid] =
    Quarry(
      "face cuboids by space diagonal",
      "space diagonal",
      (first, last) => (first to last).iterator.flatMap(NearMisses.faceCuboidsWithSpaceDiagonal),
      cuboidLine,
      parseCuboid,
      Summary.of,
      spaceDiagonalOf,
      cuboid => Set(Kind.Face, Kind.Perfect)(BigCuboid.from(cuboid).kind),
      SquaredCuboid.MaxEdge
    )

  private def parallelepipedLine(s: Parallelepiped): String =
    LineFormat.write(ParallelepipedLine(s.a, s.b, s.c, s.p, s.q, s.r))

  private def parseParallelepiped(line: String): Option[Parallelepiped] =
    LineFormat.parse[ParallelepipedLine](line).map(l => Parallelepiped(l.a, l.b, l.c, l.p, l.q, l.r))

  /** A perfect parallelepiped checked again with BigInt, without `Parallelepiped`'s code: edges in
    * order, solid (twice its Gram matrix positive definite), and all 13 lengths whole.
    */
  private def checkParallelepiped(s: Parallelepiped): Boolean =
    val List(a, b, c, p, q, r) = List(s.a, s.b, s.c, s.p, s.q, s.r).map(BigInt(_)): @unchecked
    val edges = a * a + b * b + c * c
    val squares = List(
      a * a + b * b + p,
      a * a + b * b - p,
      a * a + c * c + q,
      a * a + c * c - q,
      b * b + c * c + r,
      b * b + c * c - r,
      edges + p + q + r,
      edges + p - q - r,
      edges - p + q - r,
      edges - p - q + r
    )
    val (x, y, z) = (2 * a * a, 2 * b * b, 2 * c * c)
    val det = x * (y * z - r * r) - p * (p * z - r * q) + q * (p * r - y * q)
    a >= b && b >= c && c >= 1 && x * y - p * p > 0 && det > 0 &&
    squares.forall(n => n >= 0 && Arith.isPerfectSquareBig(n))

  /** Perfect parallelepipeds by their longest edge (phase 84): each shape once, as
    * `Parallelepiped.search` finds them. Primitive means not a scaled copy of a smaller one; a
    * perfect cuboid, the right-angled case, would count as perfect.
    */
  val parallelepipeds: Quarry[Parallelepiped] =
    Quarry(
      "perfect parallelepipeds",
      "longest edge",
      (first, last) => Parallelepiped.searchBetween(first, last).iterator,
      parallelepipedLine,
      parseParallelepiped,
      s => Summary.one(s.scaledFrom.isEmpty, s.isCuboid, Specimen(BigInt(s.a) * s.a, parallelepipedLine(s))),
      _.a,
      checkParallelepiped,
      1_000_000
    )

  private def squaredLine(c: SquaredCuboid): String =
    LineFormat.write(SquaredLine(Length.of(c.a2), Length.of(c.b2), Length.of(c.c2)))

  private def parseSquared(line: String): Option[SquaredCuboid] =
    LineFormat
      .parse[SquaredLine](line)
      .flatMap(edges => SquaredCuboid.of(edges.a.square, edges.b.square, edges.c.square))

  /** An edge cuboid is primitive when no square k² > 1 divides all three of its squared edges. */
  private def summarizeSquared(c: SquaredCuboid): Summary =
    Summary.one(c.isPrimitive, c.kind == Kind.Perfect, Specimen(BigInt(c.c2), squaredLine(c).replace(" ", " × ")))

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
  /** The name `--kind` takes for this kind, which a file's header records. */
  def nameOf(quarry: Quarry[?]): String = byName.collectFirst { case (name, q) if q eq quarry => name }.get

  val byName: Map[String, Quarry[?]] =
    Map(
      "bricks" -> eulerBricks,
      "primitive" -> primitiveBricks,
      "odd" -> oddEdge,
      "diagonal" -> faceByDiagonal,
      "parallelepiped" -> parallelepipeds,
      "face" -> faceCuboids,
      "edge" -> edgeCuboids
    )
