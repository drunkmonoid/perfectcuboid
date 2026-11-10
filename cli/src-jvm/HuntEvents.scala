package perfectcuboid

import jdk.jfr.{Category, Description, Event, Label, Name}

/** A block of keys searched, as a Java Flight Recorder event: a profile of a hunt then shows
  * each block, with its keys, its finds and how long it took, beside the JVM's own events.
  */
@Name("perfectcuboid.Block")
@Label("Block searched")
@Category(Array("perfectcuboid"))
@Description("One block of a search: its keys, from first to last, and what it found.")
class BlockEvent extends Event:
  @Label("First key") var first: Long = 0
  @Label("Last key") var last: Long = 0
  @Label("Finds") var finds: Int = 0

/** A checkpoint written to a hunt's file. */
@Name("perfectcuboid.Checkpoint")
@Label("Checkpoint written")
@Category(Array("perfectcuboid"))
@Description("A block's finds and checkpoint written to the results file.")
class CheckpointEvent extends Event:
  @Label("Searched up to") var searchedUpTo: Long = 0
  @Label("Bytes") var bytes: Int = 0

/** The hunt's events, on the JVM: recorded only while a flight recording that enables them runs,
  * and almost free otherwise.
  */
object HuntEvents:

  /** Does `work`, the search of the keys `first` to `last`, as a block event. */
  def block[A](first: Long, last: Long)(work: => Vector[A]): Vector[A] =
    val event = BlockEvent()
    event.begin()
    val finds = work
    event.end()
    if event.shouldCommit then
      event.first = first
      event.last = last
      event.finds = finds.size
      event.commit()
    finds

  /** Records that a checkpoint was written. */
  def checkpoint(searchedUpTo: Long, bytes: Int): Unit =
    val event = CheckpointEvent()
    if event.shouldCommit then
      event.searchedUpTo = searchedUpTo
      event.bytes = bytes
      event.commit()
