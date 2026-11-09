package perfectcuboid

/** Each platform says what it is: one search thread by default on Scala Native, every core
  * on the JVM, and on JavaScript, which has one thread, one.
  */
class PlatformTest extends munit.FunSuite:

  test("the default number of threads suits the platform"):
    val expected = if Platform.isNative then 1 else Runtime.getRuntime.availableProcessors
    assertEquals(Platform.defaultThreads, expected)
    assert(Platform.threadsAllowed(1))
