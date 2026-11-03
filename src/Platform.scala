package perfectcuboid

/** What differs between running on the JVM and as a Scala Native executable. */
object Platform:

  /** Scala Native reports itself as the virtual machine, though it has none. */
  val isNative: Boolean = Option(System.getProperty("java.vm.name")).exists(_.contains("Native"))

  /** How many threads to search on by default: every core on the JVM, but one on Scala Native,
    * where allocating on several threads at once corrupts memory (see RESULTS.md).
    */
  val defaultThreads: Int = if isNative then 1 else Runtime.getRuntime.availableProcessors
