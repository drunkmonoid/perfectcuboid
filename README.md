# perfectcuboid

Playing around with the [perfect cuboid](https://en.wikipedia.org/wiki/Euler_brick#Perfect_cuboid)
problem in plain, purely functional Scala 3: is there a box whose edges, face diagonals and
space diagonal are all whole numbers? Nobody knows.

See [ROADMAP.md](ROADMAP.md) for where this is going.

Needs only [scala-cli](https://scala-cli.virtuslab.org) (the `scala` command since Scala 3.5).

```sh
scala-cli run .                         # list Euler bricks with edges up to 1000
scala-cli run . -- bricks 100000        # ... or up to another limit
scala-cli run . -- unbounded 1000000    # by smallest edge, other edges unbounded, flat memory
scala-cli run . -- rules 100000         # check the known Euler brick rules on them
scala-cli run . -- saunderson 10000000  # Saunderson's bricks, built from a formula
scala-cli run . -- legs 240             # factorise 240 and list every leg it pairs with
scala-cli test .                        # run the tests
scala-cli repl .                        # REPL with the project loaded
scala-cli setup-ide .                   # set up Metals / VSCode
```

| File | What it holds |
| --- | --- |
| `src/Arith.scala` | exact integer square roots, gcd, overflow-checked squares |
| `src/Lengths.scala` | `Edge` and `Diagonal` as opaque types, and `CuboidError` |
| `src/Cuboid.scala` | the `Cuboid` type, its diagonals, and how close it is to perfect |
| `src/NumberTheory.scala` | prime factorisation and divisors |
| `src/Pythagorean.scala` | Pythagorean triples via Euclid's formula, and the legs paired with any given leg |
| `src/Search.scala` | bounded Euler brick search, its brute-force check, and the unbounded `LazyList` search |
| `src/Families.scala` | the Berggren tree of all primitive triples, Saunderson's bricks |
| `src/Theory.scala` | known divisibility and parity rules for Euler bricks |
| `src/Main.scala` | the command line: a pure `Cli.run`, printed by the only impure line |
