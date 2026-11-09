// A guided tour, part 6: numbers bigger than a Long.
// Run it with:  scala-cli run docs/tour/tour6_wide.sc
//> using scala 3.9.0
//> using file ../../src/Arith.scala ../../src/Lengths.scala ../../src/Wide.scala ../../src/Cuboid.scala
//> using file ../../src/Box.scala ../../src/NearMisses.scala ../../src/Pythagorean.scala
//> using file ../../src/NumberTheory.scala ../../src/Montgomery.scala ../../src/PollardRho.scala

import perfectcuboid.*

// A Long holds whole numbers up to 9,223,372,036,854,775,807, about 9.2·10¹⁸. The edges of the
// bricks found reach 3.8·10¹³, and the search must ask whether b² + c² is a square: ~1.4·10²⁷.
val c = 37_650_741_152_640L // the longest edge of a brick from the 10⁹ hunt
println(s"c * c as a Long: ${c * c}") // overflows: the result is nonsense, and nothing warns
println(s"c * c as a BigInt: ${BigInt(c) * c}")

// BigInt is exact but slow: it lives on the heap and allocates for every operation. Wide holds a
// number up to 2¹²⁶ in two Longs, hi·2⁶⁴ + lo, and squares a Long with two multiplications: the
// low 64 bits by ordinary (overflowing) multiplication, the high 64 by Math.multiplyHigh.
val square = Wide.square(c)
println(s"Wide: hi = ${square.hi}, lo = ${square.lo} (as unsigned), which is ${square.toBigInt}")
assert(square.toBigInt == BigInt(c) * c)

// Is b² + c² a square? The search asks this billions of times. The trick (phase 43): convert the
// sum to a Double and take its square root. A Double holds 53 bits, so this root is off, but
// for roots below 2⁵¹ by less than a quarter: rounding it gives the true root of any square
// exactly. Then one exact multiplication says yes or no. The answer is never a guess.
val (b2, c2) = (308_566_834_432L, 2_489_112_220_560L) // the other edges of a brick near 10⁹
println(s"is 999041670² + 308566834432² a square? ${Wide.isSumOfSquaresSquare(999_041_670L, b2)}")
println(s"is 308566834432² + 2489112220560² a square? ${Wide.isSumOfSquaresSquare(b2, c2)}")

// Even cheaper is not to ask. If b² + c² leaves a remainder mod 11 that no square leaves, it is
// not a square. These are the remainders of squares mod 11:
println(s"squares mod 11: ${(0 until 11).map(x => x * x % 11).distinct.sorted.mkString(", ")}")
// Six of eleven remainders: for random numbers, half pass. FastSearch tests three moduli, each a
// product of three such primes, 11·43·59, 19·53·37 and 13·47·29, and only 1 pair of legs in 300
// reaches the exact test. Which primes? Measured, not guessed: 7, 17 and 41, and the powers of
// 2, 3 and 5, let most pairs of real legs through, because legs that pair with the same edge
// share factors. RESULTS.md, "A sharper filter", has the numbers.
