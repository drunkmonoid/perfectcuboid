# Results

What the searches in this repository have found so far. Every number here can be reproduced
with the command shown next to it. Timings are on a mid-range laptop (Core i5,
12 threads, 24 GB), on the JVM unless stated.

## No perfect cuboid, as far as we looked

A perfect cuboid would be an Euler brick whose space diagonal is also whole. The hunt searched
every Euler brick with **smallest edge up to 30,500,000**, however long the other edges. It
found 2,826,042 Euler bricks, 2,651 of them primitive, and no perfect cuboid.

```sh
scala-cli run . -J -Xmx2g -- hunt hunt-bricks.txt 30500000   # about two and a half minutes
```

The other edges of a brick can be far longer than the smallest: the longest found here is
22402464 × 15472627027 × 653477074380. Their squares go beyond a `Long`, so the search tests
b² + c² with 128-bit arithmetic (`Wide.scala`). Until that was added, edges were capped at
1,753,413,056, and the same hunt found 11,309 fewer bricks.

Published searches have gone far beyond this. The point here is a search that is easy to
read, checked by tests, and safe to stop and resume.

## How many Euler bricks there are

```sh
scala-cli run . -J -Xmx2g -- stats 7   # about 50 seconds
```

| N | every edge ≤ N | primitive | smallest edge ≤ N | primitive |
| --- | ---: | ---: | ---: | ---: |
| 10³ | 10 | 5 | 69 | 14 |
| 10⁴ | 151 | 19 | 849 | 56 |
| 10⁵ | 1,714 | 65 | 9,004 | 180 |
| 10⁶ | 17,873 | 242 | 92,014 | 608 |
| 10⁷ | 180,953 | 704 | 925,637 | 1,647 |

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

Every one of the 925,637 Euler bricks with smallest edge ≤ 10⁷ obeys every rule in
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
scala-cli run . -J -Xmx2g -- bench 1e6 --runs 3
```

Each search runs twice untimed to let the JVM compile it, then three times timed. The table
gives the median run and the range.

| version, smallest edge ≤ 10⁶ | median | range |
| --- | ---: | ---: |
| readable (`Search`) | 17.77 s | 17.67 – 17.88 s |
| fast, factorising one edge at a time | 6.65 s | 6.64 – 6.76 s |
| fast, sieving blocks of edges | 6.22 s | 6.21 – 6.25 s |
| parallel, sieving blocks, 12 threads | 1.64 s | 1.22 – 1.65 s |

Factorising the edges by a sieve instead of trial division saves only 7%, so most of the
time goes into listing the legs and testing pairs of them, not into factorising.

Since the cap on the other edges was lifted, every search is about 50% slower: it now tests
every pair of legs, including the long ones it used to skip. Before that, with the cap, the
fast search took 4.05 s and the parallel one 0.71 s. A filter by remainders mod 45,045
keeps the extra cost down: it rules out 95.5% of the pairs with an addition and a table lookup.

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
