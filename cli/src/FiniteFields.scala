package perfectcuboid

/** The perfect cuboid equations over the integers mod a prime p:
  *
  *   a² + b² = x²,  a² + c² = y²,  b² + c² = z²,  a² + b² + c² = w²
  *
  * Four equations in seven unknowns: if they behaved like random ones, each would cut the p⁷
  * choices by about p, leaving about p³ solutions. How far the true count is from p³, and how
  * that depends on p, says something about the shape the equations define, the way the number
  * of points of a curve mod p does (Hasse–Weil).
  *
  * The count is quick: for each (a, b, c), the number of x with x² = t is 1 + (t/p), (t/p) the
  * Legendre symbol, so the solutions over (a, b, c) are the product of four such counts.
  */
object FiniteFields:

  /** For each t mod p, how many x mod p have x² = t. */
  def roots(p: Int): Array[Int] =
    val count = new Array[Int](p)
    for x <- 0 until p do count((x.toLong * x % p).toInt) += 1
    count

  /** The counts for one prime: all solutions, and those with no zero among a, b, c, x, y, z, w,
    * the ones a real perfect cuboid would reduce to when p divides none of its lengths.
    */
  final case class Count(p: Int, all: Long, noZero: Long):
    def cube: Long = p.toLong * p * p
    def ratio: Double = all.toDouble / cube

    /** How far from p³, in units of p², the size a correction of the next order would have. */
    def deviation: Double = (all - cube).toDouble / (p.toLong * p)

  def count(p: Int): Count =
    val r = roots(p)
    val sq = Array.tabulate(p)(x => (x.toLong * x % p).toInt)
    var all = 0L
    var noZero = 0L
    var a = 0
    while a < p do
      var b = 0
      while b < p do
        val ab = (sq(a) + sq(b)) % p
        val rab = r(ab)
        if rab > 0 then
          var c = 0
          while c < p do
            val ac = (sq(a) + sq(c)) % p
            val bc = (sq(b) + sq(c)) % p
            val abc = (ab + sq(c)) % p
            val n = rab.toLong * r(ac) * r(bc) * r(abc)
            all += n
            // With a, b, c and the four sums all nonzero, each sum has two roots, none zero.
            if n > 0 && a != 0 && b != 0 && c != 0 && ab != 0 && ac != 0 && bc != 0 && abc != 0 then noZero += n
            c += 1
        b += 1
      a += 1
    Count(p, all, noZero)

  /** p = x² + k·y² with x, y > 0, if p has such a form, x odd when k = 1. */
  def form(p: Int, k: Int): Option[(x: Long, y: Long)] =
    (1L to math.sqrt(p.toDouble / k).toLong).iterator
      .flatMap: y =>
        val rest = p - k * y * y
        Arith.exactSqrt(rest).filter(_ > 0).map(x => if k == 1 && x % 2 == 0 then (x = y, y = x) else (x = x, y = y))
      .nextOption()

  /** The count the formulas found here predict, in each class of p mod 8: with p = x² + 2y²
    * (p ≡ 1, 3) and p = u² + v², u odd (p ≡ 1, 5), the count is p³ + 9p² − 9p plus (p − 1) times
    * 0 (p ≡ 7), −16y² (p ≡ 3), 4u² − 8v² (p ≡ 5) or 4u² − 8v² + 16x² (p ≡ 1). Found by fitting
    * the counts, not proved: `lines` checks them against each count.
    */
  def predicted(p: Int): Long =
    val q = p.toLong
    val main = q * q * q + 9 * q * q - 9 * q
    lazy val two = form(p, 2).get
    lazy val one = form(p, 1).get
    val term = p % 8 match
      case 7 => 0L
      case 3 => -16 * two.y * two.y
      case 5 => 4 * one.x * one.x - 8 * one.y * one.y
      case _ => 4 * one.x * one.x - 8 * one.y * one.y + 16 * two.x * two.x
    main + term * (q - 1)

  /** The table for every odd prime up to `limit`, as lines to print. */
  def lines(limit: Int): List[String] =
    val primes = NumberTheory.smallPrimes.takeWhile(_ <= limit).map(_.toInt).filter(_ > 2).toList
    val counts = primes.map(count)
    List(
      s"Solutions of the perfect cuboid equations mod p, against the p³ of random equations:",
      "| p | p mod 8 | solutions | / p³ | (solutions − p³) / p² | with no zero | as the formula |",
      "| ---: | ---: | ---: | ---: | ---: | ---: | --- |"
    ) ++ counts.map(c =>
      f"| ${c.p} | ${c.p % 8} | ${c.all}%,d | ${c.ratio}%.4f | ${c.deviation}%.2f | ${c.noZero}%,d | ${
          if predicted(c.p) == c.all then "yes" else "NO"
        } |"
    ) :+ s"The formula gives every count: ${counts.forall(c => predicted(c.p) == c.all)}" :+ ("No solution with all seven nonzero, so a perfect cuboid has a length divisible by p, for p = " +
      counts.filter(_.noZero == 0).map(_.p).mkString(", "))
