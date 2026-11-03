package perfectcuboid

/** Progress reports for long searches: how far, how fast, and how long is left. All pure; the
  * hunt feeds in the clock readings.
  */
object Eta:

  /** How far the search had got (`searchedUpTo`) at a moment, in seconds since some start. */
  final case class Sample(seconds: Double, searchedUpTo: Long)

  /** The samples from the last `span` seconds, with the new one added. The oldest is kept even
    * when it is older, so that two samples are always left to measure a rate from.
    */
  def window(samples: Vector[Sample], now: Sample, span: Double = 60): Vector[Sample] =
    val all = samples :+ now
    all.drop(math.max(0, math.min(all.indexWhere(s => now.seconds - s.seconds <= span), all.size - 2)))

  /** Keys searched per second over the samples. The rate over the recent past, rather than
    * since the start, because each key takes longer than the last: an Euler brick search does
    * more work for larger edges.
    */
  def rate(samples: Vector[Sample]): Option[Double] =
    for
      first <- samples.headOption
      last <- samples.lastOption
      if last.seconds > first.seconds
    yield (last.searchedUpTo - first.searchedUpTo) / (last.seconds - first.seconds)

  /** Seconds left to reach `upTo` at this rate: a little optimistic, for the same reason. */
  def secondsLeft(searchedUpTo: Long, upTo: Long, rate: Double): Option[Double] =
    Option.when(rate > 0)((upTo - searchedUpTo) / rate)

  /** A duration for people: "45 s", "12 min", "3 h 20 min", "4 days". */
  def duration(seconds: Double): String =
    val s = math.round(seconds)
    if s < 60 then s"$s s"
    else if s < 3600 then s"${s / 60} min ${s % 60} s"
    else if s < 2 * 86400 then s"${s / 3600} h ${s % 3600 / 60} min"
    else s"${s / 86400} days"

  /** One progress line. */
  def line(searchedUpTo: Long, upTo: Long, found: Long, what: String, rate: Option[Double]): String =
    val done = 100.0 * searchedUpTo / upTo
    val speed = rate.fold("")(r => f", $r%,.0f per second")
    val left = rate.flatMap(secondsLeft(searchedUpTo, upTo, _)).fold("")(s => s", about ${duration(s)} left")
    f"  searched up to $searchedUpTo%,d ($done%.1f%%), $found%,d $what$speed$left"
