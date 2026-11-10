package perfectcuboid

/** The hunt's events: Scala Native has no flight recorder, so here they do nothing. */
object HuntEvents:

  def block[A](first: Long, last: Long)(work: => Vector[A]): Vector[A] =
    val _ = (first, last)
    work

  def checkpoint(searchedUpTo: Long, bytes: Int): Unit =
    val _ = (searchedUpTo, bytes)
