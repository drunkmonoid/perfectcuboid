package perfectcuboid

/** What differs between running on the JVM and as a Scala Native executable. */
object Platform:

  /** Scala Native reports itself as the virtual machine, though it has none. */
  val isNative: Boolean = Option(System.getProperty("java.vm.name")).exists(_.contains("Native"))

  /** How many threads to search on by default: every core on the JVM, but one on Scala Native,
    * where a garbage collection while several threads are working corrupts memory in optimised
    * builds (RESULTS.md has the experiments). The search's results can then be silently wrong,
    * not only crash, so native runs keep to one thread.
    */
  val defaultThreads: Int = if isNative then 1 else Runtime.getRuntime.availableProcessors

  /** Whether a search may run on this many threads here. */
  def threadsAllowed(threads: Int, native: Boolean = isNative): Boolean = threads == 1 || (threads > 1 && !native)

  val tooManyThreads: String =
    "--threads must be 1 on Scala Native, where several threads can corrupt memory; run several processes instead"
