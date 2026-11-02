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
- [ ] **5. Number theory toolkit.** Factorisation, divisors, and every leg pairing with a
  given leg, derived from the factor pairs of its square.
- [ ] **6. Unbounded search.** One edge at a time, as an infinite `LazyList[Cuboid]` with
  flat memory use.
- [ ] **7. Near-miss catalogue.** Face cuboids, and edge cuboids via squared edges.
- [ ] **8. Performance.** A timing harness, a fast inner loop that is pure from the
  outside, and parallelism across edges.
- [ ] **9. Long runs, done purely.** Checkpoint, resume and write results with Cats Effect
  and fs2.
- [ ] **10. Native binary and write-up.** Scala Native, and statistics on what the
  searches found.
