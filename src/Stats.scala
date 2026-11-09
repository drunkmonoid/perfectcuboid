package perfectcuboid

import cats.kernel.{CommutativeMonoid, Eq}
import cats.syntax.all.*

/** Counting what the searches find, for the write-up. All pure. */
object Stats:

  /** Euler brick counts below some limit N: by smallest edge <= N and by largest edge <= N,
    * all and primitive.
    */
  final case class Counts(bySmallest: Long, bySmallestPrimitive: Long, byLargest: Long, byLargestPrimitive: Long)

  /** Making and adding counts. */
  object Counts:

    val empty: Counts = Counts(0, 0, 0, 0)

    /** How one brick counts towards the totals for `limit`. */
    def of(brick: Cuboid, limit: Long): Counts =
      def one(condition: Boolean) = if condition then 1L else 0L
      val smallest = brick.a <= limit
      val largest = brick.c <= limit
      Counts(
        one(smallest),
        one(smallest && brick.isPrimitive),
        one(largest),
        one(largest && brick.isPrimitive)
      )

    /** Counts add up field by field, which is how cats already adds tuples of four Longs. So
      * the monoid for Counts is that one, carried over by `imap`: turn Counts into a tuple, add
      * the tuples, and turn the result back.
      */
    given CommutativeMonoid[Counts] =
      CommutativeMonoid[(Long, Long, Long, Long)].imap(Counts.apply.tupled)(Tuple.fromProductTyped)

    given Eq[Counts] = Eq.fromUniversalEquals

  /** The counts for one limit. */
  final case class Row(limit: Long, counts: Counts)

  /** Counts for every limit 10³, 10⁴, …, 10^maxExponent, in one pass over the bricks,
    * which must come in order of smallest edge.
    */
  def growth(maxExponent: Int, bricks: LazyList[Cuboid]): List[Row] =
    val limits = (3 to maxExponent).map(e => BigInt(10).pow(e).toLong).toList
    // A brick with largest edge <= N also has smallest edge <= N, so this reaches every brick counted.
    val totals = bricks
      .takeWhile(_.a <= limits.last)
      .foldLeft(limits.map(_ => Counts.empty)): (counts, brick) =>
        counts.lazyZip(limits).map((sum, limit) => sum |+| Counts.of(brick, limit))
    limits.lazyZip(totals).map(Row(_, _))

  /** How fast a count grows: the k in count ≈ C·Nᵏ, measured between two limits. */
  def exponent(n1: Long, count1: Long, n2: Long, count2: Long): Double =
    math.log(count2.toDouble / count1) / math.log(n2.toDouble / n1)

  /** The share of primitive Euler bricks with every edge <= limit that Saunderson's formula gives. */
  def saundersonShare(limit: Long, bricks: LazyList[Cuboid]): (Long, Long) =
    val primitive = bricks.takeWhile(_.a <= limit).filter(b => b.c <= limit && b.isPrimitive).toSet
    val fromFormula = Families.saundersonBricksUpTo(limit).filter(_.isPrimitive).toSet
    ((fromFormula intersect primitive).size.toLong, primitive.size.toLong)
