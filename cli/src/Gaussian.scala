package perfectcuboid

/** Perfect cuboids over the Gaussian integers, the numbers re + im·i with whole re and im:
  * edges a, b, c among them with a² + b², a² + c², b² + c² and a² + b² + c² all squares of
  * Gaussian integers, none of them 0. Over these, unlike the whole numbers, perfect boxes are
  * found at once.
  */
object Gaussian:

  /** re + im·i. */
  final case class G(re: Long, im: Long):
    def +(that: G): G = G(re + that.re, im + that.im)
    def *(that: G): G = G(re * that.re - im * that.im, re * that.im + im * that.re)
    def squared: G = this * this
    def norm: Long = re * re + im * im
    def isZero: Boolean = re == 0 && im == 0
    def conjugate: G = G(re, -im)
    def timesI: G = G(-im, re)
    def negate: G = G(-re, -im)
    override def toString: String =
      if im == 0 then s"$re" else if re == 0 then s"${im}i" else s"$re${if im > 0 then "+" else "-"}${math.abs(im)}i"

  /** The square root of z, if z is the square of a Gaussian integer: w = x + y·i with
    * x² − y² = re and 2xy = im, so x² = (|z| + re) / 2 and y² = (|z| − re) / 2, |z| = √norm.
    */
  def sqrt(z: G): Option[G] =
    Arith
      .exactSqrt(z.norm)
      .flatMap: size =>
        if (size + z.re) % 2 != 0 then None
        else
          for
            x <- Arith.exactSqrt((size + z.re) / 2)
            y <- Arith.exactSqrt((size - z.re) / 2)
            w = if z.im < 0 then G(x, -y) else G(x, y)
            if w.squared == z
          yield w

  def isSquare(z: G): Boolean = sqrt(z).isDefined

  /** One of each pair ±a: re > 0, or re = 0 and im > 0. a and −a have the same square. */
  def halfPlane(n: Int): Vector[G] =
    (for re <- 0 to n; im <- -n to n if re > 0 || im > 0 yield G(re, im)).toVector

  /** A box, its edges up to sign and order, and the same box multiplied by i or conjugated,
    * which is perfect exactly when it is: the smallest of the variants, as one name for all.
    */
  def canonical(edges: List[G]): List[G] =
    def sign(g: G) = if g.re > 0 || (g.re == 0 && g.im > 0) then g else g.negate
    def order(gs: List[G]) = gs.map(sign).sortBy(g => (g.norm, g.re, g.im))
    val variants = List(edges, edges.map(_.timesI), edges.map(_.conjugate), edges.map(_.conjugate.timesI)).map(order)
    variants.minBy(_.map(g => (g.norm, g.re, g.im)).toString)

  /** Whether all three edges are whole multiples of one Gaussian number, as 2 + i, 4 + 2i, 6 + 3i:
    * such a box is a box of whole numbers turned and stretched, not a new one.
    */
  def isTurnedWhole(edges: List[G]): Boolean =
    val List(a, b, c) = edges: @unchecked
    // a, b, c proportional over the rationals: every pair's "cross product" is 0.
    def parallel(x: G, y: G) = x.re * y.im == x.im * y.re
    parallel(a, b) && parallel(a, c)

  /** The perfect Gaussian cuboids with every edge's parts at most n in size, one of each: with
    * all seven lengths nonzero, or, if `zeroAllowed`, with a diagonal 0 as well.
    */
  def perfect(n: Int, zeroAllowed: Boolean = false): List[List[G]] =
    val values = halfPlane(n)
    val squares = values.map(_.squared)
    def counts(s: G) = (zeroAllowed || !s.isZero) && isSquare(s)
    // Joined when a² + b² is a square: the face diagonal.
    val partners = values.indices.map(i => (i + 1 until values.size).filter(j => counts(squares(i) + squares(j))).toSet)
    val found =
      for
        i <- values.indices
        j <- partners(i)
        l <- partners(i) intersect partners(j)
        if l > j
        if counts(squares(i) + squares(j) + squares(l))
      yield canonical(List(values(i), values(j), values(l)))
    found.distinct.toList.sortBy(edges => (edges.map(_.norm).max, edges.map(_.norm).sum))

  /** The four diagonals of a box, written out. */
  def diagonals(edges: List[G]): String =
    val List(a, b, c) = edges.map(_.squared): @unchecked
    List(a + b, a + c, b + c, a + b + c).map(s => sqrt(s).fold("?")(_.toString)).mkString(", ")

  /** The boxes found, with their diagonals, as lines to print. */
  def lines(n: Int): List[String] =
    val boxes = perfect(n)
    val degenerate = perfect(n, zeroAllowed = true).filterNot(boxes.toSet)
    def show(edges: List[G]) = f"  ${edges.mkString(" × ")}%-36s diagonals ${diagonals(edges)}"
    List(
      s"Perfect cuboids with Gaussian edges, real and imaginary parts up to $n, all seven lengths nonzero:",
      s"  ${boxes.size}, counted once up to signs, order, multiplying by i and conjugating"
    ) ++ boxes.take(25).map(show) ++
      (s"With a diagonal 0 allowed: ${degenerate.size} more, ${degenerate.count(spaceZero)} of them with space diagonal 0" +
        s" and ${degenerate.count(faceZero)} with a face diagonal 0; the smallest:" :: degenerate.take(8).map(show))

  private def spaceZero(edges: List[G]) = edges.map(_.squared).reduce(_ + _).isZero
  private def faceZero(edges: List[G]) =
    val List(a, b, c) = edges.map(_.squared): @unchecked
    List(a + b, a + c, b + c).exists(_.isZero)
