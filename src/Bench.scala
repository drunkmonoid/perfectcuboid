package perfectcuboid

import scala.concurrent.ExecutionContext.Implicits.global

/** Repeated timings of one piece of code, in seconds. The statistics are pure. */
final case class Timing(seconds: Vector[Double]):
  require(seconds.nonEmpty, "a timing needs at least one run")

  private def sorted = seconds.sorted

  /** The middle run, or the mean of the two middle ones. Unlike the mean of all runs, one
    * slow run (a garbage collection, another program waking up) barely moves it.
    */
  def median: Double =
    val n = seconds.size
    if n % 2 == 1 then sorted(n / 2) else (sorted(n / 2 - 1) + sorted(n / 2)) / 2

  def min: Double = sorted.head
  def max: Double = sorted.last

/** Timing the searches against each other. Reading the clock is a side effect, so this lives
  * out here, and `Main` runs it inside `IO.blocking`.
  *
  * The JVM starts by interpreting the code and only compiles the busy parts to machine code
  * as it goes, so first runs are slow. Each search is therefore run a few times untimed
  * (warm-up), then several times timed, and the median is reported with the spread.
  */
object Bench:

  /** A search that finds every Euler brick with smallest edge <= its argument. */
  type BrickSearch = Long => Vector[Cuboid]

  /** The search the others are compared with: the readable one. */
  val baseline: (String, BrickSearch) = "readable" -> (n => Search.allEulerBricks.takeWhile(_.a <= n).toVector)

  val others: List[(String, BrickSearch)] = List(
    "fast" -> (n => FastSearch.allEulerBricks.takeWhile(_.a <= n).toVector),
    "parallel" -> (n => FastSearch.allEulerBricksParallel().takeWhile(_.a <= n).toVector)
  )

  def time[A](body: => A): (A, Double) =
    val start = System.nanoTime
    val result = body
    (result, (System.nanoTime - start) / 1e9)

  def measure[A](warmUps: Int, runs: Int)(body: => A): (A, Timing) =
    for _ <- 1 to warmUps do body: Unit
    val timed = Vector.fill(runs)(time(body))
    (timed.head._1, Timing(timed.map(_._2)))

  /** Where the timings were taken. The heap cap matters: too small a heap makes the garbage
    * collector run all the time, and a parallel search allocates on every core at once.
    */
  def machine: String =
    val runtime = Runtime.getRuntime
    f"Java ${System.getProperty("java.version")}, ${runtime.availableProcessors} cores, heap cap ${runtime.maxMemory.toDouble / (1L << 30)}%.1f GiB"

  def run(maxSmallestEdge: Long, runs: Int, warmUps: Int = 2): Unit =
    def line(name: String, timing: Timing, note: String) =
      println(f"  $name%-9s ${timing.median}%7.3f s  (${timing.min}%.3f to ${timing.max}%.3f)  $note")
    println(s"Euler bricks with smallest edge <= $maxSmallestEdge, $runs timed runs after $warmUps warm-ups")
    println(machine)
    val (expected, base) = measure(warmUps, runs)(baseline._2(maxSmallestEdge))
    line(baseline._1, base, s"${expected.size} bricks")
    for (name, search) <- others do
      val (bricks, timing) = measure(warmUps, runs)(search(maxSmallestEdge))
      line(name, timing, f"${base.median / timing.median}%5.1fx faster, same result: ${bricks == expected}")
