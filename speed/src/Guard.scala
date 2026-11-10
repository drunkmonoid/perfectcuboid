package perfectcuboid.speed

import java.nio.file.{Files, Path}

/** A guard against the search getting slower without anyone noticing.
  *
  * CI runs on whatever machine it is given, so a time alone says little. Instead the guard times
  * the search and a reference kernel side by side, in the same JVM, taking turns, and compares
  * their ratio with the one written down in `speed/baseline.txt`. The machine's speed cancels
  * out; what is left moves only when the search itself does.
  *
  *   ./mill speed.run                 # measure and report
  *   ./mill speed.run --check         # ... and fail if the search is far slower than the baseline
  */
object Guard:

  /** The edges the search is timed on: just below 10⁹, where the long hunts spend their time. */
  val First: Long = 999_000_001L
  val Edges: Int = 3000

  /** How much slower than the baseline the search may get before `--check` fails. */
  val Tolerance: Double = 1.5

  /** The reference: every pair a < b ≤ n with a² + b² a square, by brute force. The same kind
    * of work as the search's inner loop (multiplications, a floating-point square root, a
    * comparison) in code that never changes. Do not change it: the baseline is a ratio to it.
    */
  def reference(n: Int): Long =
    var count = 0L
    var a = 1L
    while a <= n do
      var b = a + 1
      while b <= n do
        val sum = a * a + b * b
        val root = Math.round(Math.sqrt(sum.toDouble))
        if root * root == sum then count += 1
        b += 1
      a += 1
    count

  /** The search over the guard's edges: the number of Euler bricks found. */
  def search(first: Long, edges: Int): Long =
    perfectcuboid.FastSearch.eulerBricksBetween(first, first + edges - 1).size.toLong

  /** The size of the reference that takes about as long as the search, near 10⁹. */
  val ReferenceSize: Int = 6000

  private def nanos(work: => Long): (time: Long, result: Long) =
    val start = System.nanoTime
    val result = work
    (time = System.nanoTime - start, result = result)

  private def median(xs: Seq[Long]): Long = xs.sorted.apply(xs.size / 2)

  /** The two medians, after warming up, and their ratio, search over reference. */
  def measure(rounds: Int): (search: Long, reference: Long, ratio: Double) =
    // Warming up lets the JIT compile both before anything is timed.
    for _ <- 1 to 5 do
      search(First, Edges): Unit
      reference(ReferenceSize): Unit
    val pairs = (1 to rounds).map(_ => (nanos(search(First, Edges)).time, nanos(reference(ReferenceSize)).time))
    val (s, r) = (median(pairs.map(_._1)), median(pairs.map(_._2)))
    (search = s, reference = r, ratio = s.toDouble / r)

  def main(args: Array[String]): Unit =
    val baselineFile = Path.of("speed", "baseline.txt")
    val baseline =
      if Files.exists(baselineFile) then
        Files
          .readAllLines(baselineFile)
          .toArray
          .map(_.toString.trim)
          .find(l => l.nonEmpty && !l.startsWith("#"))
          .flatMap(_.toDoubleOption)
      else None
    val m = measure(rounds = 15)
    val line =
      f"search ${m.search / 1e6}%.1f ms, reference ${m.reference / 1e6}%.1f ms, ratio ${m.ratio}%.3f" +
        baseline.fold("")(b => f", baseline $b%.3f, ${m.ratio / b}%.2f× the baseline")
    println(line)
    // On GitHub, also as a notice on the run's page.
    if sys.env.contains("GITHUB_ACTIONS") then println(s"::notice title=speed guard::$line")
    if args.contains("--check") then
      baseline match
        case Some(b) if m.ratio > b * Tolerance =>
          println(f"The search is ${m.ratio / b}%.2f× as slow as the baseline, beyond $Tolerance×.")
          sys.exit(1)
        case None =>
          println(s"No baseline in $baselineFile.")
          sys.exit(1)
        case _ => ()
