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

`verify` then checked the file without trusting the search: each of the 2,826,042 bricks again
with BigInt arithmetic, the checkpoints in order with every brick between the right ones, and
three blocks spread through the file searched again. It found no problem, in 23 seconds.

The other edges of a brick can be far longer than the smallest: the longest found here is
22402464 × 15472627027 × 653477074380. Their squares go beyond a `Long`, so the search tests
b² + c² with 128-bit arithmetic (`Wide.scala`). Until that was added, edges were capped at
1,753,413,056, and the same hunt found 11,309 fewer bricks.

Published searches have gone far beyond this: Matson (2015) showed that a perfect cuboid's
smallest edge would have to be above 5·10¹¹. The point here is a search that is easy to
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

## Derived bricks come in pairs

```sh
scala-cli run . -- derived 1e7
```

If a × b × c is an Euler brick, so is ab × ac × bc, its derived brick, and deriving twice
gives back the same shape. So the primitive Euler bricks pair up. The first pair is
44 × 117 × 240 and 429 × 880 × 2340, and two of the five smallest bricks, 140 × 480 × 693 and
160 × 231 × 792, are partners.

| smallest edge ≤ | primitive bricks | pairs with both partners in range |
| --- | ---: | ---: |
| 10⁶ | 608 | 153 |
| 10⁷ | 1,647 | 390 |

So about half of the primitive bricks found have their partner among them as well. No brick is its own
partner, which would need b² = ac.

## New bricks from elliptic curves

```sh
scala-cli run . -- curve 44 117 240
```

Fix two edges a and b. A third edge c works when c² + a² and c² + b² are both squares, and then
(c², c·√(c² + a²)·√(c² + b²)) is a rational point on the elliptic curve y² = x(x + a²)(x + b²).
Points on such a curve can be added (draw the line through two of them, take the third point
where it meets the curve, reflect it), and every multiple of a brick's point is again the
point of a brick with the same ratio a : b. So each Euler brick gives three infinite
families, one for each pair of its edges:

| from 44 × 117 × 240, fixing | 2P | 3P |
| --- | --- | --- |
| 44, 117 | 9555040 × 22855959 × 25407720 | 17-digit edges |
| 44, 240 | 8430569 × 38181000 × 208260000 | 19-digit edges |
| 117, 240 | 9813375 × 20130000 × 24523072 | 16-digit edges |

The edges grow roughly like the square of the multiple: 4P has 28 to 32 digits. The 2P bricks
are within reach of the search, which finds them too: two independent methods agreeing.

## Where the primitive bricks come from

```sh
scala-cli run . -J -Xmx2g -- origins 1e7   # about a minute
```

How many of the primitive bricks found by searching would a formula or a map have given,
starting from something smaller?

| smallest edge ≤ | primitive | Saunderson | derived from a smaller brick | 2P–4P on another's curve | any of these | only by search |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 10⁶ | 608 | 23 | 153 | 1 | 177 | 431 |
| 10⁷ | 1,647 | 53 | 390 | 7 | 449 | 1,198 |

- **Saunderson:** his brick from (u, v, w) has a face diagonal of exactly w³, which is how
  `Origins.isSaunderson` spots one.
- **Curves:** the first curve multiple is 412425 × 4267872 × 4657400, the double of a point
  of 240 × 252 × 275 and also of its derived partner 1008 × 1100 × 1155.

So nearly three quarters of the primitive bricks come from none of these: they are only found
by searching.

## The Pythagorean graph

```sh
scala-cli run . -J -Xmx3g -- graph 1e6   # about 20 seconds
```

Join two numbers when the sum of their squares is a square. An Euler brick is then a triangle:
three numbers all joined to each other.

| numbers 1 to N | 10⁵ | 10⁶ |
| --- | ---: | ---: |
| with at least one neighbour | 66,636 | 677,205 |
| links | 185,864 | 2,269,788 |
| most neighbours | 18480: 145 | 240240: 341 |
| with exactly one neighbour | 24,156 | 233,766 |
| connected pieces | 6,053 | 56,664 |
| largest piece | 41,079 | 390,363 |
| triangles (Euler bricks) | 1,714 | 17,873 |
| numbers in some triangle | 2,941 | 29,611 |

About two thirds of all numbers are a leg of some triple with the other leg in range, and
the links average about 7 per number. Only 3% of numbers are in a triangle. The graph does
not hang together: 3 and 4, for example, pair only with each other, and form a piece of
their own.

## Four dimensions

```sh
scala-cli run . -J -Xmx3g -- fourd 1e6   # about 35 seconds
```

A four-dimensional Euler brick would have four edges with all six pairs making whole face
diagonals: four numbers all joined to each other in the graph above. Any such four contain a
three-dimensional brick, and so do the near misses with five whole pairs out of six, so both
are found by trying to add a fourth edge to every brick.

From the 92,014 bricks with smallest edge ≤ 10⁶ there is **no four-dimensional brick**, and
65,584 near misses, 471 of them primitive. The first is 85 × 132 × 351 × 720: 85 × 132 × 720
is a brick, and 351 makes whole diagonals with 132 and 720, but not with 85.

## Closest calls

```sh
scala-cli run . -J -Xmx2g -- closest 1e7   # about 50 seconds
```

The primitive bricks whose space diagonal comes nearest to a whole number. Only primitive
bricks count: scaling a brick by k scales the miss by k too.

