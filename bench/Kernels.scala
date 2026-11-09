package perfectcuboid.bench

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import perfectcuboid.*

/** Inputs like those the search meets near 10⁹: 100 consecutive edges, their factorisations,
  * their legs, and the pairs of legs that get past the remainder filter to the exact test.
  */
object Inputs:
  val first = 999_999_901L
  val last = 1_000_000_000L
  val factors: IArray[NumberTheory.Factorization] = Sieve.factorizeBlock(first, last)
  val legs: IndexedSeq[IArray[Long]] = (first to last).map(a => FastSearch.legsAbove(a, factors((a - first).toInt)))

  /** The same filter as the search's, mod 45045 = 63 · 65 · 11. */
  val squareRemainder: Array[Boolean] =
    val table = new Array[Boolean](45_045)
    for r <- 0 until 45_045 do table((r.toLong * r % 45_045).toInt) = true
    table
  private def passes(b: Long, c: Long) = squareRemainder(
    ((b % 45_045 * (b % 45_045) + c % 45_045 * (c % 45_045)) % 45_045).toInt
  )

  val pairs: Array[(Long, Long)] =
    legs.flatMap(ls => ls.indices.flatMap(i => (i + 1 until ls.length).map(j => (ls(i), ls(j))))).filter(passes).toArray
  val wideSquares: Array[Wide] = pairs.map((b, c) => Wide.square(b) + Wide.square(c))
  val longSquares: Array[Long] = Array.tabulate(1000)(i => 4_000_000_000_000_000_000L + 1_234_567_891L * i)

/** The pieces the search spends its time in, each timed per call in nanoseconds, averaged
  * over many different inputs, since a single input can be unusually easy or hard.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
class Kernels:
  import Inputs.*
  private val montgomery = Montgomery(1_000_000_007L)

  @Benchmark @OperationsPerInvocation(1000) def isqrtLong: Long =
    var sum = 0L
    for n <- longSquares do sum += Arith.isqrt(n)
    sum

  @Benchmark @OperationsPerInvocation(1000) def isqrtWide: Long =
    var sum = 0L
    for i <- 0 until 1000 do sum += Wide.isqrt(wideSquares(i).hi, wideSquares(i).lo)
    sum

  /** The exact test, on pairs that passed the filter: what the search spends most time on. */
  @Benchmark @OperationsPerInvocation(1000) def exactSquareTest: Int =
    var count = 0
    for i <- 0 until 1000 do if Wide.isSumOfSquaresSquare(pairs(i)._1, pairs(i)._2) then count += 1
    count

  @Benchmark @OperationsPerInvocation(1000) def montgomeryMulMod: Long =
    var x = 123_456_789L
    for _ <- 0 until 1000 do x = montgomery.mulMod(x, 987_654_321_098L)
    x

/** Whole steps of the search on the 100 edges, in microseconds per edge. */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
class Searching:
  import Inputs.*

  @Benchmark @OperationsPerInvocation(100) def sieve: IArray[NumberTheory.Factorization] =
    Sieve.factorizeBlock(first, last)

  @Benchmark @OperationsPerInvocation(100) def legs: Int =
    (first to last).map(a => FastSearch.legsAbove(a, factors((a - first).toInt)).length).sum

  @Benchmark @OperationsPerInvocation(100) def wholeSearch: List[Cuboid] = FastSearch.eulerBricksBetween(first, last)

  /** The search's pair loop with both remainder filters but no exact test: a control, to
    * tell how much of `wholeSearch` the exact tests take.
    */
  @Benchmark @OperationsPerInvocation(100) def filtersOnly: Int =
    var passed = 0
    for ls <- Inputs.legs do
      val remainders = ls.map(y => (y % 45_045 * (y % 45_045) % 45_045).toInt)
      // While loops, as in FastSearch: the same loop, with the exact test left out.
      var i = 0
      while i < ls.length do
        var j = i + 1
        while j < ls.length do
          val sum = remainders(i) + remainders(j)
          if squareRemainder(if sum >= 45_045 then sum - 45_045 else sum) && Arith.mightBeSquare(
              ls(i) * ls(i) + ls(j) * ls(j)
            )
          then passed += 1
          j += 1
        i += 1
    passed
