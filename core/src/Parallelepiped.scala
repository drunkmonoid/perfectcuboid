package perfectcuboid

import Arith.*

/** A parallelepiped: a box whose faces are parallelograms, spanned by three edge vectors u, v,
  * w of lengths a, b, c. Its lengths are the 3 edges, 12 face diagonals (6 different, |u ± v|,
  * |u ± w|, |v ± w|) and 4 body diagonals (|u + v + w|, and with one sign turned). It is
  * perfect when all 13 are whole numbers. The perfect cuboid is the case where the edges are
  * at right angles; Sawyer and Reiter found in 2009 that slanted ones exist.
  *
  * The shape is held by the edges and p = 2u·v, q = 2u·w, r = 2v·w. Then every squared length
  * is a whole number: |u ± v|² = a² + b² ± p, and |u + v − w|² = a² + b² + c² + p − q − r.
  */
final case class Parallelepiped(a: Long, b: Long, c: Long, p: Long, q: Long, r: Long):

  private val edgeSquares = a * a + b * b + c * c

  /** The squares of the six face diagonals: |u + v|², |u − v|², |u + w|², and so on. */
  def faceDiagonalSquares: List[Long] =
    List(
      a * a + b * b + p,
      a * a + b * b - p,
      a * a + c * c + q,
      a * a + c * c - q,
      b * b + c * c + r,
      b * b + c * c - r
    )

  /** The squares of the four body diagonals: |u + v + w|², and with each pair of signs turned. */
  def bodyDiagonalSquares: List[Long] =
    List(edgeSquares + p + q + r, edgeSquares + p - q - r, edgeSquares - p + q - r, edgeSquares - p - q + r)

  /** Whether u, v, w really span a solid: twice their Gram matrix of dot products,
    * [[2a², p, q], [p, 2b², r], [q, r, 2c²]], must be positive definite, which is the case
    * exactly when its leading minors are positive. Otherwise the "box" is flat.
    */
  def isSolid: Boolean =
    val (x, y, z, pp, qq, rr) =
      (BigInt(2 * a * a), BigInt(2 * b * b), BigInt(2 * c * c), BigInt(p), BigInt(q), BigInt(r))
    val det = x * (y * z - rr * rr) - pp * (pp * z - rr * qq) + qq * (pp * rr - y * qq)
    x > 0 && x * y - pp * pp > 0 && det > 0

  /** Whether it is solid and all 13 lengths are whole. */
  def isPerfect: Boolean = isSolid && (faceDiagonalSquares ++ bodyDiagonalSquares).forall(isPerfectSquare)

  /** Whether its edges meet at right angles. */
  def isCuboid: Boolean = p == 0 && q == 0 && r == 0

  /** The smaller perfect parallelepiped this one is k times, with its k, if there is one:
    * scaling by k multiplies the edges by k and p, q and r by k².
    */
  def scaledFrom: Option[(factor: Long, from: Parallelepiped)] =
    NumberTheory
      .divisors(gcd(gcd(a, b), c))
      .filter(_ > 1)
      .filter(k => p % (k * k) == 0 && q % (k * k) == 0 && r % (k * k) == 0)
      .map(k => (factor = k, from = Parallelepiped(a / k, b / k, c / k, p / (k * k), q / (k * k), r / (k * k))))
      .find(_.from.isPerfect)

  private def lengths(squares: List[Long]) = squares.map(n => exactSqrt(n).fold(s"√$n")(_.toString))

  /** Its edges, face diagonals and body diagonals, the irrational ones as square roots. */
  def describe: String =
    s"edges $a, $b, $c; face diagonals ${lengths(faceDiagonalSquares).mkString(", ")}; " +
      s"body diagonals ${lengths(bodyDiagonalSquares).mkString(", ")}"

object Parallelepiped:

  /** Sawyer and Reiter's example, the first perfect parallelepiped found. */
  val sawyerReiter: Parallelepiped = Parallelepiped(271, 106, 103, 2 * 9826, 2 * 6647, 2 * 5822)

  /** Every way of writing n as x² + y² with x ≥ y ≥ 0. */
  def twoSquares(n: Long): List[(Long, Long)] =
    (isqrt(n) to ceilSqrt(n / 2) by -1).toList.flatMap(x => exactSqrt(n - x * x).filter(_ <= x).map(x -> _))

  /** The values of p = 2u·v ≥ 0 that make both face diagonals of edges a and b whole. Since
    * |u + v|² + |u − v|² = 2(a² + b²), the two diagonals x and y split 2(a² + b²) into two
    * squares, and p = x² − a² − b². The split into (a + b)² and (a − b)² is the flat one, u
    * and v in a line, and is left out.
    */
  def slants(a: Long, b: Long): List[Long] =
    twoSquares(2 * (a * a + b * b)).map((x, _) => x * x - a * a - b * b).filter(_ < 2 * a * b)

  /** Every perfect parallelepiped with edges a ≥ b ≥ c up to `maxEdge`, each shape once.
    *
    * Turning an edge vector round changes the signs of two of p, q, r without changing the
    * shape, so p ≥ 0 and q ≥ 0 can be assumed, and only r takes both signs.
    */
  def search(maxEdge: Long): List[Parallelepiped] =
    val slantsOf = (for a <- 1L to maxEdge; b <- 1L to a yield (a, b) -> slants(a, b)).toMap
    for
      a <- (1L to maxEdge).toList
      b <- (1L to a).toList
      ps = slantsOf((a, b))
      if ps.nonEmpty
      c <- (1L to b).toList
      qs = slantsOf((a, c))
      if qs.nonEmpty
      rs = slantsOf((b, c))
      if rs.nonEmpty
      p <- ps
      q <- qs
      r <- rs.flatMap(r => List(r, -r)).distinct
      shape = Parallelepiped(a, b, c, p, q, r)
      if shape.isPerfect
    yield shape
