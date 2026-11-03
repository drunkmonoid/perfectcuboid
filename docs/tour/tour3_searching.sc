// A guided tour, part 3: searching.
// Run it with:  scala-cli run docs/tour/tour3_searching.sc
//> using scala 3.9.0
//> using file ../../src/Arith.scala ../../src/Lengths.scala ../../src/Wide.scala ../../src/Cuboid.scala
//> using file ../../src/Box.scala ../../src/NearMisses.scala ../../src/Pythagorean.scala
//> using file ../../src/NumberTheory.scala ../../src/Montgomery.scala ../../src/PollardRho.scala
//> using file ../../src/Search.scala

import perfectcuboid.*

// In an Euler brick a × b × c, each pair of edges are the legs of a right triangle. So start
// from one edge, a, and list every leg that pairs with it. There are only finitely many,
// and the factors of a² give them all.
val a = 240L
println(s"$a = ${NumberTheory.factorize(a)}")
val legs = Pythagorean.legsPairedWith(a)
println(s"${legs.size} legs pair with $a, the first few: ${legs.take(8)}")

// Two of those legs, b and c, make a brick with a exactly when they pair with each other too.
// A for-comprehension reads like the sentence: for each b, for each larger c, if b² + c² is a
// square, keep a × b × c.
val bricks =
  for
    b <- legs
    c <- legs
    if a < b && b < c && Arith.isPerfectSquare(b * b + c * c)
  yield (a, b, c)
println(s"bricks with smallest edge $a: $bricks")

// The same, for every smallest edge 1, 2, 3, … : an infinite list. A LazyList only works out
// the elements that are asked for, so `take(5)` does five bricks' worth of work.
println("the first five Euler bricks by smallest edge:")
Search.allEulerBricks.take(5).foreach(b => println(s"  ${b.a} × ${b.b} × ${b.c}"))

// And the question itself: is any of them perfect? Not among the first thousand.
val firstThousand = Search.allEulerBricks.take(1000).toList
println(s"perfect among the first ${firstThousand.size}: ${firstThousand.count(_.isPerfect)}")

// Next: tour4_monoids.sc, adding up what a search finds.
