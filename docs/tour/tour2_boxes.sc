// A guided tour, part 2: boxes.
// Run it with:  scala-cli run docs/tour/tour2_boxes.sc
//> using scala 3.9.0
//> using repository ivy2Local
//> using dep io.github.drunkmonoid::perfectcuboid-core::3.0.0-SNAPSHOT

import perfectcuboid.*

// A box has three edges, three face diagonals and a space diagonal: seven lengths. A perfect
// cuboid would have all seven whole. `Cuboid.of` sorts the edges and checks them as the program
// runs; it gives an `Option`, empty when an edge is not positive.
println(Cuboid.of(240, 0, 44)) // None
// Edges written as literals are checked before that, by the compiler: `Cuboid(240, 0, 44)` would
// not compile at all. (`Cuboid.apply` is an `inline` method, run as the code is compiled.)
val brick = Cuboid(240, 117, 44)
println(s"$brick")
println(s"face diagonals: ${brick.faceDiagonals}")
println(s"space diagonal: ${brick.spaceDiagonal}") // None: √73225 is not whole
println(s"an Euler brick? ${brick.isEuler}   perfect? ${brick.isPerfect}")

// `Kind` is an enum: a fixed list of cases. `match` must handle all of them, or the compiler
// says which one is missing.
def describe(kind: Kind): String = kind match
  case Kind.Perfect => "perfect!"
  case Kind.Body    => "an Euler brick: only the space diagonal is irrational"
  case Kind.Face    => "a face cuboid: one face diagonal is irrational"
  case Kind.Edge    => "an edge cuboid: one edge is irrational"
  case Kind.Other   => "nothing special"

for edges <- List((44L, 117L, 240L), (104L, 153L, 672L), (1L, 2L, 3L)) do
  val box = Cuboid.of(edges._1, edges._2, edges._3).get
  println(s"${box.a} × ${box.b} × ${box.c}: ${describe(box.kind)}")

// Case classes compare by their contents, so the same edges in any order are the same box.
println(s"same box? ${Cuboid.of(44, 117, 240) == Cuboid.of(240, 44, 117)}")

// A `given Ordering[Cuboid]` in the project tells `sorted` how to order boxes: by longest edge.
val boxes = List(Cuboid.of(85, 132, 720), Cuboid.of(44, 117, 240), Cuboid.of(140, 480, 693)).flatten
println(s"sorted: ${boxes.sorted.map(b => s"${b.a}×${b.b}×${b.c}")}")

// Scaling an Euler brick keeps it one, even far beyond a Long: BigCuboid has BigInt edges.
val huge = BigInt(10).pow(40)
val bigBrick = BigCuboid.of(44 * huge, 117 * huge, 240 * huge).get
println(s"44·10⁴⁰ × 117·10⁴⁰ × 240·10⁴⁰ is an Euler brick? ${bigBrick.isEuler}")

// Next: tour3_searching.sc, finding bricks instead of being given them.
