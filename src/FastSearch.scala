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

  /** The legs y with a < y <= Edge.Max that pair with a, sorted. Same as filtering
    * `Pythagorean.legsPairedWith(a)`, but without building any lists.
    */
  def legsAbove(a: Long): IArray[Long] =
    val n = a * a
    val ds = divisors(NumberTheory.factorize(a).map((p, k) => p -> 2 * k))
    val legs = new Array[Long](ds.length)
    var size = 0
    var i = 0
    while i < ds.length do
      val d = ds(i)
      val e = n / d
      if d < e && ((e - d) & 1) == 0 then
        val y = (e - d) / 2
        if y > a && y <= Edge.Max then
          legs(size) = y
          size += 1
      i += 1
    val result = java.util.Arrays.copyOf(legs, size)
    java.util.Arrays.sort(result)
    // "unsafe" only because we promise never to change `result` again, and we don't.
    IArray.unsafeFromArray(result)

  /** Same result as `Search.eulerBricksWithSmallestEdge`. */
  def eulerBricksWithSmallestEdge(a: Long): List[Cuboid] =
    val legs = legsAbove(a)
    var found = List.empty[Cuboid]
    // Going backwards and prepending leaves the list in increasing order.
    var i = legs.length - 2
    while i >= 0 do
      val b = legs(i)
      var j = legs.length - 1
      while j > i do
        val c = legs(j)
        // Both are at most Edge.Max, so b² + c² cannot overflow.
        if Arith.isPerfectSquare(b * b + c * c) then found = Cuboid.of(a, b, c).toList ::: found
        j -= 1
      i -= 1
    found

  /** The Euler bricks with smallest edge from `first` to `last`, in order. */
  def eulerBricksBetween(first: Long, last: Long): List[Cuboid] =
    (first to last).toList.flatMap(eulerBricksWithSmallestEdge)

  /** Same result as `Search.allEulerBricks`. */
  def allEulerBricks: LazyList[Cuboid] =
    LazyList.iterate(1L)(_ + 1).takeWhile(_ <= Edge.Max).flatMap(eulerBricksWithSmallestEdge)

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
      eulerBricksBetween(start, math.min(start + blockSize - 1, Edge.Max))
    LazyList
      .iterate(1L)(_ + blockSize * cores)
      .takeWhile(_ <= Edge.Max)
      .flatMap: start =>
        val blocks = (0 until cores).map(start + _ * blockSize).filter(_ <= Edge.Max).toList
        Await.result(Future.traverse(blocks)(block => Future(searchBlock(block))), Duration.Inf).flatten
