// A guided tour, part 5: effects.
// Run it with:  scala-cli run docs/tour/tour5_effects.sc
//> using scala 3.9.0
//> using dep org.typelevel::cats-effect::3.7.1
//> using file ../../core/src/Arith.scala ../../core/src/Lengths.scala ../../core/src/Wide.scala ../../core/src/Cuboid.scala
//> using file ../../core/src/Box.scala ../../core/src/NearMisses.scala ../../core/src/Pythagorean.scala
//> using file ../../core/src/NumberTheory.scala ../../core/src/Montgomery.scala ../../core/src/PollardRho.scala
//> using file ../../core/src/Search.scala ../../core/src/Sieve.scala ../../core/src/FastSearch.scala ../../core/src/Platform.scala

import cats.effect.{IO, Ref}
import cats.effect.unsafe.implicits.global // the runtime that runs IO values, for a worksheet
import cats.syntax.all.*
import perfectcuboid.*

// Everything so far was pure: same input, same output, nothing else happens. Printing,
// reading files, the clock, threads: those are effects. Cats Effect describes them as values
// of type IO. Building an IO does nothing at all...
val hello: IO[Unit] = IO.println("hello from an IO")
println("the IO is built, and nothing has been printed yet")

// ...until it is run. A real program hands its IO to an IOApp (cli/src/Main.scala); a worksheet
// can run one directly.
hello.unsafeRunSync()
hello.unsafeRunSync() // a description can be run as many times as you like

// Because they are values, IOs combine: one after another with `>>` or `flatMap`, or side by
// side. `parTraverse` searches four blocks of edges at the same time, one per core, and gives
// the results back in order. `Ref` is a variable that is safe to change from several threads.
val blocks = List((1L, 2500L), (2501L, 5000L), (5001L, 7500L), (7501L, 10000L))
val program: IO[Unit] =
  for
    found <- Ref.of[IO, Int](0)
    perBlock <- blocks.parTraverse: (first, last) =>
      val bricks = FastSearch.eulerBricksBetween(first, last)
      found.update(_ + bricks.size).as(s"$first to $last: ${bricks.size} bricks")
    total <- found.get
    _ <- perBlock.traverse_(IO.println)
    _ <- IO.println(s"$total Euler bricks with smallest edge up to 10000, none of them perfect")
  yield ()
program.unsafeRunSync()

// The hunt in cli/src/Hunt.scala is this idea grown up: blocks searched in parallel by fs2
// streams, written to a file with checkpoints, stopped cleanly by Ctrl+C, resumed later.
// Part 6 looks at numbers too big for a Long, part 7 at the hunt's file, part 8 at platforms.
