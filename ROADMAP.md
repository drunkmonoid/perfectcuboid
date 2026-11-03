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
- [ ] **27. The Pythagorean graph.** Numbers as vertices, joined when their squares add to
  a square. Euler bricks are its triangles. Degrees and triangle counts.
- [ ] **28. Four dimensions.** Four edges whose six pairs all have whole diagonals, the
  graph's 4-cliques. Search for them, and count the near misses with five of six.
- [ ] **29. Closest calls.** The Euler bricks whose space diagonal comes nearest to a whole
  number, as a ranked catalogue.

### Searching harder

- [ ] **30. Hunt any kind.** The resumable hunt made generic, so face and edge cuboids get
  long runs too.
- [ ] **31. Chunked streams.** The hunt emits one fs2 `Chunk` per block straight from
  iterators, with no `LazyList` in between.
- [ ] **32. Progress and ETA.** A progress line on long hunts: edges per second, bricks
  so far, time left.
- [ ] **33. Verify.** `verify FILE` re-checks every brick in a results file with `BigInt`,
  and that its checkpoints leave no gaps.
- [ ] **34. Native, round two.** Single-threaded, iterator-based paths so long runs work on
  Scala Native, measured against the JVM.
- [ ] **35. The big hunt.** Smallest edge up to 10⁹ with no cap on the other edges, run
  with a capped heap, and recorded in RESULTS.md.

### Showing it

- [ ] **36. Cross-check with the OEIS.** The published Euler brick sequences as test
  fixtures.
- [ ] **37. Pictures.** SVG charts (growth on log–log axes, bricks by edge size) written by
  plain Scala into `docs/`.
- [ ] **38. Scala.js.** The core library compiled to JavaScript, and a static page that
  checks any box in the browser.
- [ ] **39. Guided tour.** Runnable `.sc` worksheets in `docs/` that walk a Scala newcomer
  through the ideas.
- [ ] **40. Version 1.0.** A final pass over README and RESULTS, a changelog, and a
  `v1.0` tag.
