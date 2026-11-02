package perfectcuboid

/** Counting what the searches find, for the write-up. All pure. */
object Stats:

  /** Euler brick counts up to `limit`, by smallest edge and by largest edge, all and primitive. */
  final case class Counts(limit: Long, bySmallest: Long, bySmallestPrimitive: Long, byLargest: Long, byLargestPrimitive: Long):

    def add(brick: Cuboid): Counts =
      def one(condition: Boolean) = if condition then 1L else 0L
      val smallest = brick.a <= limit
      val largest = brick.c <= limit
      Counts(
        limit,
        bySmallest + one(smallest),
        bySmallestPrimitive + one(smallest && brick.isPrimitive),
        byLargest + one(largest),
        byLargestPrimitive + one(largest && brick.isPrimitive)
      )

  /** Counts for every limit 10³, 10⁴, …, 10^maxExponent, in one pass over the bricks,
    * which must come in order of smallest edge.
    */
  def growth(maxExponent: Int, bricks: LazyList[Cuboid]): List[Counts] =
    val limits = (3 to maxExponent).map(e => BigInt(10).pow(e).toLong).toList
    val empty = limits.map(Counts(_, 0, 0, 0, 0))
    // A brick with largest edge <= N also has smallest edge <= N, so this reaches every brick counted.
    bricks.takeWhile(_.a <= limits.last).foldLeft(empty)((counts, brick) => counts.map(_.add(brick)))

  /** How fast a count grows: the k in count ≈ C·Nᵏ, measured between two limits. */
  def exponent(n1: Long, count1: Long, n2: Long, count2: Long): Double =
    math.log(count2.toDouble / count1) / math.log(n2.toDouble / n1)

  /** The share of primitive Euler bricks with every edge <= limit that Saunderson's formula gives. */
  def saundersonShare(limit: Long, bricks: LazyList[Cuboid]): (Long, Long) =
    val primitive = bricks.takeWhile(_.a <= limit).filter(b => b.c <= limit && b.isPrimitive).toSet
    val fromFormula = Families.saundersonBricksUpTo(limit).filter(_.isPrimitive).toSet
    ((fromFormula intersect primitive).size.toLong, primitive.size.toLong)
