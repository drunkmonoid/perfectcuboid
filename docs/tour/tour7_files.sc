// A guided tour, part 7: a long search that can stop at any moment.
// Run it with:  scala-cli run docs/tour/tour7_files.sc
//> using scala 3.9.0
//> using repository ivy2Local
//> using dep io.github.drunkmonoid::perfectcuboid-cli::3.0.0-SNAPSHOT

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.io.file.{Files, Path}
import scala.concurrent.duration.*
import perfectcuboid.*

// A results file is plain text: each brick on a line, and after every block of edges a
// checkpoint saying how far the search has got. The line format is derived from a case class
// (core/src/LineFormat.scala): `EdgesLine(a, b, c) derives LineFormat` writes and reads "a b c".
println(LineFormat.write(EdgesLine(44, 117, 240)))
println(LineFormat.parse[EdgesLine]("85 132 720"))

// A hunt, cut off after a second and a half as if by Ctrl+C. `timeoutTo` cancels the IO, and the
// file is closed cleanly: only whole blocks with their checkpoints are in it.
val dir = Files[IO].createTempDirectory.unsafeRunSync()
val file = dir / "bricks.txt"
Hunt.run(Quarry.eulerBricks, file, 200_000, blockSize = 1000).timeoutTo(1500.millis, IO.unit).unsafeRunSync()
val stopped = Hunt.readResume(Quarry.eulerBricks, file).unsafeRunSync()
println(s"stopped after edge ${stopped.searchedUpTo}, with ${stopped.found.found} bricks so far")

// The same command again carries on from the last checkpoint.
Hunt.run(Quarry.eulerBricks, file, 200_000, blockSize = 1000).unsafeRunSync()

// verify checks the file without trusting the code that wrote it: every brick again in BigInt
// arithmetic, the checkpoints in order, and a few blocks searched again.
Verify.run(Quarry.eulerBricks, file, blocks = 3).unsafeRunSync().foreach(println)

// One range can be split into shards, each with its own file and starting edge, searched at the
// same time by separate processes or machines, and joined afterwards.
val (low, high, joined) = (dir / "low.txt", dir / "high.txt", dir / "joined.txt")
Hunt.run(Quarry.eulerBricks, low, 100_000, blockSize = 1000).unsafeRunSync()
Hunt.run(Quarry.eulerBricks, high, 200_000, blockSize = 1000, from = Some(100_001)).unsafeRunSync()
Merge.run(Quarry.eulerBricks, joined, List(high, low)).unsafeRunSync().foreach(println)
val same =
  Files[IO].readUtf8(joined).compile.string.unsafeRunSync() == Files[IO].readUtf8(file).compile.string.unsafeRunSync()
println(s"the two shards, joined, hold the same bricks as the hunt in one file: $same")
Files[IO].deleteRecursively(dir).unsafeRunSync()
