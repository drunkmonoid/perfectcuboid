# perfectcuboid

[![CI](https://github.com/drunkmonoid/perfectcuboid/actions/workflows/ci.yml/badge.svg)](https://github.com/drunkmonoid/perfectcuboid/actions/workflows/ci.yml)

Playing around with the [perfect cuboid](https://en.wikipedia.org/wiki/Euler_brick#Perfect_cuboid)
problem in plain, purely functional Scala 3: is there a box whose edges, face diagonals and
space diagonal are all whole numbers? Nobody knows.

The biggest search here went through every primitive Euler brick with smallest edge up to 10⁹:
10,220 of them, and no perfect cuboid. See [RESULTS.md](RESULTS.md) for that and the rest of
what the searches have found, [ROADMAP.md](ROADMAP.md) for how the project was built up, and
[CHANGELOG.md](CHANGELOG.md) for what each version holds.
New to Scala? Start with the [guided tour](docs/tour/README.md).

![How the number of Euler bricks grows](docs/growth.svg)

![The primitive Euler bricks, by smallest and longest edge](docs/bricks.svg)

[`docs/catalogue`](docs/catalogue/index.html) lists every primitive brick of the 10⁹ hunt, sortable and
filterable, each one opening in the checker; its data is [`bricks.txt`](docs/catalogue/bricks.txt),
the hunt's file without its checkpoints (`grep -v '^#' hunt-primitive.txt`).

[`docs/check/index.html`](docs/check/index.html) is a page that checks any box in the browser: the
same Scala, compiled to JavaScript by Scala.js, and it can also run the search itself, in a
background thread of the browser (about 35,000 edges a second). Open it from a clone of the
repository through a local web server (browsers start no background threads from files), or on the
project's site, which also has the charts and the API docs. The site is built by the `Pages`
workflow on every version tag, once GitHub Pages is switched on for the repository (Settings >
Pages > Source: GitHub Actions); it then lives at https://drunkmonoid.github.io/perfectcuboid/.

Needs only [scala-cli](https://scala-cli.virtuslab.org) (the `scala` command since Scala 3.5).
`scala-cli run . -- --help` lists every command with its arguments and defaults. For the
big runs, cap the memory with `-J -Xmx2g` after `run .`.

**Searching**

```sh
scala-cli run .                          # Euler bricks with every edge up to 1000
scala-cli run . -- bricks 100000         # ... or up to another limit
scala-cli run . -- unbounded 1e7         # by smallest edge, other edges unbounded, flat memory
scala-cli run . -- hunt hunt-primitive.txt 1e9 --kind primitive  # long search to a file: Ctrl+C stops, rerun resumes
scala-cli run . -- hunt hunt-face.txt 1e6 --kind face            # ... of face cuboids (or bricks, or edge)
scala-cli run . -- hunt part2.txt 2e9 --from 1000000001          # one range of a longer search, a shard
scala-cli run . -- merge all.txt part1.txt part2.txt              # join shards that follow on from each other
scala-cli run . -- verify hunt-primitive.txt --kind primitive    # check a hunt's file without trusting the search
scala-cli run . -- analyse hunt-primitive.txt --kind primitive   # counts, rules, origins and closest calls of its bricks
scala-cli run . -- families hunt-primitive.txt                   # Saunderson's family and its offspring, against the file
scala-cli run . -- densities hunt-primitive.txt                  # how often small primes divide the edges, against prediction
scala-cli run . -- records hunt-primitive.txt                    # the edges with most bricks, flattest and squarest bricks
scala-cli run . -- face 1000             # face cuboids: one face diagonal irrational
scala-cli run . -- edge 3000000          # edge cuboids: one edge irrational
scala-cli run . -- crosscheck 1e5        # face cuboids found two independent ways, compared
scala-cli run . -- parallelepiped 300    # slanted boxes with all 13 lengths whole
scala-cli run . -- nearmisses            # the first body, face and edge cuboid
scala-cli run . -- bench 1e6 --runs 3    # time the readable, fast and parallel searches
scala-cli run . -- scaling --chart t.svg # search speed on 1 thread, 2, ... every core
scala-cli --power run bench              # microbenchmarks of the kernels, by JMH
```

A hunt writes each block of edges followed by a checkpoint line, so stopping it at any
moment loses nothing; just don't run two hunts on the same file at once. To search one range
on several machines or processes at once, give each its own file and `--from`, then `merge`.

**Mathematics**

```sh
scala-cli run . -- check 44 117 240      # the seven lengths of any box, of any size
scala-cli run . -- legs 240              # factorise a number and list every leg it pairs with
scala-cli run . -- legs 3825123056546413051  # ... any number up to 2⁶³ − 1
scala-cli run . -- rules 100000          # check the known Euler brick rules on every brick found
scala-cli run . -- local                 # prove such rules by trying every remainder mod pᵏ
scala-cli run . -- saunderson 1e7        # Saunderson's bricks, built from a formula
scala-cli run . -- derived 1e6           # primitive bricks paired with their derived bricks ab × ac × bc
scala-cli run . -- curve 44 117 240      # new bricks from points on elliptic curves
scala-cli run . -- origins 1e6           # which primitive bricks formulas explain, and which only searching finds
scala-cli run . -- graph 1e5             # the graph joining x and y when x² + y² is a square
scala-cli run . -- fourd 1e5             # four-dimensional Euler bricks, and those missing one pair
scala-cli run . -- closest 1e6           # the bricks whose space diagonal is nearest a whole number
scala-cli run . -- stats 7               # how the number of Euler bricks grows, up to 10⁷
scala-cli run . -- pictures docs 7       # the two charts above, as SVG files
```

**Working on the code**

```sh
scala-cli test .                         # the tests
UPDATE_GOLDEN=1 scala-cli test .         # accept changed command output into test/golden
scala-cli fmt .                          # format the code (CI checks it)
scala-cli doc . -o api -O -project:perfectcuboid   # the API docs, every public definition described, in api/
scala-cli repl .                         # a REPL with the project loaded
scala-cli setup-ide .                    # set up Metals / VS Code
scala-cli test web                       # the web page's tests, on Node
scala-cli test cross --js                # the core's tests on JavaScript (or --native, needs clang)
scala-cli --power package web --js --js-mode release -o docs/check/main.js -f   # rebuild the page
scala-cli --power package . --native --native-mode release-fast -o perfectcuboid  # a native binary (needs clang)
```

The native binary suits short commands and single-threaded hunts: see
[RESULTS.md](RESULTS.md#scala-native-long-runs-work-on-one-thread).

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
| `src/LineFormat.scala` | a type class for lines of results files, derived for any case class with `derives` |
| `src/Verify.scala` | checking a results file without trusting the code that wrote it |
| `src/Merge.scala` | joining the files of hunts over ranges that follow on from each other |
| `src/Analysis.scala` | the reports that need only a list of bricks, for a fresh search or a hunt's file |
| `src/Chance.scala` | how many perfect cuboids chance alone would put among the bricks found |
| `src/Parallelepiped.scala` | slanted boxes, and the perfect ones among them |
| `src/Coverage.scala` | families of bricks built from Pythagorean triples, against the bricks a search found |
| `src/Records.scala` | records among the bricks found, and how bricks per edge follow its legs |
| `src/Eta.scala` | progress lines for long searches: share done, recent speed, time left |
| `src/Hunt.scala` | long searches with Cats Effect and fs2: blocks searched in parallel as a stream of chunks, results file, checkpoints, resume |
| `src/CommandLine.scala` | the commands, and how the command line is parsed into one |
| `src/Platform.scala` | what differs on Scala Native: one search thread by default |
| `src/Bench.scala` | timing the searches: warm-up, repeated runs, median and range |
| `src/Scaling.scala` | how the search's speed grows with the number of threads, as a table and a chart |
| `src/Main.scala` | the entry point, an `IOApp`, and the pure `Cli.run` it prints |
| `bench/Kernels.scala` | JMH microbenchmarks of the search's kernels, on inputs from near 10⁹ |

## License

MIT, see [LICENSE](LICENSE).
