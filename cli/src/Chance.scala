package perfectcuboid

/** How many perfect cuboids to expect among the bricks found, if chance alone decided whether
  * a brick's space diagonal is whole: a heuristic, not a proof of anything.
  *
  * Near N, squares are about 2√N apart, so a number N taken at random is a square with a
  * chance of about 1 / (2√N). Remainders change that. Of the squares x² near N, the share
  * with x² ≡ r (mod m) is s(r) / m, where s(r) counts the x mod m with x² ≡ r; and the share of
  * all numbers near N with that remainder is 1 / m. So, knowing that N ≡ r, the chance becomes
  * s(r) / (2√N). It is 0 when no square leaves remainder r: then that brick cannot be a perfect
  * cuboid at all.
  */
object Chance:

  /** 2⁶ · 3² · 5 · 7 · 11 · 13, the prime powers that the rules for Euler bricks involve, and a few more. */
  val Modulus: Int = 64 * 9 * 5 * 7 * 11 * 13

  /** roots(r) = how many x mod `Modulus` have x² ≡ r. */
  private[perfectcuboid] lazy val roots: IArray[Int] =
    val counts = new Array[Int](Modulus)
    var x = 0L
    while x < Modulus do
      counts((x * x % Modulus).toInt) += 1
      x += 1
    IArray.unsafeFromArray(counts)

  private def spaceDiagonalSquare(brick: Cuboid): BigInt =
    BigInt(brick.a).pow(2) + BigInt(brick.b).pow(2) + BigInt(brick.c).pow(2)

  /** 1 / (2√N), N the square of the brick's space diagonal: the chance, knowing only its size. */
  def bySize(brick: Cuboid): Double = 1 / (2 * math.sqrt(spaceDiagonalSquare(brick).toDouble))

  /** The same, knowing its remainder mod `Modulus` too. */
  def byRemainder(brick: Cuboid): Double =
    roots((spaceDiagonalSquare(brick) % Modulus).toInt) * bySize(brick)

  /** One row of the table: the bricks with smallest edge in a range, and the perfect cuboids
    * to expect among them.
    */
  final case class Row(upTo: Long, bricks: Int, bySize: Double, byRemainder: Double, ruledOut: Int)

  /** A row for each power of ten, each counting the bricks above the row before. */
  def byDecade(bricks: Seq[Cuboid], searchedUpTo: Long): List[Row] =
    val limits = Iterator.iterate(10L)(_ * 10).takeWhile(_ < searchedUpTo).toList :+ searchedUpTo
    limits
      .zip(0L :: limits)
      .map: (upTo, above) =>
        val these = bricks.filter(b => b.a > above && b.a <= upTo)
        Row(upTo, these.size, these.map(bySize).sum, these.map(byRemainder).sum, these.count(byRemainder(_) == 0))
      .filter(_.bricks > 0)

  /** The table of expected perfect cuboids by decade, and its total, as lines to print. */
  def lines(bricks: Seq[Cuboid], searchedUpTo: Long): List[String] =
    val rows = byDecade(bricks.filter(_.isPrimitive), searchedUpTo)
    val total = rows.map(_.byRemainder).sum
    List(
      "Perfect cuboids to expect among the primitive bricks, if chance decided:",
      "| smallest edge up to | bricks | by size alone | by size and remainder | ruled out by remainder |",
      "| ---: | ---: | ---: | ---: | ---: |"
    ) ++ rows.map(r =>
      f"| ${r.upTo}%,d | ${r.bricks}%,d | ${r.bySize}%.2e | ${r.byRemainder}%.2e | ${r.ruledOut}%,d |"
    ) :+ f"In all: $total%.2e, so about one chance in ${if total > 0 then f"${1 / total}%,.0f" else "∞"}."
