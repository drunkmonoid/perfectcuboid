// A guided tour, part 4: monoids.
// Run it with:  scala-cli run docs/tour/tour4_monoids.sc
//> using scala 3.9.0
//> using dep org.typelevel::cats-core::2.13.0
//> using file ../../core/src/Arith.scala ../../core/src/Lengths.scala ../../core/src/Wide.scala ../../core/src/Cuboid.scala
//> using file ../../core/src/Box.scala ../../core/src/NearMisses.scala ../../core/src/Pythagorean.scala
//> using file ../../core/src/NumberTheory.scala ../../core/src/Montgomery.scala ../../core/src/PollardRho.scala
//> using file ../../core/src/Search.scala ../../cli/src/Summary.scala

import cats.syntax.all.*
import perfectcuboid.*

// A monoid is a way of combining things, `|+|`, with an empty thing that changes nothing.
// Numbers under + are one (empty: 0), lists under ++ another (empty: Nil), and cats knows
// many more, like maps that add up the values of matching keys:
println(1 |+| 2)
println(List(1, 2) |+| List(3))
println(Map("a" -> 1, "b" -> 2) |+| Map("b" -> 10))

// The project's `Summary` of a search is a monoid too: counts add up, the larger find wins.
val bricks = Search.allEulerBricks.take(200).toList
val (firstHalf, secondHalf) = bricks.splitAt(100)
val together = Summary.of(bricks)
val inPieces = Summary.of(firstHalf) |+| Summary.of(secondHalf)
println(together.describe("Euler bricks").mkString("\n"))

// Because |+| does not care how things are grouped, the two halves could have been
// summarised at the same time, on two cores, and combined afterwards. Same answer:
println(s"the same either way? ${together == inPieces}")

// The laws that make that safe (grouping and order don't matter, empty changes nothing) are
// checked on random summaries in test/LawsTest.scala.

// Next: tour5_effects.sc, programs that do things.
