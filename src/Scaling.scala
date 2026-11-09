package perfectcuboid

import cats.effect.IO
import cats.syntax.all.*
import scala.concurrent.duration.SECONDS

/** How the search's speed grows with the number of threads: the same edges searched again and
  * again, on more threads each time. The table and the chart are pure; only `run` touches the
  * clock.
  */
object Scaling:

  /** One timed search of `edges` edges on `threads` threads. */
  final case class Row(threads: Int, edges: Long, seconds: Double):
    def perSecond: Double = edges / seconds

  def header(first: Long, edges: Long, blockSize: Long): String =
    f"Euler bricks with smallest edge from $first%,d, $edges%,d edges, in blocks of $blockSize%,d"

  /** A row, with its speed compared to the row with the fewest threads, `base`. */
  def line(row: Row, base: Row): String =
    val speedUp = row.perSecond / base.perSecond
    val perfect = row.threads.toDouble / base.threads
    f"  ${row.threads}%2d ${if row.threads == 1 then "thread " else "threads"}  ${row.seconds}%7.2f s  ${row.perSecond}%,9.0f edges/s  " +
      f"${speedUp}%5.2fx  (${100 * speedUp / perfect}%3.0f%% of perfect)"

  /** Edges per second against threads, measured and as it would be if every thread added as
    * much as the first.
    */
  def chart(rows: List[Row]): String =
    val one = rows.minBy(_.threads)
    Charts.lines(
      "Search speed near 10⁹ by number of threads",
      "threads",
      "edges per second",
      List(
        Charts.Series("measured", 1, rows.map(r => (r.threads.toDouble, r.perSecond))),
        Charts
          .Series("perfect scaling", 2, rows.map(r => (r.threads.toDouble, one.perSecond * r.threads / one.threads)))
      ),
      linear = true,
      tip = (name, threads, speed) => f"$name: ${speed.round}%,d edges per second on ${threads.round} threads"
    )

  /** Searches the edges once on `threads` threads, and returns how long it took. */
  def time(first: Long, edges: Long, blockSize: Long, threads: Int): IO[Row] =
    for
      start <- IO.monotonic
      _ <- Blocks
        .stream(Quarry.eulerBricks, first, first + edges - 1, blockSize, threads, summarize = false)
        .compile
        .drain
      end <- IO.monotonic
    yield Row(threads, edges, (end - start).toUnit(SECONDS))

  /** Times every thread count in turn, from the fewest, printing each row as it comes. A
    * warm-up on a tenth as many edges goes first, so that the JIT compiler has done its work
    * before the clock starts.
    */
  def run(first: Long, edges: Long, blockSize: Long, threadCounts: List[Int]): IO[List[Row]] =
    val counts = threadCounts.distinct.sorted
    for
      _ <- IO.println(header(first, edges, blockSize))
      _ <- time(first, edges / 10, blockSize, counts.max)
      base <- time(first, edges, blockSize, counts.head)
      _ <- IO.println(line(base, base))
      rest <- counts.tail.traverse(threads =>
        time(first, edges, blockSize, threads).flatTap(r => IO.println(line(r, base)))
      )
    yield base :: rest
