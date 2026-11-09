# Roadmap

Ten phases. Each one adds a piece of mathematics and a piece of Scala, and ends in a
tested state.

- [x] **1. Property-based testing.** State the facts the code relies on (`isqrt` brackets
  the square root, edge order doesn't matter, scaling keeps the kind) and check them on
  random inputs with ScalaCheck.
- [x] **2. Euler brick theory.** Check the known parity and divisibility rules (some edge
  divisible by 16, 9, 5, 11 …) on every brick found. Rules as `Cuboid => Boolean` values.
- [x] **3. Parametric families.** Saunderson's formula and the Berggren tree of primitive
  triples, as infinite `LazyList`s.
- [x] **4. Domain types.** Opaque types for edges and diagonals, extension methods,
  `Either` for errors instead of exceptions.
- [x] **5. Number theory toolkit.** Factorisation, divisors, and every leg pairing with a
  given leg, derived from the factor pairs of its square.
- [x] **6. Unbounded search.** One edge at a time, as an infinite `LazyList[Cuboid]` with
  flat memory use.
- [x] **7. Near-miss catalogue.** Face cuboids, and edge cuboids via squared edges.
- [x] **8. Performance.** A timing harness, a fast inner loop that is pure from the
  outside, and parallelism across edges.
- [x] **9. Long runs, done purely.** Checkpoint, resume and write results with Cats Effect
  and fs2.
- [x] **10. Native binary and write-up.** Scala Native, and statistics on what the
  searches found.

## Thirty more

Same rules: one phase, one commit, every test passing.

### Groundwork

- [x] **11. Stricter compiler.** `-Werror`, explicit nulls and safe initialisation, and
  fix whatever they find.
- [x] **12. Formatting and CI.** `scala-cli fmt` with a checked-in style, and GitHub
  Actions running the tests and the format check on every push.
- [x] **13. A real command line.** Subcommands, `--help`, and clear errors for bad input
  instead of silently falling back to 1000.
- [x] **14. Monoids for results.** Block summaries (counts, extremes, histograms) as
  Cats `Monoid`s, combined with `|+|`, their laws checked with discipline.
- [x] **15. Golden tests.** The output of every command saved under `test/golden`, so a
  refactor cannot change it unnoticed.
- [x] **16. A better benchmark.** Warm-up, repeated runs, median and spread, and the heap
  cap printed with every result.

### Number theory engine

- [x] **17. Primality for any `Long`.** Deterministic Miller–Rabin, with overflow-free
  modular multiplication, checked against the sieve.
- [x] **18. Pollard–Brent rho.** Factorise any 64-bit number, so `legs` works for all of
  them.
- [x] **19. Factorising whole blocks.** A segmented sieve of smallest prime factors, so the
  search factorises a block of edges at once instead of one by one.
- [x] **20. Counting formulas.** How many legs a number pairs with, and in how many ways it
  is a hypotenuse, read off its factorisation and checked against the enumerations.
- [x] **21. Cuboids of any size.** A `BigInt` cuboid sharing its checks with `Cuboid`
  through a type class.
- [x] **22. No cap on the other edges.** 128-bit squares and square roots, so a search by
  smallest edge finds every brick, not only those with every edge ≤ `Edge.Max`.

### More mathematics

- [x] **23. Local obstructions.** Work out by computer which residues modulo small numbers
  an Euler brick or a perfect cuboid can have, derive divisibility rules from them, and
  compare with `Theory.scala` and the literature.
- [x] **24. Derived bricks.** (ab, ac, bc) is an Euler brick whenever (a, b, c) is, and
  deriving twice gives the same shape back, so the primitive bricks come in pairs.
- [x] **25. Elliptic curves.** The third edges that go with two fixed edges a, b are points
  on y² = x(x + a²)(x + b²). Exact rational point arithmetic, and new bricks from multiples
  of a known one.
- [x] **26. Where bricks come from.** What share of primitive bricks are derived bricks,
  Saunderson bricks or curve multiples of smaller ones.
- [x] **27. The Pythagorean graph.** Numbers as vertices, joined when their squares add to
  a square. Euler bricks are its triangles. Degrees and triangle counts.
- [x] **28. Four dimensions.** Four edges whose six pairs all have whole diagonals, the
  graph's 4-cliques. Search for them, and count the near misses with five of six.
- [x] **29. Closest calls.** The Euler bricks whose space diagonal comes nearest to a whole
  number, as a ranked catalogue.

### Searching harder

- [x] **30. Hunt any kind.** The resumable hunt made generic, so face and edge cuboids get
  long runs too.
- [x] **31. Chunked streams.** The hunt emits one fs2 `Chunk` per block straight from
  iterators, with no `LazyList` in between.
- [x] **32. Progress and ETA.** A progress line on long hunts: edges per second, bricks
  so far, time left.
- [x] **33. Verify.** `verify FILE` re-checks every brick in a results file with `BigInt`,
  and that its checkpoints leave no gaps.
- [x] **34. Native, round two.** Single-threaded, iterator-based paths so long runs work on
  Scala Native, measured against the JVM.
- [x] **35. The big hunt.** Smallest edge up to 10⁹ with no cap on the other edges, run
  with a capped heap, and recorded in RESULTS.md.

### Showing it

- [x] **36. Cross-check with the OEIS.** The published Euler brick sequences as test
  fixtures.
- [x] **37. Pictures.** SVG charts (growth on log–log axes, bricks by edge size) written by
  plain Scala into `docs/`.
- [x] **38. Scala.js.** The core library compiled to JavaScript, and a static page that
  checks any box in the browser.
- [x] **39. Guided tour.** Runnable `.sc` worksheets in `docs/` that walk a Scala newcomer
  through the ideas.
- [x] **40. Version 1.0.** A final pass over README and RESULTS, a changelog, and a
  `v1.0` tag.

## Towards 2.0

Thirty more, same rules. Measure before speeding anything up, search further with what that
buys, and make the results easier to see and to reuse.

### Measuring first

- [x] **41. Profiles.** JFR profiles of `unbounded` and `hunt` on an idle machine: how the time
  splits between sieving, generating legs, pairing them and the 128-bit square checks.
- [x] **42. Microbenchmarks.** JMH benchmarks (`scala-cli --power --jmh`) of the kernels: `isqrt`,
  `Wide` square roots, Montgomery multiplication, a sieve block, the legs of one edge.
- [x] **43. A cheaper square test.** The profile (phase 41) puts 86–93% of the time in the
  exact test whether b² + c² is a square, and almost none in allocation: so one rounded
  floating-point square root and one exact check, with no correcting loops or boxing.
- [x] **44. Rejecting pairs sooner.** More remainder tests on b² + c² before the 128-bit square
  root, each kept only if a count of the pairs it rejects says it pays.
- [x] **45. Scaling across threads.** Speed at 1 to 12 threads and for different block sizes,
  tuned, and charted in `docs/`.
- [x] **46. Native threads.** Find why native hunts crash on several threads, fix it, and
  compare native with the JVM at full thread count. (The cause turned out to be inside Scala
  Native's garbage collector, beyond this project's reach, so native runs now refuse more
  than one thread instead.)

### Searching further

- [x] **47. Ranges and shards.** `hunt --from`, so a range can be split across runs or machines,
  and `merge` to join shard files into one that `verify` accepts.
- [x] **48. Reading results files.** `stats`, `origins`, `closest`, `rules` and the charts run
  over a hunt's file instead of a fresh search.
- [x] **49. Past the `Long`.** Smallest edges above 3,037,000,499, whose squares overflow a
  `Long`, with `Wide` all the way through, so the search has no limit left. (In the end
  `BigInt` for the rare edges past it; the limit left is `Edge.Max`, the longest edge a
  `Cuboid` holds.)
- [ ] **50. The long hunt.** Every primitive brick with smallest edge up to 3·10⁹, over several
  resumable nights, verified, and recorded in RESULTS.md.
- [ ] **51. Near misses, far.** Long hunts of face and edge cuboids, and a catalogue of the
  closest near misses found.
- [x] **52. Search by space diagonal.** Face cuboids found a second way, from the sums of three
  squares that make a square, as an independent check on the first.

### More mathematics

- [x] **53. How unlucky are we?** A heuristic for the number of perfect cuboids to expect among
  the bricks found, if each space diagonal were a square by chance, and how it grows.
- [x] **54. Perfect parallelepipeds.** Sawyer and Reiter's 2009 box with slanted faces, whose
  edges, face diagonals and body diagonals are all whole, checked exactly, and a small search.
- [x] **55. More families.** Parametric families of Euler bricks from the literature, generated,
  checked, and measured against the 10⁹ catalogue. (The sources give formulas for
  Saunderson's family only, so: his, its derived bricks and its curve multiples.)
- [x] **56. Local densities.** How often each residue pattern occurs among the primitive bricks
  found, against the shares `Local` predicts.
- [x] **57. Records.** The edges with the most bricks, the longest and flattest bricks, and how
  the number of bricks on an edge follows from its number of legs.

### Scala

- [x] **58. Derived formats.** Each hunt kind's line format derived from its type with
  `derives`, in place of the hand-written `line` and `parse`.
- [x] **59. Newer Scala 3.** Named tuples and the other recent features, where they make the
  code read better, and nowhere else.
- [ ] **60. Stopping at any moment.** A property test that cancels a hunt at random points and
  checks that resuming always ends in the same file.
- [ ] **61. One test suite, three platforms.** The tests of the shared code run on Scala.js and
  Scala Native too, in CI.
- [ ] **62. API docs.** `scala-cli doc` for the library, with every public definition described.

### Sharing it

- [ ] **63. GitHub Pages.** The check page, the charts and the API docs published by CI.
- [ ] **64. Searching in the browser.** A web worker running the unbounded search on the page,
  with progress, and stopped at any time.
- [ ] **65. The catalogue online.** A page listing the primitive bricks found, sortable and
  filterable, each one opening in the checker.
- [ ] **66. Releases.** On a tag, CI builds native binaries for Linux and macOS and a runnable
  jar, and attaches them to a GitHub release.
- [ ] **67. Tour, part two.** Worksheets for what came after 1.0: `Wide`, the hunt's effects,
  the browser build.
- [ ] **68. An essay.** What looking for a perfect cuboid taught us, in plain words, with the
  charts, for readers who know neither the problem nor Scala.
- [ ] **69. A contributor's guide.** How to add a hunt kind, a command or a chart, and how to
  run a shard and check it.
- [ ] **70. Version 2.0.** A final pass over every document, the changelog, and a `v2.0` tag.
