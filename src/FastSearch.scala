package perfectcuboid

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

/** The unbounded Euler brick search again, written for speed. Tests check it gives exactly
  * the same results as the readable version in `Search`.
  *
  * Inside, it uses arrays and while loops that mutate local variables, which allocate far
  * less than lists and for-comprehensions. Nothing mutable ever escapes, so from the outside
  * these functions are just as pure: same input, same output, no visible effects.
  */
object FastSearch:

  /** All divisors of the number with these prime factors, unsorted. */
  private def divisors(factors: NumberTheory.Factorization): Array[Long] =
    val out = new Array[Long](factors.map(_._2 + 1).product)
    out(0) = 1
    var size = 1
    // A while loop, not `for (p, k) <- factors do`: a `var` changed inside a closure has to
    // live in a separate heap object, which costs an allocation on every call.
    var rest = factors
    while rest.nonEmpty do
      val (p, k) = rest.head
      val before = size
      var power = 1L
      var i = 0
      while i < k do
        power *= p
        var j = 0
        while j < before do
          out(size) = out(j) * power
          size += 1
          j += 1
        i += 1
      rest = rest.tail
    out

  /** The block searches go up to here: every edge of a `Cuboid` is at most `Edge.Max`. Up to
    * `Search.MaxSmallestEdge`, a² fits in a Long and every leg that pairs with a is within
    * `Edge.Max`, so the search finds every brick. Beyond, it finds every brick whose edges are
    * all within `Edge.Max`, which is every brick a `Cuboid` can hold.
    */
  val MaxSmallestEdge: Long = Edge.Max

  /** The legs y > a that pair with a, sorted. Same as filtering
    * `Pythagorean.legsPairedWith(a)`, but without building any lists.
    */
  def legsAbove(a: Long): IArray[Long] = legsAbove(a, NumberTheory.factorize(a))

  /** The same, given the factorisation of a. */
  def legsAbove(a: Long, factors: NumberTheory.Factorization): IArray[Long] =
    if a > Search.MaxSmallestEdge then legsAboveWide(a, factors) else legsAboveLong(a, factors)

  /** For a above `Search.MaxSmallestEdge`, where a² overflows a Long: the same, in BigInt
    * arithmetic, keeping only the legs up to `Edge.Max`. Slow, but these edges are rarely
    * searched.
    */
  private[perfectcuboid] def legsAboveWide(a: Long, factors: NumberTheory.Factorization): IArray[Long] =
    val n = BigInt(a) * a
    val legs =
      for
        d <- NumberTheory.divisorsBig(factors.map((p, k) => p -> 2 * k))
        if d < a
        difference = n / d - d
        if !difference.testBit(0) && difference / 2 > a && difference / 2 <= Edge.Max
      yield (difference / 2).toLong
    IArray.from(legs.sorted)

  private[perfectcuboid] def legsAboveLong(a: Long, factors: NumberTheory.Factorization): IArray[Long] =
    val n = a * a
    val ds = divisors(factors.map((p, k) => p -> 2 * k))
    val legs = new Array[Long](ds.length)
    var size = 0
    var i = 0
    while i < ds.length do
      val d = ds(i)
      val e = n / d
      if d < e && ((e - d) & 1) == 0 then
        val y = (e - d) / 2
        if y > a then
          legs(size) = y
          size += 1
      i += 1
    val result = java.util.Arrays.copyOf(legs, size)
    java.util.Arrays.sort(result)
    // "unsafe" only because we promise never to change `result` again, and we don't.
    IArray.unsafeFromArray(result)

  /** Same result as `Search.eulerBricksWithSmallestEdge`. */
  def eulerBricksWithSmallestEdge(a: Long): List[Cuboid] = eulerBricksWithLegs(a, legsAbove(a))

  /** The filter's three moduli, each a product of three primes. Each prime alone lets about half
    * of the pairs of legs through, so each modulus lets through about one in nine. The primes
    * were chosen by measuring real legs (RESULTS.md), since legs that pair with the same edge
    * are far from random: 7, 17 and 41, for example, let 74–89% through, and the powers of 2,
    * 3 and 5 are not much better.
    */
  private val FirstModulus = 11 * 43 * 59
  private val SecondModulus = 19 * 53 * 37
  private val ThirdModulus = 13 * 47 * 29

  /** Bit r is set when some square leaves remainder r mod m: one bit per remainder, so that
    * each table, 2 to 5 KB, stays in the processor's fastest cache.
    */
  private def squareRemainderBits(m: Int): IArray[Long] =
    val bits = new Array[Long]((m + 63) / 64)
    for r <- 0 until m do
      val square = (r.toLong * r % m).toInt
      bits(square >>> 6) |= 1L << square
    IArray.unsafeFromArray(bits)

  private val FirstSquares = squareRemainderBits(FirstModulus)
  private val SecondSquares = squareRemainderBits(SecondModulus)
  private val ThirdSquares = squareRemainderBits(ThirdModulus)

  /** The remainders of the legs' squares mod m, each computed once for all its pairs. */
  private def squareRemainders(legs: IArray[Long], m: Int): Array[Int] =
    val out = new Array[Int](legs.length)
    var i = 0
    while i < legs.length do
      val r = legs(i) % m
      out(i) = (r * r % m).toInt
      i += 1
    out

  /** Whether (x + y) mod m, for x and y already below m, is a square remainder. */
  private inline def mightBeSquareSum(x: Int, y: Int, m: Int, squares: IArray[Long]): Boolean =
    val sum = if x + y >= m then x + y - m else x + y
    ((squares(sum >>> 6) >>> sum) & 1) != 0

  /** The Euler bricks a × b × c with b and c taken from `legs`, all the legs above a.
    *
    * Most pairs are ruled out by remainders alone: if b² + c² leaves a remainder mod 27,907
    * that no square leaves, it is not a square. With the remainders of the squared legs
    * computed beforehand, that costs each pair an addition and one bit looked up. The second
    * modulus is only needed for the pairs that pass the first, and so on, and only about one
    * pair in 300 is left for the exact test. (Benchmarks chose three moduli: with two, too
    * many pairs reach the exact test, and a fourth costs more than it saves.)
    */
  private def eulerBricksWithLegs(a: Long, legs: IArray[Long]): List[Cuboid] =
    val first = squareRemainders(legs, FirstModulus)
    val second = squareRemainders(legs, SecondModulus)
    val third = squareRemainders(legs, ThirdModulus)
    var found = List.empty[Cuboid]
    // Going backwards and prepending leaves the list in increasing order.
    var i = legs.length - 2
    while i >= 0 do
      val b = legs(i)
      var j = legs.length - 1
      while j > i do
        if mightBeSquareSum(first(i), first(j), FirstModulus, FirstSquares) &&
          mightBeSquareSum(second(i), second(j), SecondModulus, SecondSquares) &&
          mightBeSquareSum(third(i), third(j), ThirdModulus, ThirdSquares) &&
          Wide.isSumOfSquaresSquare(b, legs(j))
        then found = Cuboid.of(a, b, legs(j)).toList ::: found
        j -= 1
      i -= 1
    found

  /** The Euler bricks with smallest edge from `first` to `last`, in order. The edges are
    * factorised all together by the sieve, instead of one at a time.
    */
  def eulerBricksBetween(first: Long, last: Long): List[Cuboid] = eulerBricksIterator(first, last).toList

  /** The same bricks, handed out one at a time, without first collecting them in a list. */
  def eulerBricksIterator(first: Long, last: Long): Iterator[Cuboid] =
    // Beyond 10¹², the sieve's primes no longer reach the square roots of the edges.
    val factors =
      if last <= Sieve.MaxLast then Sieve.factorizeBlock(first, last)
      else IArray.from((first to last).map(NumberTheory.factorize))
    (first to last).iterator.flatMap(a => eulerBricksWithLegs(a, legsAbove(a, factors((a - first).toInt))))

  /** Same result as `Search.allEulerBricks`, a block of edges at a time. */
  def allEulerBricks: LazyList[Cuboid] =
    val blockSize = 1000L
    LazyList
      .iterate(1L)(_ + blockSize)
      .takeWhile(_ <= Search.MaxSmallestEdge)
      .flatMap(start => eulerBricksBetween(start, math.min(start + blockSize - 1, Search.MaxSmallestEdge)))

  /** The same again, but factorising each edge on its own by trial division, for comparison. */
  def allEulerBricksOneByOne: LazyList[Cuboid] =
    LazyList.iterate(1L)(_ + 1).takeWhile(_ <= Search.MaxSmallestEdge).flatMap(eulerBricksWithSmallestEdge)

  /** Same result as `Search.allEulerBricks`, using every core.
    *
    * The edges are cut into blocks; one block per core is searched at the same time with
    * `Future`s, and the results are put back in order before anyone sees them. The
    * `using ExecutionContext` parameter is the thread pool, which the caller provides.
    */
  def allEulerBricksParallel(blockSize: Long = 2000, cores: Int = Platform.defaultThreads)(using
      ExecutionContext
  ): LazyList[Cuboid] =
    def searchBlock(start: Long): List[Cuboid] =
      eulerBricksBetween(start, math.min(start + blockSize - 1, Search.MaxSmallestEdge))
    LazyList
      .iterate(1L)(_ + blockSize * cores)
      .takeWhile(_ <= Search.MaxSmallestEdge)
      .flatMap: start =>
        val blocks = (0 until cores).map(start + _ * blockSize).filter(_ <= Search.MaxSmallestEdge).toList
        Await.result(Future.traverse(blocks)(block => Future(searchBlock(block))), Duration.Inf).flatten
