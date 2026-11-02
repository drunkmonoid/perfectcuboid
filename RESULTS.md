# Results

What the searches in this repository have found so far. Every number here can be reproduced
with the command shown next to it. Timings are on a mid-range laptop (Core i5,
12 threads, 24 GB), on the JVM unless stated.

## No perfect cuboid, as far as we looked

A perfect cuboid would be an Euler brick whose space diagonal is also whole. The hunt searched
every Euler brick with **smallest edge up to 30,500,000** (and other edges up to
`Edge.Max` = 1,753,413,056). It found 2,814,733 Euler bricks and no perfect cuboid.

```sh
scala-cli run . -- hunt hunt-bricks.txt 30500000   # about half a minute
```

Published searches have gone far beyond this. The point here is a search that is easy to
read, checked by tests, and safe to stop and resume.

## How many Euler bricks there are

```sh
scala-cli run . -- stats 7   # about 15 seconds
```

| N | every edge ≤ N | primitive | smallest edge ≤ N | primitive |
| --- | ---: | ---: | ---: | ---: |
| 10³ | 10 | 5 | 69 | 14 |
| 10⁴ | 151 | 19 | 849 | 56 |
| 10⁵ | 1,714 | 65 | 9,004 | 180 |
| 10⁶ | 17,873 | 242 | 92,010 | 608 |
| 10⁷ | 180,953 | 704 | 925,071 | 1,629 |

Fitting count ≈ C·Nᵏ between successive rows gives these values of k:

| between | primitive, every edge ≤ N | primitive, smallest edge ≤ N |
| --- | --- | --- |
| 10³ and 10⁴ | 0.58 | 0.60 |
| 10⁴ and 10⁵ | 0.53 | 0.51 |
| 10⁵ and 10⁶ | 0.57 | 0.53 |
| 10⁶ and 10⁷ | 0.46 | 0.43 |

So primitive Euler bricks grow roughly like √N, perhaps a little slower. The counts of all
bricks grow almost exactly in proportion to N, because every primitive brick contributes all
its multiples.

## Saunderson's formula covers only a few

Saunderson's formula turns every Pythagorean triple into an Euler brick, but of the 242
primitive Euler bricks with every edge ≤ 10⁶, only **15** come from it. Most Euler bricks are
not in that family.

## The known rules hold

Every one of the 925,071 Euler bricks with smallest edge ≤ 10⁷ obeys every rule in
`Theory.scala`:

- one edge is divisible by 16, another by 4;
- one edge is divisible by 9, another by 3;
- some edge is divisible by 5, and some edge by 11;
- so the product of the edges is divisible by 95,040 = 2⁶·3³·5·11;
- in a primitive brick, exactly one edge and exactly two face diagonals are odd.

## Near misses

Each kind has exactly one irrational length out of seven.

```sh
scala-cli run . -- nearmisses
```

| kind | the first one found | ordered by |
| --- | --- | --- |
| body (Euler brick) | 44 × 117 × 240, space diagonal √73225 | smallest edge |
| face | 104 × 153 × 672, face diagonal √474993 | the edge shared by its two whole face diagonals |
| edge | √461776 × 1443 × 1800 | the irrational edge |

- **Face cuboids:** there are 5,092 with shared edge ≤ 10⁵, of which 170 are primitive.
- **Edge cuboids:** they are rare. Only 5 have an irrational edge ≤ √5,000,000, and 3 of
  those are multiples of the first two. The one usually quoted, 520 × 576 × √618849, is the
  second; 1443 × 1800 × √461776 comes first.

## Speed

```sh
scala-cli run . -- bench 1000000
```

| version, smallest edge ≤ 10⁶ | time |
| --- | ---: |
| readable (`Search`) | 12.8 s |
| fast (`FastSearch`) | 4.3 s |
| parallel, 12 threads | 1.0 s |

## Scala Native: works for short commands, not for long ones

The same code builds to a native executable, about 10 MB, with Scala Native 0.5.12. That
needs clang installed.

```sh
scala-cli --power package . --native --native-mode release-fast -o perfectcuboid
GC_MAXIMUM_HEAP_SIZE=1g ./perfectcuboid legs 240
```

Set `GC_MAXIMUM_HEAP_SIZE`. Without it, Scala Native's garbage collector may grow its heap
towards all of the machine's memory, and on this laptop the system killed it.

| command | native | JVM | output |
| --- | ---: | ---: | --- |
| `legs 240` | 0.09 s | 0.9 s | same |
| `saunderson 10000000` | 0.05 s | 1.0 s | same |
| `face 1000` | 0.11 s | 1.0 s | same |
| `rules 100000` | 2.5 s | 1.9 s | same |
| `bricks 100000` | 3.2 s | 2.4 s | same |
| `edge 3000000` | out of heap at 1 GB | 12.4 s | – |
| `unbounded`, `hunt`, `stats` | crash | fine | – |

- **Short commands:** native starts instantly and gives the same output. That helps when
  the work itself is small.
- **Allocation-heavy work:** the JVM is faster. On `bench 100000`, the readable search took
  6.1 s natively against 1.2 s on the JVM, and the fast one 0.67 s against 0.33 s.
- **Long, lazy computations** run out of heap on native. Its collector scans the stack
  conservatively, so it can keep the start of a `LazyList` alive, and then every element
  computed after it. The JVM knows exactly which variables are still in use, so these run
  in 64 MB.
- **Allocating on many threads at once** corrupts memory on native, whether through
  `Future`s or Cats Effect. It shows up as segmentation faults and garbage array indices.
  Single-threaded, the same search is correct.

So the JVM is the platform for this project. The native build is kept because it costs
nothing: the dependencies use `org::name::version`, which picks each library's build for
the current platform.