| primitive brick, smallest edge ≤ 10⁷ | space diagonal | off by |
| --- | ---: | ---: |
| 37835 × 269280 × 1244484 | 1,273,846 | 6.5·10⁻⁵ |
| 318175 × 3357792 × 6431880 | 7,262,581 | 8.8·10⁻⁵ |
| 330165 × 672220 × 3502992 | 3,582,156 | 9.0·10⁻⁵ |
| 6954717 × 10367280 × 16888556 | 21,001,712 | 4.1·10⁻⁴ |
| 241604 × 2965395 × 34911360 | 35,037,908 | 4.6·10⁻⁴ |

If the 1,647 misses were spread evenly between 0 and ½, the smallest would be expected around
½ / 1,648 ≈ 3·10⁻⁴, and the three closest calls are a few times nearer than that. Nothing
here suggests the space diagonals are drawn towards whole numbers. They miss about as chance
would have them miss.

## Rules proved by computer

```sh
scala-cli run . -- local      # primes up to 37, about 20 seconds
scala-cli run . -- local 43   # up to 43, about a minute
```

Reduce a primitive brick's edges mod a prime power pᵏ. They are not all divisible by p, and
each face diagonal's square must be a square mod pᵏ, and so must the space diagonal's if the
cuboid is perfect. Trying every triple of remainders (`Local.scala`) shows what all the
possible ones have in common, and so what every brick must have, however large:

| prime power | every Euler brick | every perfect cuboid, in addition |
| --- | --- | --- |
| 2⁸ | one edge divisible by 16, another by 4 | |
| 3⁴ | one edge divisible by 9, another by 3 | |
| 5² | one edge divisible by 5 | |
| 7² | | one edge divisible by 7 |
| 11² | one edge divisible by 11 | |
| 13² | | an edge or the space diagonal divisible by 13 |
| 17², 29², 37² | | one of the seven lengths divisible by p |
| 19² | | one edge divisible by 19 |
| 23², 31², 41², 43² | | nothing |

These are exactly the divisibility rules Kraitchik gave for perfect cuboids in 1945, as
listed on Wikipedia. The computation also shows that the rules for 16, 9 and 5 hold for every
Euler brick, not only for perfect ones. Lower powers are not enough: mod 2⁷ shows only "8 and
4", and mod 17 nothing at all.

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

- **Face cuboids:** there are 5,092 with shared edge ≤ 10⁵, of which 170 are primitive
  (`hunt hunt-face.txt 1e5 --kind face`, 13 seconds). The longest is 9321 × 85428 × 14479920.
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
| readable (`Search`) | 17.61 s | 17.59 – 17.65 s |
| fast, factorising one edge at a time | 6.61 s | 6.61 – 6.66 s |
| fast, sieving blocks of edges | 6.24 s | 6.22 – 6.25 s |
| parallel, an fs2 stream of chunks, 12 threads | 0.95 s | 0.94 – 1.40 s |
| parallel, futures, 12 threads | 1.52 s | 1.51 – 1.56 s |

The two parallel versions are equally fast. Whichever of them runs second takes about 30%
longer: run in the other order, the futures took 1.1 s and the stream 1.5 s. On a laptop that
is most likely heat: after a few minutes at full load the processor slows itself down. So
compare timings taken in the same position, or with pauses between them.

Factorising the edges by a sieve instead of trial division saves only 7%, so most of the
time goes into listing the legs and testing pairs of them, not into factorising.

Since the cap on the other edges was lifted, every search is about 50% slower: it now tests
every pair of legs, including the long ones it used to skip. Before that, with the cap, the
fast search took 4.05 s and the parallel one 0.71 s. A filter by remainders mod 45,045
keeps the extra cost down: it rules out 95.5% of the pairs with an addition and a table lookup.

## Scala Native: long runs work, on one thread

The same code builds to a native executable, about 10 MB, with Scala Native 0.5.12. That
needs clang installed.

```sh
scala-cli --power package . --native --native-mode release-fast -o perfectcuboid
GC_MAXIMUM_HEAP_SIZE=1g ./perfectcuboid legs 240
GC_MAXIMUM_HEAP_SIZE=1g ./perfectcuboid hunt hunt-bricks.txt 1e6
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
| `unbounded 1e6`, one thread | 18.5 s, 97 MB | 11.2 s, 290 MB | same |
| `unbounded 1e6`, every core | crashes, sometimes | 3.6 s, 390 MB | – |
| `hunt 1e6`, one thread | 16.0 s, 166 MB | | same |
| `edge 3000000` | out of heap at 1 GB | 12.4 s | – |
| `stats`, `derived`, `origins`, … | crash | fine | – |

The rows that say nothing about threads are from phase 10. The JVM times include its
start-up, about 0.8 s.

- **Short commands:** native starts instantly and gives the same output. That helps when
  the work itself is small.
- **Long runs work, on one thread.** On Scala Native, `unbounded` and `hunt` search on one
  thread by default (`Platform.defaultThreads`), and they go through an fs2 stream of chunks
  rather than a `LazyList`, so memory stays flat. In phase 10 both crashed.
- **The JVM is faster.** One thread for one thread, it takes 11.2 s where native takes 18.5 s,
  and it can use every core.
- **Allocating on several threads at once** still breaks native runs. With `--threads 4`,
  two of four runs crashed, with a segmentation fault or an error exit. The runs that
  finished gave the right bricks.
- **Lazy lists** still keep too much alive on native: its collector scans the stack
  conservatively, so it can hold on to the start of a `LazyList` and every element computed
  after it. The reports built on `LazyList`s and `Future`s, like `stats`, still crash.
- **One loss on both platforms:** Scala Native's `Runtime` has no `maxMemory`, so the
  benchmark no longer prints the heap cap. It is in each command instead, as `-J -Xmx2g`.

So the JVM is the platform for this project, and the native build is for short commands
and patient single-threaded hunts.
