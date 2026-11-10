package perfectcuboid.vector

import jdk.incubator.vector.{IntVector, VectorOperators}
import perfectcuboid.{FastSearch, Sieve}

/** Phase 80, an experiment: the search's three remainder filters on SIMD lanes, with the Vector
  * API (`jdk.incubator.vector`), against the plain loop the search uses.
  *
  *   ./mill experiments.vector.run                    # 3,000 edges from 999,000,001
  *   ./mill experiments.vector.run 3000000001 3000
  *
  * For one leg i, the remainders of the squares of the legs j after it are taken 8 at a time:
  * added to i's, brought below m, and looked up in a table of square remainders, all three
  * moduli for all 8 lanes, the lanes that pass every one kept. The lookups are gathers, which
  * Java 17's Vector API reads from an array of indices.
  */
object VectorFilter:

  val Moduli: List[Int] = List(FastSearch.FirstModulus, FastSearch.SecondModulus, FastSearch.ThirdModulus)
  private val Species = IntVector.SPECIES_256
  private val Lanes = Species.length

  /** One int per remainder: 1 when some square leaves it. */
  def table(m: Int): Array[Int] =
    val bits = FastSearch.squareRemainderBits(m)
    Array.tabulate(m)(r => ((bits(r >>> 6) >>> r) & 1).toInt)

  final case class Block(legs: Array[Long], end: Array[Int], r: Array[Array[Int]])

  def block(first: Long, count: Int): Block =
    val factors = Sieve.factorizeBlock(first, first + count - 1)
    val perEdge = (0 until count).map(k => FastSearch.legsAbove(first + k, factors(k)))
    val legs = perEdge.flatMap(_.toSeq).toArray
    val ends = perEdge.scanLeft(0)(_ + _.length).tail
    val end = perEdge.zipWithIndex.flatMap((ls, k) => Seq.fill(ls.length)(ends(k))).toArray
    Block(legs, end, Moduli.map(m => FastSearch.squareRemainders(IArray.from(legs), m)).toArray)

  private val tables = Moduli.map(table).toArray

  /** How many pairs pass the three filters, by the plain loop. */
  def scalar(b: Block): Long =
    val (r1, r2, r3) = (b.r(0), b.r(1), b.r(2))
    val (t1, t2, t3) = (tables(0), tables(1), tables(2))
    val (m1, m2, m3) = (Moduli(0), Moduli(1), Moduli(2))
    var count = 0L
    var i = 0
    while i < b.legs.length do
      var j = i + 1
      while j < b.end(i) do
        var x = r1(i) + r1(j)
        if x >= m1 then x -= m1
        if t1(x) != 0 then
          var y = r2(i) + r2(j)
          if y >= m2 then y -= m2
          if t2(y) != 0 then
            var z = r3(i) + r3(j)
            if z >= m3 then z -= m3
            if t3(z) != 0 then count += 1
        j += 1
      i += 1
    count

  /** The same count, 8 lanes at a time, the leftover lanes of each leg by the plain loop. */
  def vector(b: Block): Long =
    val index = new Array[Int](Lanes)
    var count = 0L
    var i = 0
    while i < b.legs.length do
      var j = i + 1
      val end = b.end(i)
      while j + Lanes <= end do
        var pass = Species.maskAll(true)
        var k = 0
        while k < 3 do
          val m = Moduli(k)
          val sums = IntVector.broadcast(Species, b.r(k)(i)).add(IntVector.fromArray(Species, b.r(k), j))
          val below = sums.sub(m, sums.compare(VectorOperators.GE, m))
          below.intoArray(index, 0)
          val found = IntVector.fromArray(Species, tables(k), 0, index, 0)
          pass = pass.and(found.compare(VectorOperators.NE, 0))
          k += 1
        count += pass.trueCount
        j += Lanes
      while j < end do
        if (0 until 3).forall: k =>
            val x = b.r(k)(i) + b.r(k)(j)
            tables(k)(if x >= Moduli(k) then x - Moduli(k) else x) != 0
        then count += 1
        j += 1
      i += 1
    count

  private def time(work: => Long): (count: Long, ms: Double) =
    val start = System.nanoTime
    val n = work
    (count = n, ms = (System.nanoTime - start) / 1e6)

  private val bitTables = Moduli.map(FastSearch.squareRemainderBits).toArray

  /** The same count as the search itself does it: tables of one bit per remainder, a few KB,
    * small enough for the fastest cache. This is the loop to beat.
    */
  def bits(b: Block): Long =
    val (r1, r2, r3) = (b.r(0), b.r(1), b.r(2))
    val (t1, t2, t3) = (bitTables(0), bitTables(1), bitTables(2))
    val (m1, m2, m3) = (Moduli(0), Moduli(1), Moduli(2))
    var count = 0L
    var i = 0
    while i < b.legs.length do
      var j = i + 1
      while j < b.end(i) do
        var x = r1(i) + r1(j)
        if x >= m1 then x -= m1
        if ((t1(x >>> 6) >>> x) & 1) != 0 then
          var y = r2(i) + r2(j)
          if y >= m2 then y -= m2
          if ((t2(y >>> 6) >>> y) & 1) != 0 then
            var z = r3(i) + r3(j)
            if z >= m3 then z -= m3
            if ((t3(z >>> 6) >>> z) & 1) != 0 then count += 1
        j += 1
      i += 1
    count

  /** The bit tables as 32-bit words, one bit per remainder, as the lanes read them. */
  private val wordTables = Moduli
    .map: m =>
      val bits = FastSearch.squareRemainderBits(m)
      Array.tabulate((m + 31) / 32)(w => ((bits(w / 2) >>> (32 * (w % 2))) & 0xffffffffL).toInt)
    .toArray

  /** The same count, 8 lanes at a time, on the small bit tables: each lane gathers the word its
    * remainder is in and shifts its bit out.
    */
  def vectorBits(b: Block): Long =
    val index = new Array[Int](Lanes)
    var count = 0L
    var i = 0
    while i < b.legs.length do
      var j = i + 1
      val end = b.end(i)
      while j + Lanes <= end do
        var pass = Species.maskAll(true)
        var k = 0
        while k < 3 do
          val m = Moduli(k)
          val sums = IntVector.broadcast(Species, b.r(k)(i)).add(IntVector.fromArray(Species, b.r(k), j))
          val below = sums.sub(m, sums.compare(VectorOperators.GE, m))
          below.lanewise(VectorOperators.LSHR, 5).intoArray(index, 0)
          val words = IntVector.fromArray(Species, wordTables(k), 0, index, 0)
          val bit = words.lanewise(VectorOperators.LSHR, below.and(31)).and(1)
          pass = pass.and(bit.compare(VectorOperators.NE, 0))
          k += 1
        count += pass.trueCount
        j += Lanes
      while j < end do
        if (0 until 3).forall: k =>
            val x = b.r(k)(i) + b.r(k)(j)
            val y = if x >= Moduli(k) then x - Moduli(k) else x
            ((wordTables(k)(y >>> 5) >>> (y & 31)) & 1) != 0
        then count += 1
        j += 1
      i += 1
    count

  def main(args: Array[String]): Unit =
    val first = args.headOption.flatMap(_.toLongOption).getOrElse(999_000_001L)
    val count = args.drop(1).headOption.flatMap(_.toIntOption).getOrElse(3000)
    val b = block(first, count)
    val (s, v, t, w) = (scalar(b), vector(b), bits(b), vectorBits(b))
    println(
      s"$count edges from $first: ${b.legs.length} legs, $s pairs through; all four agree: ${Set(s, v, t, w).size == 1}"
    )
    require(Set(s, v, t, w).size == 1, "the filters disagree")
    for _ <- 1 to 5 do
      scalar(b): Unit
      vector(b): Unit
      bits(b): Unit
      vectorBits(b): Unit
    val rounds = 11
    def median(xs: Seq[Double]) = xs.sorted.apply(xs.size / 2)
    val search = median((1 to rounds).map(_ => time(bits(b)).ms))
    val plain = median((1 to rounds).map(_ => time(scalar(b)).ms))
    val lanes = median((1 to rounds).map(_ => time(vector(b)).ms))
    println(f"the filters as the search does them, bit tables: $search%8.1f ms")
    println(f"the filters, a plain loop on int tables:         $plain%8.1f ms")
    val laneBits = median((1 to rounds).map(_ => time(vectorBits(b)).ms))
    println(f"the filters, $Lanes lanes on int tables:   $lanes%8.1f ms (${search / lanes}%.2f× the search's loop)")
    println(
      f"the filters, $Lanes lanes on bit tables:   $laneBits%8.1f ms (${search / laneBits}%.2f× the search's loop)"
    )
