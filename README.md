# perfectcuboid

Playing around with the [perfect cuboid](https://en.wikipedia.org/wiki/Euler_brick#Perfect_cuboid)
problem in plain, purely functional Scala 3: is there a box whose edges, face diagonals and
space diagonal are all whole numbers? Nobody knows.

See [ROADMAP.md](ROADMAP.md) for where this is going.

Needs only [scala-cli](https://scala-cli.virtuslab.org) (the `scala` command since Scala 3.5).

```sh
scala-cli run .            # list Euler bricks with edges up to 1000
scala-cli run . -- 100000  # ... or up to another limit
scala-cli test .           # run the tests
scala-cli repl .           # REPL with the project loaded
scala-cli setup-ide .      # set up Metals / VSCode
```

| File | What it holds |
| --- | --- |
| `src/Arith.scala` | exact integer square roots, gcd, overflow-checked squares |
| `src/Cuboid.scala` | the `Cuboid` type, its diagonals, and how close it is to perfect |
| `src/Pythagorean.scala` | Pythagorean triples via Euclid's formula |
| `src/Search.scala` | Euler brick search, plus a brute-force version to check it against |
| `src/Main.scala` | the entry point, the only code with side effects |
