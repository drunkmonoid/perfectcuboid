package perfectcuboid

import cats.kernel.{CommutativeMonoid, Eq}
import cats.syntax.all.*

/** What a search found, boiled down to a few numbers.
  *
  * Summaries form a monoid: `Summary.empty` is the summary of finding nothing, and `x |+| y`
  * is the summary of finding both x's bricks and y's. It is commutative too, so the order of
  * combining doesn't matter either. That is what lets each block of a search be summarised on
  * its own, on any thread, and the pieces be added up afterwards in any grouping.
  *
  * @param largest
  *   the largest brick, in the order of `Ordering[Cuboid]` (by largest edge first)
  * @param byDigits
  *   how many bricks have a largest edge with 1, 2, 3, … digits
  */
final case class Summary(
    bricks: Long,
    primitive: Long,
    perfect: Long,
    largest: Option[Cuboid],
    byDigits: Map[Int, Long]
):

  def describe: List[String] =
    s"$bricks Euler bricks, $primitive primitive, $perfect perfect" ::
      largest.map(c => s"largest: ${c.a} × ${c.b} × ${c.c}").toList :::
      Option
        .when(byDigits.nonEmpty)(
          byDigits.toList.sorted
            .map((digits, n) => s"$digits: $n")
            .mkString("by digits in the largest edge: ", ", ", "")
        )
        .toList

object Summary:

  val empty: Summary = Summary(0, 0, 0, None, Map.empty)

  def of(brick: Cuboid): Summary =
    def one(condition: Boolean) = if condition then 1L else 0L
    Summary(1, one(brick.isPrimitive), one(brick.isPerfect), Some(brick), Map(brick.c.toString.length -> 1L))

  def of(bricks: IterableOnce[Cuboid]): Summary =
    bricks.iterator.foldLeft(empty)((summary, brick) => summary |+| of(brick))

  /** Each field is combined by an operation that is itself associative and commutative:
    * adding counts, taking the larger brick, and adding maps key by key (cats' `|+|` on a
    * `Map` adds the values of keys that appear in both).
    */
  given CommutativeMonoid[Summary] with
    def empty: Summary = Summary.empty
    def combine(x: Summary, y: Summary): Summary =
      Summary(
        x.bricks + y.bricks,
        x.primitive + y.primitive,
        x.perfect + y.perfect,
        (x.largest ++ y.largest).maxOption,
        x.byDigits |+| y.byDigits
      )

  /** Cats compares with `Eq` rather than `==`; for a case class they mean the same. */
  given Eq[Summary] = Eq.fromUniversalEquals
