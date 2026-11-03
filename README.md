# perfectcuboid

[![CI](https://github.com/drunkmonoid/perfectcuboid/actions/workflows/ci.yml/badge.svg)](https://github.com/drunkmonoid/perfectcuboid/actions/workflows/ci.yml)

Playing around with the [perfect cuboid](https://en.wikipedia.org/wiki/Euler_brick#Perfect_cuboid)
problem in plain, purely functional Scala 3: is there a box whose edges, face diagonals and
space diagonal are all whole numbers? Nobody knows.

See [RESULTS.md](RESULTS.md) for what the searches have found, and [ROADMAP.md](ROADMAP.md)
for how the project was built up.

Needs only [scala-cli](https://scala-cli.virtuslab.org) (the `scala` command since Scala 3.5).

```sh
scala-cli run . -- --help               # every command, its arguments and defaults
scala-cli run .                         # list Euler bricks with edges up to 1000
scala-cli run . -- bricks 100000        # ... or up to another limit
scala-cli run . -- unbounded 10000000   # by smallest edge, other edges unbounded, flat memory
scala-cli run . -- bench 1e6 --runs 3   # time the readable, fast and parallel searches
scala-cli run . -- stats 7              # how the number of Euler bricks grows, up to 10^7
scala-cli run . -- hunt hunt-bricks.txt 1e9  # long search to a file: Ctrl+C stops, rerun resumes
scala-cli run . -- rules 100000         # check the known Euler brick rules on them
scala-cli run . -- local                # prove such rules by trying every remainder mod p^k
scala-cli run . -- derived 1e6           # primitive bricks paired with their derived bricks ab × ac × bc
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
scala-cli --power package . --native --native-mode release-fast -o perfectcuboid  # native binary (needs clang)
```

The native binary suits short commands only: see [RESULTS.md](RESULTS.md#scala-native-works-for-short-commands-not-for-long-ones).

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
| `src/Local.scala` | divisibility rules for Euler bricks and perfect cuboids, proved by trying every remainder |
| `src/Stats.scala` | counts and growth rates for the write-up |
| `src/Hunt.scala` | long searches with Cats Effect and fs2: results file, checkpoints, resume |
| `src/CommandLine.scala` | the commands, and how the command line is parsed into one |
| `src/Bench.scala` | timing the searches: warm-up, repeated runs, median and range |
| `src/Main.scala` | the entry point, an `IOApp`, and the pure `Cli.run` it prints |
