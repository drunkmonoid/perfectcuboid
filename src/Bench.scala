package perfectcuboid

import cats.effect.unsafe.IORuntime
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

  /** The fastest run. */
  def min: Double = sorted.head

  /** The slowest run. */
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
  val baseline: (name: String, search: BrickSearch) =
    (name = "readable", search = n => Search.allEulerBricks.takeWhile(_.a <= n).toVector)

  /** The faster searches, each timed against the baseline. */
  val others: List[(String, BrickSearch)] = List(
    "fast, one edge at a time" -> (n => FastSearch.allEulerBricksOneByOne.takeWhile(_.a <= n).toVector),
    "fast, sieving blocks" -> (n => FastSearch.allEulerBricks.takeWhile(_.a <= n).toVector),
    // The stream is a description of the search; `unsafeRunSync` runs it, on Cats Effect's
    // global runtime, and waits for the result.
    "parallel, fs2 chunks" -> (n =>
      Blocks.finds(Quarry.eulerBricks, 1, n).compile.toVector.unsafeRunSync()(using IORuntime.global)
    ),
    "parallel, futures" -> (n => FastSearch.allEulerBricksParallel().takeWhile(_.a <= n).toVector)
  )

  /** The result of `body` and how many seconds it took. */
  def time[A](body: => A): (result: A, seconds: Double) =
    val start = System.nanoTime
    val result = body
    (result = result, seconds = (System.nanoTime - start) / 1e9)

  /** Runs `body` `warmUps` times untimed, then `runs` times timed. */
  def measure[A](warmUps: Int, runs: Int)(body: => A): (result: A, timing: Timing) =
    for _ <- 1 to warmUps do body: Unit
    val timed = Vector.fill(runs)(time(body))
    (result = timed.head.result, timing = Timing(timed.map(_.seconds)))

  /** Where the timings were taken. The heap cap matters too, but there is no way to read it
    * that also builds for Scala Native, whose `Runtime` has no `maxMemory`: so set it in the
    * command, with `-J -Xmx2g`.
    */
  def machine: String =
    val platform = if Platform.isNative then "Scala Native" else s"Java ${System.getProperty("java.version")}"
    s"$platform, ${Runtime.getRuntime.availableProcessors} cores"

  /** Times every search and prints a line for each. */
  def run(maxSmallestEdge: Long, runs: Int, warmUps: Int = 2): Unit =
    def line(name: String, timing: Timing, note: String) =
      println(f"  $name%-26s ${timing.median}%7.3f s  (${timing.min}%.3f to ${timing.max}%.3f)  $note")
    println(s"Euler bricks with smallest edge <= $maxSmallestEdge, $runs timed runs after $warmUps warm-ups")
    println(machine)
    val (expected, base) = measure(warmUps, runs)(baseline.search(maxSmallestEdge))
    line(baseline.name, base, s"${expected.size} bricks")
    for (name, search) <- others do
      val (bricks, timing) = measure(warmUps, runs)(search(maxSmallestEdge))
      line(name, timing, f"${base.median / timing.median}%5.1fx faster, same result: ${bricks == expected}")
