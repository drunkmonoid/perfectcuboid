package perfectcuboid.gpu

import org.jocl.*
import org.jocl.CL.*
import perfectcuboid.{Cuboid, FastSearch, Sieve, Wide}

/** Phase 81, an experiment: the remainder filters of the search on the laptop's integrated GPU.
  *
  *   ./mill experiments.gpu.run                    # 20,000 edges from 999,000,001
  *   ./mill experiments.gpu.run 3000000001 20000   # or from anywhere, any number of edges
  *
  * The search spends about half its time on the three remainder filters, which rule out 99.7% of
  * the pairs of legs with 32-bit additions and a bit looked up in a table. That much suits a GPU:
  * millions of pairs, each tested on its own. The exact 128-bit test needs 64-bit products and
  * double precision, which this GPU only emulates, so it stays on the CPU, for the few pairs
  * left. The run checks the GPU's pairs and bricks against the CPU's before timing anything.
  */
object GpuFilter:

  /** One work-item per leg i: every longer leg j of the same edge, through the three filters. */
  val kernelSource: String = """
    |inline int passes(int x, int m, __constant const uint* squares) {
    |  if (x >= m) x -= m;
    |  return (squares[x >> 5] >> (x & 31)) & 1;
    |}
    |__kernel void pairs(__global const int* r1, __global const int* r2, __global const int* r3,
    |                    __global const int* end,
    |                    __constant const uint* s1, __constant const uint* s2, __constant const uint* s3,
    |                    const int m1, const int m2, const int m3, const int legs,
    |                    __global int* out, volatile __global int* count, const int capacity) {
    |  int i = get_global_id(0);
    |  if (i >= legs) return;
    |  int a1 = r1[i], a2 = r2[i], a3 = r3[i], e = end[i];
    |  for (int j = i + 1; j < e; j++) {
    |    if (passes(a1 + r1[j], m1, s1) && passes(a2 + r2[j], m2, s2) && passes(a3 + r3[j], m3, s3)) {
    |      int k = atomic_inc(count);
    |      if (k < capacity) { out[2 * k] = i; out[2 * k + 1] = j; }
    |    }
    |  }
    |}
    |""".stripMargin

  /** A block of edges, laid out for the GPU: every leg of every edge one after another, the edge
    * each belongs to, where its edge's legs end, and the remainders of its square.
    */
  final case class Block(
      edges: Array[Long],
      legs: Array[Long],
      edgeOf: Array[Int],
      end: Array[Int],
      r: List[Array[Int]]
  )

  val Moduli: List[Int] = List(FastSearch.FirstModulus, FastSearch.SecondModulus, FastSearch.ThirdModulus)

  def block(first: Long, count: Int): Block =
    val factors = Sieve.factorizeBlock(first, first + count - 1)
    val perEdge = (0 until count).map(k => FastSearch.legsAbove(first + k, factors(k)))
    val legs = perEdge.flatMap(_.toSeq).toArray
    val edgeOf = perEdge.zipWithIndex.flatMap((ls, k) => Seq.fill(ls.length)(k)).toArray
    val ends = perEdge.scanLeft(0)(_ + _.length).tail
    val end = perEdge.zipWithIndex.flatMap((ls, k) => Seq.fill(ls.length)(ends(k))).toArray
    val r = Moduli.map(m => FastSearch.squareRemainders(IArray.from(legs), m))
    Block(Array.tabulate(count)(first + _), legs, edgeOf, end, r)

  /** The pairs (i, j) through the three filters, worked out on the CPU, as the search does. */
  def cpuPairs(b: Block): Vector[(Int, Int)] =
    val tables = Moduli.map(m => FastSearch.squareRemainderBits(m))
    def passes(x: Int, m: Int, bits: IArray[Long]): Boolean =
      val y = if x >= m then x - m else x
      ((bits(y >>> 6) >>> y) & 1) != 0
    val out = Vector.newBuilder[(Int, Int)]
    for i <- b.legs.indices; j <- i + 1 until b.end(i) do
      if Moduli.indices.forall(k => passes(b.r(k)(i) + b.r(k)(j), Moduli(k), tables(k))) then out += ((i, j))
    out.result()

  /** The tables as 32-bit words, as the kernel reads them. */
  def words(m: Int): Array[Int] =
    val bits = FastSearch.squareRemainderBits(m)
    Array.tabulate((m + 31) / 32)(w => ((bits(w / 2) >>> (32 * (w % 2))) & 0xffffffffL).toInt)

  /** The GPU, its program and kernel, made once. */
  final class Gpu:
    setExceptionsEnabled(true)
    private val platforms = new Array[cl_platform_id](1)
    clGetPlatformIDs(1, platforms, null): Unit
    private val devices = new Array[cl_device_id](1)
    clGetDeviceIDs(platforms(0), CL_DEVICE_TYPE_GPU, 1, devices, null): Unit
    val device: cl_device_id = devices(0)
    private val context = clCreateContext(null, 1, devices, null, null, null)
    private val queue = clCreateCommandQueueWithProperties(context, device, null, null)
    private val program = clCreateProgramWithSource(context, 1, Array(kernelSource), null, null)
    clBuildProgram(program, 0, null, null, null, null): Unit
    private val kernel = clCreateKernel(program, "pairs", null)
    private val tables = Moduli.map(m => input(words(m)))

    def name: String =
      val size = new Array[Long](1)
      clGetDeviceInfo(device, CL_DEVICE_NAME, 0, null, size): Unit
      val bytes = new Array[Byte](size(0).toInt)
      clGetDeviceInfo(device, CL_DEVICE_NAME, bytes.length.toLong, Pointer.to(bytes), null): Unit
      new String(bytes, 0, bytes.length - 1, "UTF-8")

    private def input(a: Array[Int]): cl_mem =
      clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, 4L * math.max(1, a.length), Pointer.to(a), null)

    /** The surviving pairs of a block, from the GPU. */
    def pairs(b: Block, capacity: Int): Vector[(Int, Int)] =
      val buffers = (b.r :+ b.end).map(input)
      val out = clCreateBuffer(context, CL_MEM_WRITE_ONLY, 8L * capacity, null, null)
      val count = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, 4L, Pointer.to(Array(0)), null)
      val mems = buffers ++ tables
      mems.zipWithIndex.foreach((mem, k) => clSetKernelArg(kernel, k, Sizeof.cl_mem, Pointer.to(mem)): Unit)
      List(Moduli(0), Moduli(1), Moduli(2), b.legs.length).zipWithIndex.foreach: (v, k) =>
        clSetKernelArg(kernel, 7 + k, Sizeof.cl_int, Pointer.to(Array(v))): Unit
      clSetKernelArg(kernel, 11, Sizeof.cl_mem, Pointer.to(out)): Unit
      clSetKernelArg(kernel, 12, Sizeof.cl_mem, Pointer.to(count)): Unit
      clSetKernelArg(kernel, 13, Sizeof.cl_int, Pointer.to(Array(capacity))): Unit
      val global = Array(((b.legs.length + 63) / 64 * 64).toLong)
      clEnqueueNDRangeKernel(queue, kernel, 1, null, global, Array(64L), 0, null, null): Unit
      val found = new Array[Int](1)
      clEnqueueReadBuffer(queue, count, CL_TRUE, 0, 4L, Pointer.to(found), 0, null, null): Unit
      val n = math.min(found(0), capacity)
      val pairs = new Array[Int](2 * math.max(1, n))
      if n > 0 then clEnqueueReadBuffer(queue, out, CL_TRUE, 0, 8L * n, Pointer.to(pairs), 0, null, null): Unit
      (buffers :+ out :+ count).foreach(clReleaseMemObject(_): Unit)
      require(found(0) <= capacity, s"${found(0)} pairs, more than the room for $capacity")
      Vector.tabulate(n)(k => (pairs(2 * k), pairs(2 * k + 1))).sorted

  /** The bricks from the surviving pairs, by the exact test. */
  def bricks(b: Block, pairs: Vector[(Int, Int)]): List[Cuboid] =
    pairs.toList.flatMap: (i, j) =>
      if Wide.isSumOfSquaresSquare(b.legs(i), b.legs(j)) then
        Cuboid.of(b.edges(b.edgeOf(i)), b.legs(i), b.legs(j)).toList
      else Nil

  private def time[A](work: => A): (A, Double) =
    val start = System.nanoTime
    val result = work
    (result, (System.nanoTime - start) / 1e6)

  private def median(xs: Seq[Double]): Double = xs.sorted.apply(xs.size / 2)

  def main(args: Array[String]): Unit =
    val first = args.headOption.flatMap(_.toLongOption).getOrElse(999_000_001L)
    val count = args.drop(1).headOption.flatMap(_.toIntOption).getOrElse(20_000)
    val gpu = Gpu()
    println(s"${gpu.name}: ${Moduli.mkString(", ")} as filters, edges from $first, $count of them")
    val b = block(first, count)
    val expectedPairs = cpuPairs(b)
    val capacity = expectedPairs.size * 2 + 1024
    val gpuPairs = gpu.pairs(b, capacity)
    val sameBricks = bricks(b, gpuPairs).sorted == FastSearch.eulerBricksBetween(first, first + count - 1).sorted
    println(
      s"${b.legs.length} legs, ${b.legs.indices.map(i => b.end(i) - i - 1).sum.toLong} pairs, " +
        s"${expectedPairs.size} through the filters; the GPU's pairs are the CPU's: ${gpuPairs == expectedPairs}; " +
        s"its bricks are the search's: $sameBricks"
    )
    require(gpuPairs == expectedPairs && sameBricks, "the GPU disagrees with the CPU")
    // Warm up both, then time each step, many times over.
    for _ <- 1 to 3 do
      gpu.pairs(b, capacity): Unit
      cpuPairs(b): Unit
      FastSearch.eulerBricksBetween(first, first + count - 1): Unit
    val rounds = 9
    val search = median((1 to rounds).map(_ => time(FastSearch.eulerBricksBetween(first, first + count - 1))._2))
    val prepare = median((1 to rounds).map(_ => time(block(first, count))._2))
    val filterCpu = median((1 to rounds).map(_ => time(cpuPairs(b))._2))
    val filterGpu = median((1 to rounds).map(_ => time(gpu.pairs(b, capacity))._2))
    val exact = median((1 to rounds).map(_ => time(bricks(b, gpuPairs))._2))
    println(f"one CPU thread, the whole search:      $search%8.1f ms")
    println(f"  legs and remainders, laid out:        $prepare%8.1f ms")
    println(f"  the filters on the CPU (a plain loop): $filterCpu%8.1f ms")
    println(f"  the filters on the GPU, with copying:  $filterGpu%8.1f ms")
    println(f"  the exact test of what is left:        $exact%8.1f ms")
    println(
      f"with the GPU filtering: ${prepare + filterGpu + exact}%.1f ms, ${search / (prepare + filterGpu + exact)}%.2f× the search"
    )
