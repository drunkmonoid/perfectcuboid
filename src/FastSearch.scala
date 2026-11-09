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

  /** The legs y > a that pair with a, sorted. Same as filtering
    * `Pythagorean.legsPairedWith(a)`, but without building any lists.
    */
  def legsAbove(a: Long): IArray[Long] = legsAbove(a, NumberTheory.factorize(a))

  /** The same, given the factorisation of a. */
  def legsAbove(a: Long, factors: NumberTheory.Factorization): IArray[Long] =
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

  /** 63 · 65 · 11. Only 4.5% of remainders mod this number are remainders of squares, and the
    * remainder mod 64 then rules out most of those: together, 0.84% of random sums get through.
    * Sums of squared legs are not random, though, and about 9% of them get through (RESULTS.md).
    */
  private val Modulus = 45_045

  /** Which remainders mod `Modulus` squares can leave. */
  private val isSquareRemainder: IArray[Boolean] =
    val squares = new Array[Boolean](Modulus)
    for r <- 0 until Modulus do squares((r.toLong * r % Modulus).toInt) = true
    IArray.unsafeFromArray(squares)

  /** The Euler bricks a × b × c with b and c taken from `legs`, all the legs above a.
    *
    * Most pairs are ruled out by remainders alone: if b² + c² leaves a remainder mod 45045
    * that no square leaves, it is not a square. The remainders of b² and c² are computed once
    * per leg, so each pair costs an addition and a table lookup. Only the rest go on to the
    * exact test, which needs 128-bit arithmetic once the legs pass 2³¹.
    */
  private def eulerBricksWithLegs(a: Long, legs: IArray[Long]): List[Cuboid] =
    val squareRemainders = legs.map(y => (y % Modulus * (y % Modulus) % Modulus).toInt)
    var found = List.empty[Cuboid]
    // Going backwards and prepending leaves the list in increasing order.
    var i = legs.length - 2
    while i >= 0 do
      val b = legs(i)
      var j = legs.length - 1
      while j > i do
        val c = legs(j)
        val sum = squareRemainders(i) + squareRemainders(j)
        if isSquareRemainder(if sum >= Modulus then sum - Modulus else sum) && Wide.isSumOfSquaresSquare(b, c) then
          found = Cuboid.of(a, b, c).toList ::: found
        j -= 1
      i -= 1
    found

  /** The Euler bricks with smallest edge from `first` to `last`, in order. The edges are
    * factorised all together by the sieve, instead of one at a time.
    */
  def eulerBricksBetween(first: Long, last: Long): List[Cuboid] = eulerBricksIterator(first, last).toList

  /** The same bricks, handed out one at a time, without first collecting them in a list. */
  def eulerBricksIterator(first: Long, last: Long): Iterator[Cuboid] =
    val factors = Sieve.factorizeBlock(first, last)
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
  def allEulerBricksParallel(blockSize: Long = 2000, cores: Int = Runtime.getRuntime.availableProcessors)(using
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
