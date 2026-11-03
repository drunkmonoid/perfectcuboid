package perfectcuboid

import cats.kernel.{CommutativeMonoid, Eq}
import cats.syntax.all.*

/** One find, as a summary needs it: the square of its longest edge, to tell which is largest
  * (squared, because an edge may be irrational), and how to show it.
  */
final case class Specimen(longestSquared: BigInt, text: String)

object Specimen:
  given Ordering[Specimen] = Ordering.by(s => (s.longestSquared, s.text))

/** What a search found, boiled down to a few numbers.
  *
  * Summaries form a monoid: `Summary.empty` is the summary of finding nothing, and `x |+| y`
  * is the summary of finding both x's finds and y's. It is commutative too, so the order of
  * combining doesn't matter either. That is what lets each block of a search be summarised on
  * its own, on any thread, and the pieces be added up afterwards in any grouping.
  *
  * @param largest
  *   the find with the longest edge
  * @param byDigits
  *   how many finds have a longest edge with 1, 2, 3, … digits (before the decimal point)
  */
final case class Summary(
    found: Long,
    primitive: Long,
    perfect: Long,
    largest: Option[Specimen],
    byDigits: Map[Int, Long]
):

  /** Lines for a report; `what` names the finds, as in "Euler bricks". */
  def describe(what: String): List[String] =
    s"$found $what, $primitive primitive, $perfect perfect" ::
      largest.map(specimen => s"largest: ${specimen.text}").toList :::
      Option
        .when(byDigits.nonEmpty)(
          byDigits.toList.sorted
            .map((digits, n) => s"$digits: $n")
            .mkString("by digits in the longest edge: ", ", ", "")
        )
        .toList

object Summary:

  val empty: Summary = Summary(0, 0, 0, None, Map.empty)

  /** The summary of a single find. */
  def one(primitive: Boolean, perfect: Boolean, specimen: Specimen): Summary =
    def count(condition: Boolean) = if condition then 1L else 0L
    val digits = Arith.isqrtBig(specimen.longestSquared).toString.length
    Summary(1, count(primitive), count(perfect), Some(specimen), Map(digits -> 1L))

  def of(cuboid: Cuboid): Summary =
    one(
      cuboid.isPrimitive,
      cuboid.isPerfect,
      Specimen(BigInt(cuboid.c).pow(2), s"${cuboid.a} × ${cuboid.b} × ${cuboid.c}")
    )

  def of(cuboids: IterableOnce[Cuboid]): Summary =
    cuboids.iterator.foldLeft(empty)((summary, cuboid) => summary |+| of(cuboid))

  /** Each field is combined by an operation that is itself associative and commutative:
    * adding counts, taking the larger find, and adding maps key by key (cats' `|+|` on a
    * `Map` adds the values of keys that appear in both).
    */
  given CommutativeMonoid[Summary] with
    def empty: Summary = Summary.empty
    def combine(x: Summary, y: Summary): Summary =
      Summary(
        x.found + y.found,
        x.primitive + y.primitive,
        x.perfect + y.perfect,
        (x.largest ++ y.largest).maxOption,
        x.byDigits |+| y.byDigits
      )

  /** Cats compares with `Eq` rather than `==`; for a case class they mean the same. */
  given Eq[Summary] = Eq.fromUniversalEquals
