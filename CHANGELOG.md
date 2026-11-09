# Changelog

## 2.0

Thirty more phases, 41 to 70.

### Faster

- Profiles and JMH microbenchmarks first: the time went into the exact square test and the
  remainder filter, not into factorising, and the profiler overstated the first.
- A square test by rounding the floating-point root, exact below 2⁵¹; remainder filters chosen
  by measuring real pairs of legs, in tables small enough for the fastest cache. Three times as
  fast near 10⁹.
- `scaling` times the search on 1 to 12 threads; `hunt --block-size`.

### Searching further

- A hunt of every primitive brick with smallest edge up to 3·10⁹: 15,101 bricks, no perfect
  cuboid, verified. Face cuboids to a shared edge of 10⁷ and edge cuboids to a squared edge of
  10⁹, with their closest calls. See RESULTS.md.
- Shards: `hunt --from` and `merge`; past the `Long` square limit, up to `Edge.Max`.
- `analyse`, `families`, `densities`, `records` and `nearcalls` read a hunt's file instead of
  searching again; `crosscheck` finds face cuboids a second, independent way.

### More mathematics

- How many perfect cuboids chance would predict: about one in five up to 3·10⁹, mostly spent on
  small bricks.
- Perfect parallelepipeds: Sawyer and Reiter's checked, and four found with edges up to 700.
- Saunderson's family and its offspring built forwards and found in the catalogue; local
  densities against the bricks found; records.

### Scala and tooling

- Derived line formats (`derives LineFormat`), named tuples, property tests that stop hunts at
  random moments, every public definition documented, API docs.
- The core's tests on the JVM, JavaScript and Scala Native; native runs keep to one thread,
  since several corrupt memory in optimised builds.
- A site for GitHub Pages: the checker, a search in the browser, the catalogue, the charts and
  the API docs. Releases with a Linux executable and a jar. A second part of the tour, an essay,
  and a contributor's guide.

### Corrected

- RESULTS.md gave the chance of a perfect cuboid by size alone as 0.0085 in all; its own rows
  add up to 0.0074, so remainders raise it 28 times, not 25.

## 1.0

Forty phases, each one commit. [ROADMAP.md](ROADMAP.md) describes them, and
[RESULTS.md](RESULTS.md) says what they found.

### Searching

- Euler bricks with every edge up to a limit, and by smallest edge with the other edges
  unbounded: 128-bit arithmetic for the squares, a remainder filter for the pairs, a
  segmented sieve for factorising, and a parallel fs2 stream of chunks.
- Long hunts written to a file with checkpoints, stopped with Ctrl+C and resumed, for Euler
  bricks, primitive bricks only, face cuboids or edge cuboids, with a progress line and
  an estimate of the time left.
- `verify`, which checks a hunt's file without trusting the code that wrote it.
- A hunt of every primitive Euler brick with smallest edge up to 10⁹: 10,220 bricks, no perfect
  cuboid, and the OEIS's 3,556 smallest bricks all among them.

### Mathematics

- Pythagorean triples by Euclid's formula and the Berggren tree, every leg pairing with a
  number from the factor pairs of its square, and how many there are from its factorisation.
- Factorisation of any 64-bit number: trial division, Miller–Rabin with Montgomery
  multiplication, Pollard–Brent rho.
- Saunderson's bricks, derived bricks, and new bricks from points on elliptic curves; which
  primitive bricks these explain, and which only a search finds.
- Divisibility rules for Euler bricks and perfect cuboids, proved by trying every
  remainder, which reproduce Kraitchik's rules of 1945.
- Face and edge cuboids, four-dimensional bricks, the Pythagorean graph, and the bricks
  whose space diagonal comes nearest to a whole number.

### Scala

- Plain scala-cli, with every warning an error, explicit nulls and safe initialisation.
- Opaque types, enums, `Either` errors, type classes (`WholeNumber`), monoids with their laws
  checked, `LazyList`s, Cats Effect and fs2, a decline command line.
- Property tests, golden tests of every command's output, and the published OEIS sequences
  as fixtures; CI on every push.
- A Scala Native build for short commands and single-threaded hunts, a Scala.js page that
  checks any box, SVG charts, and a guided tour in five worksheets.
- MIT licensed.
