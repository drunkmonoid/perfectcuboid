# perfectcuboid

[![CI](https://github.com/drunkmonoid/perfectcuboid/actions/workflows/ci.yml/badge.svg)](https://github.com/drunkmonoid/perfectcuboid/actions/workflows/ci.yml)

Looking for a [perfect cuboid](https://en.wikipedia.org/wiki/Euler_brick#Perfect_cuboid) in purely
functional Scala 3: is there a box whose edges, face diagonals and space diagonal are all whole
numbers? Nobody knows.

The biggest search here went through every primitive Euler brick with smallest edge up to 3·10⁹:
15,101 of them, and no perfect cuboid. New to the problem? Read [the essay](docs/essay.md). See
[RESULTS.md](RESULTS.md) for that and the rest of what the searches have found,
[ROADMAP.md](ROADMAP.md) for how the project was built up, [CHANGELOG.md](CHANGELOG.md) for what
each version holds, and [CONTRIBUTING.md](CONTRIBUTING.md) for how to add to it.
New to Scala? Start with the [guided tour](docs/tour/README.md).

![How the number of Euler bricks grows](docs/growth.svg)

![The primitive Euler bricks, by smallest and longest edge](docs/bricks.svg)

[`docs/catalogue`](docs/catalogue/index.html) lists every primitive brick of the 3·10⁹ hunt, sortable and
filterable, each one opening in the checker; its data is [`bricks.txt`](docs/catalogue/bricks.txt),
the hunt's file without its checkpoints (`grep -v '^#' hunt-primitive.txt`).

[`docs/check/index.html`](docs/check/index.html) is a page that checks any box in the browser: the
same Scala, compiled to JavaScript by Scala.js, and it can also run the search itself, in a
background thread of the browser (about 35,000 edges a second). Open it from a clone of the
repository through a local web server (browsers start no background threads from files), or on the
project's site, which also has the charts and the API docs. The site is built by the `Pages`
workflow on every version tag, once GitHub Pages is switched on for the repository (Settings >
Pages > Source: GitHub Actions); it then lives at https://drunkmonoid.github.io/perfectcuboid/.

Take a ready-made program from the [releases](https://github.com/drunkmonoid/perfectcuboid/releases):
a native executable for Linux, or `perfectcuboid.jar` for any Java from 17. Or build the jar
yourself with [Mill](https://mill-build.org): the `./mill` script in the repository downloads
Mill and a Java 17 the first time, so nothing needs installing first.

```sh
./mill cli.jvm.assembly && cp out/cli/jvm/assembly.dest/out.jar perfectcuboid.jar
java -jar perfectcuboid.jar check 44 117 240   # or ./mill java -jar ..., with Mill's own Java
```

Below, `perfectcuboid` stands for `java -jar perfectcuboid.jar`, or the native executable.
`perfectcuboid --help` lists every command with its arguments and defaults. For the big runs,
cap the memory: `java -Xmx2g -jar perfectcuboid.jar ...`.

**Searching**

```sh
perfectcuboid                            # Euler bricks with every edge up to 1000
perfectcuboid bricks 100000              # ... or up to another limit
perfectcuboid unbounded 1e7              # by smallest edge, other edges unbounded, flat memory
perfectcuboid hunt hunt-primitive.txt 3e9 --kind primitive   # long search to a file: Ctrl+C stops, rerun resumes
perfectcuboid hunt hunt-face.txt 1e6 --kind face             # ... of face cuboids (or bricks, or edge)
perfectcuboid hunt hunt-odd.txt 1e7 --kind odd               # ... of primitive bricks by their odd edge
perfectcuboid hunt part2.txt 2e9 --from 1000000001           # one range of a longer search, a shard
perfectcuboid hunt hunt.txt 2e7 --processes 4                # ... split between processes, and merged
perfectcuboid merge all.txt part1.txt part2.txt              # join shards that follow on from each other
perfectcuboid verify hunt-primitive.txt --kind primitive     # check a hunt's file without trusting the search
perfectcuboid analyse hunt-primitive.txt --kind primitive    # counts, rules, origins and closest calls of its bricks
perfectcuboid families hunt-primitive.txt                    # Saunderson's family and its offspring, against the file
perfectcuboid densities hunt-primitive.txt                   # how often small primes divide the edges, against prediction
perfectcuboid records hunt-primitive.txt                     # the edges with most bricks, flattest and squarest bricks
perfectcuboid forecast hunt-primitive.txt 1e10               # what the bricks so far predict for a longer hunt
perfectcuboid misses hunt-primitive.txt                      # where the space diagonals fall between whole numbers
perfectcuboid face 1000                  # face cuboids: one face diagonal irrational
perfectcuboid edge 3000000               # edge cuboids: one edge irrational
perfectcuboid crosscheck 1e5             # face cuboids found two independent ways, compared
perfectcuboid nearcalls hunt-face.txt --kind face            # the face (or edge) cuboids nearest to perfect
perfectcuboid parallelepiped 300         # slanted boxes with all 13 lengths whole
perfectcuboid nearmisses                 # the first body, face and edge cuboid
perfectcuboid bench 1e6 --runs 3         # time the readable, fast and parallel searches
perfectcuboid scaling --chart t.svg      # search speed on 1 thread, 2, ... every core
./mill bench.runJmh                      # microbenchmarks of the kernels, by JMH
```

A hunt writes each block of edges followed by a checkpoint line, so stopping it at any
moment loses nothing; just don't run two hunts on the same file at once. To search one range
on several machines or processes at once, give each its own file and `--from`, then `merge`.

**Mathematics**

```sh
perfectcuboid check 44 117 240           # the seven lengths of any box, of any size
perfectcuboid legs 240                   # factorise a number and list every leg it pairs with
perfectcuboid legs 3825123056546413051   # ... any number up to 2⁶³ − 1
perfectcuboid rules 100000               # check the known Euler brick rules on every brick found
perfectcuboid local                      # prove such rules by trying every remainder mod pᵏ
perfectcuboid saunderson 1e7             # Saunderson's bricks, built from a formula
perfectcuboid derived 1e6                # primitive bricks paired with their derived bricks ab × ac × bc
perfectcuboid curve 44 117 240           # new bricks from points on elliptic curves
perfectcuboid origins 1e6                # which primitive bricks formulas explain, and which only searching finds
perfectcuboid graph 1e5                  # the graph joining x and y when x² + y² is a square
perfectcuboid fourd 1e5                  # four-dimensional Euler bricks, and those missing one pair
perfectcuboid closest 1e6                # the bricks whose space diagonal is nearest a whole number
perfectcuboid stats 7                    # how the number of Euler bricks grows, up to 10⁷
perfectcuboid pictures docs 7            # the two charts above, as SVG files
```

**Working on the code**

```sh
./mill __.test                           # every test: the core on the JVM, JavaScript and Native, the rest on the JVM
./mill core.js.test                      # ... or one module's (core.jvm, core.native, cli.jvm, web)
UPDATE_GOLDEN=1 ./mill cli.jvm.test      # accept changed command output into cli/test/resources/golden
./mill mill.scalalib.scalafmt/           # format the code (CI checks it)
./mill api.docJar                        # the API docs, every public definition described, in out/api/docJar.dest/docs
./mill cli.jvm.console                   # a REPL with the project loaded
./mill mill.bsp.BSP/install              # set up Metals, IntelliJ or VS Code
./mill web.fullLinkJS && cp out/web/fullLinkJS.dest/main.js docs/check/main.js  # rebuild the page
./mill speed.run                         # the search's speed against a fixed reference, as CI checks it
./mill cli.native.nativeLink             # a native executable in out/cli/native/nativeLink.dest/out (needs clang)
```

The build is `build.mill`. `core` is the pure library, built for the JVM, JavaScript and Scala
Native with one set of tests for all three; `cli` is the command line, hunts and reports, on
the JVM (and Native, for the release executable); `web` is the page, `bench` the JMH
benchmarks. The worksheets in `docs/tour` run with [scala-cli](https://scala-cli.virtuslab.org), on
the library as `./mill __.publishLocal` publishes it to this machine.

The native binary suits short commands and single-threaded hunts: see
[RESULTS.md](RESULTS.md#scala-native-long-runs-work-on-one-thread).

| File | What it holds |
| --- | --- |
| `core/src/Arith.scala` | exact integer square roots, gcd, overflow-checked squares |
| `core/src/Wide.scala` | 128-bit whole numbers in two Longs: exact squares and square roots beyond a `Long` |
| `core/src/Lengths.scala` | `Edge` and `Diagonal` as opaque types, and `CuboidError` |
| `core/src/Box.scala` | the cuboid checks written once for any whole-number type; `BigCuboid` for boxes of any size; derived bricks |
| `core/src/Cuboid.scala` | the `Cuboid` type, its diagonals, and how close it is to perfect |
| `core/src/Montgomery.scala` | a·b mod m for any 64-bit numbers, without overflow, by Montgomery's trick |
| `core/src/PollardRho.scala` | Pollard's rho method with Brent's improvements, for large factors |
| `core/src/NumberTheory.scala` | primes (a sieve, and Miller–Rabin for any `Long`), factorisation and divisors |
| `core/src/Pythagorean.scala` | Pythagorean triples via Euclid's formula, and the legs paired with any given leg |
| `core/src/Search.scala` | bounded Euler brick search, its brute-force check, and the unbounded `LazyList` search |
| `core/src/Counting.scala` | how many triples a number is a leg or hypotenuse of, from its factorisation |
| `core/src/Families.scala` | the Berggren tree of all primitive triples, Saunderson's bricks |
| `core/src/Sieve.scala` | factorising a block of consecutive numbers at once, by a segmented sieve |
| `core/src/OddSearch.scala` | the primitive bricks found by their odd edge instead of their smallest |
| `core/src/FastSearch.scala` | the unbounded search with arrays and while loops, and in parallel |
| `core/src/NearMisses.scala` | `SquaredCuboid` for irrational edges, face and edge cuboid searches |
| `core/src/Theory.scala` | known divisibility and parity rules for Euler bricks |
| `cli/src/Summary.scala` | a search's results in a few numbers, as a monoid, so summaries of separate blocks add up |
| `core/src/Elliptic.scala` | exact fractions, and the curves y² = x(x + a²)(x + b²) whose points are Euler bricks |
| `cli/src/Origins.scala` | telling Saunderson bricks, derived bricks and curve multiples apart from the rest |
| `cli/src/Graph.scala` | the Pythagorean graph: degrees, connected pieces, and its triangles, the Euler bricks |
| `cli/src/FourD.scala` | four edges with all six pairs whole, and with five of six |
| `core/src/ClosestCalls.scala` | how far a brick's space diagonal is from a whole number, accurately |
| `cli/src/Local.scala` | divisibility rules for Euler bricks and perfect cuboids, proved by trying every remainder |
| `cli/src/Charts.scala` | SVG line and scatter charts with log axes, written as plain text |
| `cli/src/Pictures.scala` | the charts in `docs/`, from search results |
| `cli/src/Stats.scala` | counts and growth rates for the write-up |
| `cli/src/Quarry.scala` | the kinds of cuboid a hunt can look for: how to search, write and read each |
| `core/src/LineFormat.scala` | a type class for lines of results files, derived for any case class with `derives` |
| `cli/src/Verify.scala` | checking a results file without trusting the code that wrote it |
| `cli/src/Processes.scala` | a hunt split between processes on one machine, and merged |
| `cli/src/Merge.scala` | joining the files of hunts over ranges that follow on from each other |
| `cli/src/Analysis.scala` | the reports that need only a list of bricks, for a fresh search or a hunt's file |
| `cli/src/Chance.scala` | how many perfect cuboids chance alone would put among the bricks found |
| `core/src/Parallelepiped.scala` | slanted boxes, and the perfect ones among them |
| `cli/src/Coverage.scala` | families of bricks built from Pythagorean triples, against the bricks a search found |
| `cli/src/Records.scala` | records among the bricks found, and how bricks per edge follow its legs |
| `cli/src/Misses.scala` | where the space diagonals fall between whole numbers, against an even spread |
| `cli/src/Forecast.scala` | what the bricks found predict for a longer hunt: count, chance, closest call, patterns |
| `cli/src/Eta.scala` | progress lines for long searches: share done, recent speed, time left |
| `cli/src/Hunt.scala` | long searches with Cats Effect and fs2: blocks searched in parallel as a stream of chunks, results file, checkpoints, resume |
| `cli/src/CommandLine.scala` | the commands, and how the command line is parsed into one |
| `core/src/Platform.scala` | what differs on Scala Native: one search thread, and no more |
| `cli/src/Bench.scala` | timing the searches: warm-up, repeated runs, median and range |
| `cli/src/Scaling.scala` | how the search's speed grows with the number of threads, as a table and a chart |
| `cli/src/Main.scala` | the entry point, an `IOApp`, and the pure `Cli.run` it prints |
| `bench/src/Kernels.scala` | JMH microbenchmarks of the search's kernels, on inputs from near 10⁹ |

## License

MIT, see [LICENSE](LICENSE).
