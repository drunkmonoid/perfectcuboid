# perfectcuboid

[![CI](https://github.com/drunkmonoid/perfectcuboid/actions/workflows/ci.yml/badge.svg)](https://github.com/drunkmonoid/perfectcuboid/actions/workflows/ci.yml)

Playing around with the [perfect cuboid](https://en.wikipedia.org/wiki/Euler_brick#Perfect_cuboid)
problem in plain, purely functional Scala 3: is there a box whose edges, face diagonals and
space diagonal are all whole numbers? Nobody knows.

See [RESULTS.md](RESULTS.md) for what the searches have found, and [ROADMAP.md](ROADMAP.md)
for how the project was built up.

![How the number of Euler bricks grows](docs/growth.svg)

![The primitive Euler bricks, by smallest and longest edge](docs/bricks.svg)

[`docs/check/index.html`](docs/check/index.html) is a page that checks any box in the browser: the
same Scala, compiled to JavaScript by Scala.js. Open it from a clone of the repository.

Needs only [scala-cli](https://scala-cli.virtuslab.org) (the `scala` command since Scala 3.5).

```sh
scala-cli run . -- --help               # every command, its arguments and defaults
scala-cli run .                         # list Euler bricks with edges up to 1000
scala-cli run . -- bricks 100000        # ... or up to another limit
scala-cli run . -- unbounded 10000000   # by smallest edge, other edges unbounded, flat memory
scala-cli run . -- bench 1e6 --runs 3   # time the readable, fast and parallel searches
scala-cli run . -- stats 7              # how the number of Euler bricks grows, up to 10^7
scala-cli run . -- pictures docs 7      # the two charts above, as SVG files
scala-cli run . -- hunt hunt-bricks.txt 1e9  # long search to a file: Ctrl+C stops, rerun resumes
scala-cli run . -- hunt hunt-face.txt 1e6 --kind face  # ... or of face cuboids, or edge cuboids (--kind edge)
scala-cli run . -- verify hunt-bricks.txt  # check a hunt's file: every find again with BigInt, and a few blocks
scala-cli run . -- rules 100000         # check the known Euler brick rules on them
scala-cli run . -- local                # prove such rules by trying every remainder mod p^k
scala-cli run . -- derived 1e6           # primitive bricks paired with their derived bricks ab × ac × bc
scala-cli run . -- curve 44 117 240     # new bricks from points on elliptic curves
scala-cli run . -- origins 1e6          # which primitive bricks come from formulas, and which only from searching
scala-cli run . -- graph 1e5            # the graph joining x and y when x² + y² is a square
scala-cli run . -- fourd 1e5            # four-dimensional Euler bricks, and those missing one pair
scala-cli run . -- closest 1e6          # the bricks whose space diagonal is nearest a whole number
scala-cli run . -- saunderson 10000000  # Saunderson's bricks, built from a formula
scala-cli run . -- legs 240             # factorise 240 and list every leg it pairs with
scala-cli run . -- legs 3825123056546413051  # ... any number up to 2⁶³ − 1
scala-cli run . -- check 44 117 240     # the seven lengths of any box, of any size
scala-cli run . -- nearmisses           # the first body, face and edge cuboid
scala-cli run . -- face 1000            # face cuboids: one face diagonal irrational
scala-cli run . -- edge 3000000         # edge cuboids: one edge irrational
scala-cli test .                        # run the tests
UPDATE_GOLDEN=1 scala-cli test .        # accept changed command output into test/golden
scala-cli fmt .                         # format the code (CI checks it)
scala-cli repl .                        # REPL with the project loaded
scala-cli setup-ide .                   # set up Metals / VSCode
scala-cli test web                      # the web page's tests, on Node
scala-cli --power package web --js --js-mode release -o docs/check/main.js -f  # rebuild the page's JavaScript
scala-cli --power package . --native --native-mode release-fast -o perfectcuboid  # native binary (needs clang)
```

The native binary suits short commands and single-threaded hunts: see [RESULTS.md](RESULTS.md#scala-native-long-runs-work-on-one-thread).

A hunt writes each block of edges followed by a checkpoint line, so stopping it at any
moment loses nothing; just don't run two hunts on the same file at once.

| File | What it holds |
| --- | --- |
| `src/Arith.scala` | exact integer square roots, gcd, overflow-checked squares |
| `src/Wide.scala` | 128-bit whole numbers in two Longs: exact squares and square roots beyond a `Long` |
| `src/Lengths.scala` | `Edge` and `Diagonal` as opaque types, and `CuboidError` |
| `src/Box.scala` | the cuboid checks written once for any whole-number type; `BigCuboid` for boxes of any size; derived bricks |
| `src/Cuboid.scala` | the `Cuboid` type, its diagonals, and how close it is to perfect |
| `src/Montgomery.scala` | a·b mod m for any 64-bit numbers, without overflow, by Montgomery's trick |
| `src/PollardRho.scala` | Pollard's rho method with Brent's improvements, for large factors |
| `src/NumberTheory.scala` | primes (a sieve, and Miller–Rabin for any `Long`), factorisation and divisors |
| `src/Pythagorean.scala` | Pythagorean triples via Euclid's formula, and the legs paired with any given leg |
| `src/Search.scala` | bounded Euler brick search, its brute-force check, and the unbounded `LazyList` search |
| `src/Counting.scala` | how many triples a number is a leg or hypotenuse of, from its factorisation |
| `src/Families.scala` | the Berggren tree of all primitive triples, Saunderson's bricks |
| `src/Sieve.scala` | factorising a block of consecutive numbers at once, by a segmented sieve |
| `src/FastSearch.scala` | the unbounded search with arrays and while loops, and in parallel |
| `src/NearMisses.scala` | `SquaredCuboid` for irrational edges, face and edge cuboid searches |
| `src/Theory.scala` | known divisibility and parity rules for Euler bricks |
| `src/Summary.scala` | a search's results in a few numbers, as a monoid, so summaries of separate blocks add up |
| `src/Elliptic.scala` | exact fractions, and the curves y² = x(x + a²)(x + b²) whose points are Euler bricks |
| `src/Origins.scala` | telling Saunderson bricks, derived bricks and curve multiples apart from the rest |
| `src/Graph.scala` | the Pythagorean graph: degrees, connected pieces, and its triangles, the Euler bricks |
| `src/FourD.scala` | four edges with all six pairs whole, and with five of six |
| `src/ClosestCalls.scala` | how far a brick's space diagonal is from a whole number, accurately |
| `src/Local.scala` | divisibility rules for Euler bricks and perfect cuboids, proved by trying every remainder |
| `src/Charts.scala` | SVG line and scatter charts with log axes, written as plain text |
| `src/Pictures.scala` | the charts in `docs/`, from search results |
| `src/Stats.scala` | counts and growth rates for the write-up |
| `src/Quarry.scala` | the kinds of cuboid a hunt can look for: how to search, write and read each |
| `src/Verify.scala` | checking a results file without trusting the code that wrote it |
| `src/Eta.scala` | progress lines for long searches: share done, recent speed, time left |
| `src/Hunt.scala` | long searches with Cats Effect and fs2: blocks searched in parallel as a stream of chunks, results file, checkpoints, resume |
| `src/CommandLine.scala` | the commands, and how the command line is parsed into one |
| `src/Platform.scala` | what differs on Scala Native: one search thread by default |
| `src/Bench.scala` | timing the searches: warm-up, repeated runs, median and range |
| `src/Main.scala` | the entry point, an `IOApp`, and the pure `Cli.run` it prints |
