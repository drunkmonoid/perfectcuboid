// The pure core of the library and its tests, built for any of the three platforms:
//
//   scala-cli test cross            # the JVM
//   scala-cli test cross --js       # JavaScript, run on Node
//   scala-cli test cross --native   # a native executable (needs clang)
//
// Only the core: no files, no Cats Effect, no threads. Hunts and parallel searches stay on
// the JVM, where the root project tests them; on Scala Native, several threads corrupt
// memory (RESULTS.md). The tests come in through `using file`, which makes them main sources,
// so munit is an ordinary dependency here.
//> using scala 3.9.0
//> using options -deprecation -feature -Werror -Wunused:all -Wvalue-discard -Wnonunit-statement -Wsafe-init -Yexplicit-nulls
//> using file ../src/Arith.scala ../src/Lengths.scala ../src/Wide.scala ../src/Cuboid.scala ../src/Box.scala
//> using file ../src/NumberTheory.scala ../src/Montgomery.scala ../src/PollardRho.scala ../src/Pythagorean.scala
//> using file ../src/NearMisses.scala ../src/Elliptic.scala ../src/Families.scala ../src/Theory.scala
//> using file ../src/Search.scala ../src/Sieve.scala ../src/FastSearch.scala ../src/Platform.scala
//> using file ../src/Parallelepiped.scala ../src/LineFormat.scala ../src/Counting.scala
//> using file ../test/Generators.scala ../test/ArithTest.scala ../test/ArithProperties.scala ../test/WideTest.scala
//> using file ../test/CuboidTest.scala ../test/CuboidProperties.scala ../test/BigCuboidTest.scala
//> using file ../test/NumberTheoryTest.scala ../test/PrimalityTest.scala ../test/FactorizationTest.scala
//> using file ../test/CountingTest.scala ../test/SieveTest.scala ../test/NearMissesTest.scala
//> using file ../test/EllipticTest.scala ../test/DerivedTest.scala ../test/ParallelepipedTest.scala
//> using file ../test/LineFormatTest.scala
// Native tests are built optimised: unoptimised, two of them take longer than munit allows.
//> using nativeMode release-fast
//> using dep org.scalameta::munit::1.3.6
//> using dep org.scalameta::munit-scalacheck::1.3.1
