# Changelog

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
