// A guided tour of this project, part 1: numbers.
// Run it with:  scala-cli run docs/tour/tour1_numbers.sc
//
// A worksheet is a Scala file that runs from top to bottom, like a script. This one uses the
// project's library like any other dependency, from the local repository on this machine,
// which `./mill __.publishLocal` fills: run that once first, and again after changing the code.
//> using scala 3.9.0
//> using repository ivy2Local
//> using dep io.github.drunkmonoid::perfectcuboid-core::3.0.0-SNAPSHOT

import perfectcuboid.*

// `val` names a value that never changes. Scala works out the type, here Long, a 64-bit
// whole number. There is no `var` in this tour: values are never changed, only new ones made.
val big = 4_000_000_000L
println(s"big = $big")

// 64 bits run out. Squaring 4·10⁹ goes past Long.MaxValue and silently wraps round to a
// negative number. That is why the project squares with `Arith.square`, which refuses.
println(s"big * big wraps round to ${big * big}")
println(s"Arith.square(big) throws: ${scala.util.Try(Arith.square(big))}")

// An exact square root: `Option` says "a Long, or nothing". There is no null.
println(s"exactSqrt(73225) = ${Arith.exactSqrt(73225)}") // 270.6…, not whole: None
println(s"exactSqrt(15625) = ${Arith.exactSqrt(15625)}") // 125² = 15625: Some(125)

// So 44² + 117² is a square: 44 and 117 are the legs of a right triangle with hypotenuse 125.
println(s"44² + 117² = ${Arith.sumOfSquares(44, 117)}, root ${Arith.exactSqrt(Arith.sumOfSquares(44, 117))}")

// An Edge is a Long that has been checked: `Edge.from` returns `Either`, Right(edge) when the
// number is fine, Left(reason) when it is not. The caller has to look at both cases.
for n <- List(240L, 0L, Long.MaxValue) do
  Edge.from(n) match
    case Right(edge) => println(s"$n is a fine edge: $edge")
    case Left(error) => println(s"$n is not an edge: ${error.message}")

// Next: tour2_boxes.sc, where three edges make a box.
