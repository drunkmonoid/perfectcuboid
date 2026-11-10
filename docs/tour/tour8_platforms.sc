// A guided tour, part 8: one program, three platforms.
// Run it with:  scala-cli run docs/tour/tour8_platforms.sc
//         or:   scala-cli run docs/tour/tour8_platforms.sc --js       (needs Node)
//         or:   scala-cli run docs/tour/tour8_platforms.sc --native   (needs clang)
//> using scala 3.9.0
//> using file ../../core/src/Arith.scala ../../core/src/Lengths.scala ../../core/src/Wide.scala ../../core/src/Cuboid.scala
//> using file ../../core/src/Box.scala ../../core/src/NumberTheory.scala ../../core/src/Montgomery.scala ../../core/src/NearMisses.scala
//> using file ../../core/src/PollardRho.scala ../../core/src/Pythagorean.scala ../../core/src/Search.scala
//> using file ../../core/src/Sieve.scala ../../core/src/FastSearch.scala ../../core/src/Platform.scala

import perfectcuboid.*

// Scala compiles to JVM bytecode, to JavaScript (Scala.js) and to native machine code (Scala
// Native). The pure code of this project is the same on all three: run this file three ways.
val vm = Option(System.getProperty("java.vm.name")).getOrElse("(none: this is JavaScript)")
println(s"running on: $vm; Platform.isNative = ${Platform.isNative}")

// The arithmetic must agree everywhere. On JavaScript, numbers are Doubles, so Scala.js emulates
// every Long in software, slower but exact; Wide's 128-bit squares give the same digits.
println(s"2⁶³ − 1 = ${Long.MaxValue}; 999999937² = ${Wide.square(999_999_937L).toBigInt}")

// The search itself, a block at a time, and how fast this platform does it.
val start = System.nanoTime
val bricks = FastSearch.eulerBricksBetween(1, 20_000)
val seconds = (System.nanoTime - start) / 1e9
println(
  f"${bricks.size} Euler bricks with smallest edge up to 20000, ${bricks.count(_.isPrimitive)} primitive, in $seconds%.2f s"
)

// What differs is threads. The JVM has many, and the hunt searches on every core. JavaScript has
// one per page, so the browser page in docs/check searches in a web worker, a second JavaScript
// thread with no page, which talks to the page by messages. Scala Native has threads, but in
// optimised builds a garbage collection while several run can corrupt memory (RESULTS.md,
// "Why several threads break native runs"), so native runs keep to one, and use several
// processes instead, each searching its own range, joined afterwards by `merge`.
println(s"threads a search uses here by default: ${Platform.defaultThreads}")
