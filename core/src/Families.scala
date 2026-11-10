package perfectcuboid

/** A Pythagorean triple: a² + b² = c². */
final case class Triple(a: Long, b: Long, c: Long)

/** Infinite families of triples and Euler bricks, built from formulas instead of searches. */
object Families:

  /** The three children of a triple in the Berggren tree. */
  def children(t: Triple): List[Triple] =
    val Triple(a, b, c) = t
    List(
      Triple(a - 2 * b + 2 * c, 2 * a - b + 2 * c, 2 * a - 2 * b + 3 * c),
      Triple(a + 2 * b + 2 * c, 2 * a + b + 2 * c, 2 * a + 2 * b + 3 * c),
      Triple(-a + 2 * b + 2 * c, -2 * a + b + 2 * c, -2 * a + 2 * b + 3 * c)
    )

  /** The levels of the Berggren tree: (3, 4, 5), then its 3 children, then their 9, and so on.
    *
    * Every primitive triple appears exactly once, with its odd leg first. `LazyList.iterate`
    * describes the whole infinite sequence, but only computes a level when it is asked for.
    *
    * This is a `def`, not a `val`: a LazyList remembers every element it has computed, so a
    * `val` holding the start of an infinite list would keep all of it in memory forever.
    */
  def berggrenLevels: LazyList[List[Triple]] =
    LazyList.iterate(List(Triple(3, 4, 5)))(_.flatMap(children))

  /** All primitive triples, level by level. */
  def primitiveTriples: LazyList[Triple] = berggrenLevels.flatten

  /** The primitive triples with hypotenuse <= max.
    *
    * A child always has a bigger hypotenuse than its parent, so a branch can be cut as soon
    * as it passes the bound. Cutting whole levels instead would not work: the branch
    * (3, 4, 5), (5, 12, 13), (7, 24, 25), … grows so slowly that some level always survives,
    * while level n holds 3ⁿ triples.
    */
  def primitiveTriplesUpTo(max: Long): List[Triple] =
    def small(ts: List[Triple]) = ts.filter(_.c <= max)
    LazyList
      .iterate(small(List(Triple(3, 4, 5))))(level => small(level.flatMap(children)))
      .takeWhile(_.nonEmpty)
      .toList
      .flatten

  /** Saunderson's Euler brick (1740) built from a Pythagorean triple (u, v, w):
    * edges u·|4v² − w²|, v·|4u² − w²| and 4uvw. The edges grow like the cube of the triple,
    * so they are BigInts.
    */
  def saundersonBig(t: Triple): BigCuboid =
    val (u, v, w) = (BigInt(t.a), BigInt(t.b), BigInt(t.c))
    BigCuboid.of(u * (4 * v * v - w * w).abs, v * (4 * u * u - w * w).abs, 4 * u * v * w).get

  /** Saunderson's brick as a `Cuboid`, or `None` when it is too large for one. */
  def saunderson(t: Triple): Option[Cuboid] = saundersonBig(t).toCuboid.toOption

  /** Saunderson's bricks with every edge <= max, sorted.
    *
    * The edge 4uvw is at least 4·3·4·w = 48w, so only triples with w <= max / 48 can give one.
    */
  def saundersonBricksUpTo(max: Long): List[Cuboid] =
    primitiveTriplesUpTo(max / 48).flatMap(saunderson).filter(_.c <= max).sorted
