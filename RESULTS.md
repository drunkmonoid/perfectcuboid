# Results

What the searches in this repository have found so far. Every number here can be reproduced
with the command shown next to it. Timings are on a mid-range laptop (Core i5,
12 threads, 24 GB), on the JVM unless stated.

## No perfect cuboid, as far as we looked

A perfect cuboid would be an Euler brick whose space diagonal is also whole. Every perfect
cuboid is a multiple of a primitive one, whose edges share no common factor, so it is enough
to look at primitive bricks. The big hunt searched every primitive Euler brick with **smallest
edge up to 1,000,000,000**, however long the other edges. It found 10,220 of them, and no
perfect cuboid.

```sh
scala-cli run . -J -Xmx1g -- hunt hunt-primitive.txt 1e9 --kind primitive   # 4 h 15 min on 10 threads
scala-cli run . -J -Xmx1g -- verify hunt-primitive.txt --kind primitive     # 0 problems
```

The memory stays flat: blocks of edges are searched and written as they finish, so a 1 GB heap
is plenty. The speed fell from about 350,000 edges per second at the start to about 50,000
near the end, since larger edges have more factors and more legs to pair.

| digits in the longest edge | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| primitive bricks | 5 | 14 | 46 | 177 | 462 | 1,180 | 2,747 | 3,930 | 1,384 | 244 | 29 | 2 |

The longest is 825973020 × 74800516901 × 37650741152640, whose longest edge is more than
45,000 times its shortest.

**Checked against the OEIS.** A031173, A031174 and A031175 list the primitive Euler bricks with
longest edge below 5·10⁸, each brick's edges in one of the three sequences. Any such brick has
its smallest edge below 10⁹, so the hunt must contain all of them. It does: the 3,556 bricks in
the hunt's file with longest edge below 5·10⁸ are exactly the 3,556 in the OEIS tables, with
none missing and none extra. (The comment in A031174 and A031175 says "3356 primitive bricks";
the tables the same entries link to have 3,556 rows, so the comment has two digits swapped.)

The earlier hunt of all Euler bricks, not only primitive ones, searched every smallest edge
up to 30,500,000. It found 2,826,042 Euler bricks, 2,651 of them primitive, and no perfect cuboid.

```sh
scala-cli run . -J -Xmx2g -- hunt hunt-bricks.txt 30500000   # about two and a half minutes
```

Stopping is safe at any moment: a property test (`StopAnywhereTest`) stops a hunt after
random delays, up to three times, resuming it each time on a random number of threads, and
another cuts a finished file off at a random byte, as a crash while writing would. Every time,
the resumed hunt ended in exactly the file an uninterrupted one writes, byte for byte.

`verify` then checked the file without trusting the search: each of the 2,826,042 bricks again
with BigInt arithmetic, the checkpoints in order with every brick between the right ones, and
three blocks spread through the file searched again. It found no problem, in 23 seconds.

The other edges of a brick can be far longer than the smallest: the longest found in that hunt is
22402464 × 15472627027 × 653477074380. Their squares go beyond a `Long`, so the search tests
b² + c² with 128-bit arithmetic (`Wide.scala`). Until that was added, edges were capped at
1,753,413,056, and the same hunt found 11,309 fewer bricks.

**No limit below `Edge.Max`.** Up to 3,037,000,499 an edge's square fits in a `Long`, and
every leg that pairs with it stays below `Edge.Max` ≈ 5.3·10¹⁸, the longest edge whose
diagonals fit in a `Long`. Since phase 49, the block search goes on past that edge: it works
out a² and its divisor pairs in `BigInt` arithmetic, and keeps the legs up to `Edge.Max`. So
from there on it finds every brick whose edges all fit, which is every brick a `Cuboid` can
hold. Past 10¹², where the sieve's primes no longer reach the edges' square roots, each edge is
factorised on its own, by trial division and Pollard's rho.

| 100,000 smallest edges from, primitive bricks, 12 threads | time | `verify` |
| --- | ---: | --- |
| 3,037,000,001, across the old limit | 3 s | OK |
| 3,037,000,500, just past it | 3 s | OK |
| 1,000,000,000,001, past the sieve | 7 s | OK |

A test scales the brick 44 × 117 × 240 up so that its smallest edge lands just past the old
limit, and again past 10¹², and checks the search finds both.

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

The big hunt carries the last column further: 4,271 primitive bricks with smallest edge up
to 10⁸, and 10,220 up to 10⁹, which gives k = 0.41 and then 0.38.

So primitive Euler bricks grow roughly like √N, and a little slower as N grows. The counts of all
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

So does every one of the 10,220 primitive bricks with smallest edge ≤ 10⁹ (see the next
section).

## Reading the big hunt's file

```sh
scala-cli run . -J -Xmx2g -- analyse hunt-primitive.txt --kind primitive --charts charts   # 25 seconds
```

`analyse` runs the reports that need only a list of bricks, growth, rules, derived pairs,
origins and closest calls, over the bricks in a hunt's file instead of a fresh search, and can
draw the charts from them too. On the 10⁹ hunt it takes 25 seconds where searching again would
take hours. The figures for 10⁹ in the tables of this file come from it.

![The primitive bricks of the 10⁹ hunt, by smallest and longest edge](docs/bricks-1e9.svg)

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
| 10⁹ | 10,220 | 2,316 |

So up to 10⁷ about half of the primitive bricks found have their partner among them as well,
and up to 10⁹ 45%. No brick is its own
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
| 10⁹ | 10,220 | 259 | 2,316 | 20 | 2,594 | 7,626 |

- **Saunderson:** his brick from (u, v, w) has a face diagonal of exactly w³, which is how
  `Origins.isSaunderson` spots one.
- **Curves:** the first curve multiple is 412425 × 4267872 × 4657400, the double of a point
  of 240 × 252 × 275 and also of its derived partner 1008 × 1100 × 1155.

So nearly three quarters of the primitive bricks come from none of these, at 10⁹ as at 10⁷:
they are only found by searching.

### Families built forwards

```sh
scala-cli run . -J -Xmx2g -- families hunt-primitive.txt   # 40 seconds
```

The usual references give formulas for one family only: Wikipedia and MathWorld both give
Saunderson's of 1740, and mention Euler's two of 1770 and 1772 without formulas. So
`Coverage.scala` builds outwards from Saunderson's: his bricks from every primitive
Pythagorean triple with hypotenuse up to 3,000, their derived bricks, and the bricks at 2P,
3P and 4P on the curves of both, all reduced to primitive form. Of these, every one with all
edges up to 10⁹ should be in the 10⁹ hunt's file, since its smallest edge is below 10⁹ too.

| family | primitive members with every edge ≤ 10⁹ | in the hunt's file |
| --- | ---: | ---: |
| Saunderson's bricks | 143 | 143 |
| their derived bricks | 20 | 20 |
| 2P–4P on the curves of both | 3 | 3 |
| any of these | 166 | 166 |

The file holds 4,631 primitive bricks with every edge up to 10⁹, so these families give 3.6%
of them. Triples up to 1,500 or up to 6,000 give exactly the same table, so no larger triple
adds a member in range. The search found every member: a check on it that shares nothing
with it but the test for an Euler brick.

**Kraitchik's count.** MathWorld quotes Kraitchik (1945) as giving 257 cuboids with odd edge
below a million. Every primitive brick has exactly one odd edge (all 10,220 in the hunt's file
do), and 416 of them have it below a million: so his list, made without computers, was
incomplete, unless it counted something else.

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
those have about 7 neighbours each on average. Only 3% of numbers are in a triangle. The graph does
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
½ / 1,648 ≈ 3·10⁻⁴, and the three closest calls are a few times nearer than that.

The 10⁹ hunt holds 10,220 primitive bricks, and a closer call than any above:

| primitive brick, smallest edge ≤ 10⁹ | space diagonal | off by |
| --- | ---: | ---: |
| 640347864 × 1045085760 × 1939671305 | 2,294,466,039 | 4.2·10⁻⁵ |
| 37835 × 269280 × 1244484 | 1,273,846 | 6.5·10⁻⁵ |
| 280415119 × 720177120 × 5609322840 | 5,662,313,171 | 7.7·10⁻⁵ |
| 318175 × 3357792 × 6431880 | 7,262,581 | 8.8·10⁻⁵ |
| 330165 × 672220 × 3502992 | 3,582,156 | 9.0·10⁻⁵ |

Spread evenly, 10,220 misses would put the smallest around ½ / 10,221 ≈ 4.9·10⁻⁵, and the
closest is 4.2·10⁻⁵. Nothing here suggests the space diagonals are drawn towards whole numbers.
They miss about as chance would have them miss.

## Records

```sh
scala-cli run . -J -Xmx2g -- records hunt-primitive.txt   # 45 seconds
```

Among the 10,220 primitive bricks with smallest edge up to 10⁹:

- **Most bricks on one edge:** 23,760 is an edge of four primitive bricks: 4599 × 18368 ×
  23760, 4599 × 23760 × 144832, 4900 × 17157 × 23760 and 23760 × 35075 × 35604. Four other
  edges, the smallest 700,128, are shared by three.
- **Longest:** 825973020 × 74800516901 × 37650741152640.
- **Flattest:** 61427951 × 12916225080 × 3375128053440, its longest edge 54,944 times its
  smallest.
- **Most like a cube:** 108192528 × 109141700 × 110562771, its longest edge only 2.2% longer
  than its smallest.

**Bricks and legs.** Every brick on a smallest edge a is a pair of the legs above a that pair
with it. So the more legs, the more bricks; but how much more? All the bricks with smallest
edge up to 10⁶, primitive or not, sorted by how many legs their edge has:

| legs above the edge | edges | pairs of legs, on average | bricks, on average | bricks per 1,000 pairs |
| --- | ---: | ---: | ---: | ---: |
| 0–1 | 120,038 | 0 | 0 | – |
| 2–3 | 38,414 | 3.0 | 0.0001 | 0.03 |
| 4–7 | 283,706 | 8.1 | 0.0001 | 0.01 |
| 8–15 | 247,458 | 67 | 0.024 | 0.35 |
| 16–31 | 104,807 | 243 | 0.039 | 0.16 |
| 32–63 | 145,648 | 994 | 0.17 | 0.17 |
| 64–127 | 37,588 | 4,627 | 0.52 | 0.11 |
| 128 or more | 22,341 | 27,112 | 1.7 | 0.06 |

The bricks follow the pairs, but less and less: an edge with many legs pairs with legs far
longer than itself, up to a²/2, and a sum of two long squares is a square less often. The
record is 720,720 = 2⁴ · 3² · 5 · 7 · 11 · 13, the least common multiple of 1 to 16, with 1,184
legs above it and 25 bricks on it.

## How unlucky are we?

```sh
scala-cli run . -J -Xmx2g -- analyse hunt-primitive.txt --kind primitive   # its last table
```

Suppose chance alone decided whether a brick's space diagonal is whole. How many perfect
cuboids should the hunt have found? Near a number N, squares are about 2√N apart, so a number
taken at random is a square with a chance of about 1 / (2√N). Remainders change that: if
N ≡ r (mod m) and s(r) numbers x mod m have x² ≡ r, the chance becomes s(r) / (2√N), which is
0 when no square leaves remainder r. `Chance.scala` takes m = 2⁶ · 3² · 5 · 7 · 11 · 13 and N
the square of each primitive brick's space diagonal, and adds up the chances.

| primitive bricks, smallest edge | bricks | by size alone | by size and remainder | ruled out by remainder |
| --- | ---: | ---: | ---: | ---: |
| up to 100 | 2 | 2.5·10⁻³ | 0 | 2 |
| 101 to 1,000 | 12 | 3.4·10⁻³ | 0.13 | 7 |
| to 10⁴ | 42 | 1.0·10⁻³ | 0.054 | 9 |
| to 10⁵ | 124 | 2.7·10⁻⁴ | 0.015 | 37 |
| to 10⁶ | 428 | 1.2·10⁻⁴ | 0.0062 | 142 |
| to 10⁷ | 1,039 | 2.6·10⁻⁵ | 0.0015 | 270 |
| to 10⁸ | 2,624 | 6.5·10⁻⁶ | 4.0·10⁻⁴ | 709 |
| to 10⁹ | 5,949 | 1.5·10⁻⁶ | 9.6·10⁻⁵ | 1,600 |
| **in all** | **10,220** | **0.0085** | **0.21** | **2,776** |

- **Remainders matter a great deal.** 2,776 of the 10,220 bricks, 27%, have a space diagonal
  whose square leaves a remainder no square leaves, so they could never have been perfect,
  among them the first two, 44 × 117 × 240 and 85 × 132 × 720. The rest leave remainders that
  squares leave unusually often, mostly from high powers of 2 and 3, and together that makes
  the chance 25 times what size alone suggests.
- **So: about one chance in five** that a perfect cuboid would have turned up below 10⁹. Not
  finding one is no surprise.
- **Most of that chance was spent early.** Each decade adds about a quarter of what the one
  before it added: the bricks there are 2½ times as many, but their space diagonals are ten
  times as long. If that goes on, all the bricks beyond 10⁹ together add about 3·10⁻⁵.

On this reckoning a perfect cuboid, if one exists, is most likely small enough that searches
would have found it already, and the expected number of them is finite and well below one.
That fits the general belief that there are none. It is a heuristic, not evidence: whether a
diagonal is whole is not decided by chance, and a structure that made perfect cuboids exist
would be just the kind of thing this reckoning cannot see.

## Perfect parallelepipeds

```sh
scala-cli run . -J -Xmx2g -- parallelepiped 700   # about a minute
```

Let the box lean. A parallelepiped is spanned by three edge vectors u, v, w, with
parallelograms for faces, and has 13 lengths: 3 edges, 6 different face diagonals |u ± v|,
|u ± w|, |v ± w|, and 4 body diagonals. A perfect parallelepiped has all 13 whole, and the
perfect cuboid is the one whose edges meet at right angles. Sawyer and Reiter showed in 2009
that slanted perfect parallelepipeds exist, with this example:

| edges | face diagonals | body diagonals |
| --- | --- | --- |
| 271, 106, 103 | 101, 183, 255, 266, 312, 323 | 272, 278, 300, 374 |

`Parallelepiped.scala` holds a shape by its edges and p = 2u·v, q = 2u·w, r = 2v·w, which
are whole numbers, and every squared length is a sum of them: |u ± v|² = a² + b² ± p, and so
on. The example checks out, with u·v = 9826, u·w = 6647 and v·w = 5822, and the three vectors
span a solid (their matrix of dot products is positive definite).

To search, note that |u + v|² + |u − v|² = 2(a² + b²): the two face diagonals of a face split
2(a² + b²) into two squares, and each split gives one possible p. Splitting for every pair of
edges, and trying the combinations for each triple:

| every edge ≤ | perfect parallelepipeds |
| ---: | --- |
| 300 | 1: Sawyer and Reiter's, the smallest there is |
| 700 | 4: theirs, twice theirs, 630 × 365 × 335 and 647 × 540 × 375 |

None is a cuboid. Whether the last two are in Sawyer and Reiter's paper, which lists more
examples, has not been checked here. Unlike cuboids, slanted perfect boxes turn up readily in
a small search: so whatever stops a perfect cuboid from existing, if anything does, needs the
right angles.

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

### How often, not only whether

```sh
scala-cli run . -J -Xmx2g -- densities hunt-primitive.txt   # 15 seconds
```

The rules say which patterns of divisibility a primitive brick can have. How often should
each one occur? The usual guess is the local density: mod m = pᵏ, the edges a, b, c have
N(a² + b²) · N(a² + c²) · N(b² + c²) solutions for their face diagonals, N(r) being the
number of square roots of r, and each pattern's share of all those solutions is its
predicted share of the bricks. Against the 10,220 primitive bricks of the 10⁹ hunt, with
patterns written as how often p divides each edge, largest first:

| p | pattern | predicted | found |
| ---: | --- | ---: | ---: |
| 2 | (4, 2, 0) | 25.8% | 32.9% |
| 2 | (5+, 2, 0) | 25.8% | 25.3% |
| 2 | (5+, 3, 0) | 25.8% | 24.4% |
| 2 | (5+, 4, 0) | 12.9% | 10.8% |
| 2 | (5+, 5+, 0) | 9.7% | 6.6% |
| 3 | (2+, 1, 0) | 68.6% | 76.1% |
| 3 | (2+, 2+, 0) | 31.4% | 23.9% |
| 5 | (2+, 1, 0) | 41.0% | 34.2% |
| 5 | (1, 0, 0) | 20.5% | 28.9% |
| 5 | (1, 1, 0) | 20.5% | 24.9% |
| 5 | (2+, 2+, 0) | 12.8% | 7.7% |
| 5 | (2+, 0, 0) | 5.1% | 4.4% |
| 7 | two edges | 54.1% | 41.2% |
| 7 | one edge | 27.5% | 31.6% |
| 7 | none | 18.4% | 27.2% |
| 11 | two edges | 57.9% | 47.8% |
| 11 | one edge | 42.1% | 52.2% |
| 13 | two edges | 45.9% | 32.5% |
| 13 | one edge | 35.3% | 37.9% |
| 13 | none | 18.4% | 29.6% |

- **The modulus has to be large.** A first try mod 2⁸ predicted 14% of bricks with edges
  divisible by exactly 2⁴ and 2³, and the hunt had none. Mod 2⁸, 2⁶ · 5 = 320 ≡ 64 = 8², so it
  looks like a square, but 5 is not a square mod 8, so 2⁶ · 5 is not one. Whether a sum of
  squares divisible by p²ᵛ is a square shows only in its digits beyond that, so the table uses
  2¹³, 3⁷, 5⁵, 7⁴, 11³ and 13³, and counts classes of remainders rather than remainders to keep
  that affordable. Then no pattern is predicted that never occurs, and every pattern that
  occurs is predicted: a check on the rules from the other side.
- **The shares are roughly right, and off in one direction.** The bricks found are less
  divisible than predicted, by up to 13 points: fewer have two edges divisible by 7, 11 or
  13, and more have none. Nothing promises that local densities predict this. They are
  justified by the circle method for equations in many variables, and three equations in six
  variables are too few; they also count every solution mod pᵏ alike, while these bricks
  were chosen by their smallest edge.

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

### Face cuboids, found a second way

```sh
scala-cli run . -- crosscheck 1e5   # about a minute
```

The face cuboid search starts from the edge where the two whole face diagonals meet and pairs
up the legs that go with it. `crosscheck` also finds them the other way round, from the space
diagonal d. If the face s × x has the whole diagonal p, and y is the third edge, then
d² = p² + y² and p² = s² + x²: so a face cuboid is a hypotenuse split into two legs, one of
which splits again. The splits come from Euclid's formula, k(m² − n², 2mn, m² + n²), over the
divisors of the hypotenuse, with no legs paired at all. The two searches share only the test
for squares.

| space diagonal ≤ | from the space diagonal | from the shared edge | the same |
| ---: | ---: | ---: | --- |
| 10⁴ | 85 | 85 | yes |
| 10⁵ | 1,127 | 1,127 | yes |

So the first search misses none of the face cuboids up to there, and the second finds none
the first does not.

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
keeps the extra cost down, by ruling out most pairs with an addition and a table lookup.

### Where the time goes

Java Flight Recorder, built into the JDK, samples what each thread is running about every
10 ms. Two slices of the primitive hunt on one thread, recorded with it: smallest edges from
1 to 2 million, and the 300,000 just below 10⁹, each resumed from a results file holding only
a checkpoint.

```sh
printf '# searched smallest edges up to 999000000\n' > hunt-slice.txt
scala-cli run . -J -XX:StartFlightRecording=filename=slice.jfr,settings=profile -- \
  hunt hunt-slice.txt 999300000 --kind primitive --threads 1
jfr view hot-methods slice.jfr
```

| share of the samples | edges 1–2 million | edges near 10⁹ |
| --- | ---: | ---: |
| the exact test whether b² + c² is a square | 86% | 93% |
| the pair loop and its remainder filter | 3% | 5% |
| listing and sorting the legs | 9% | 2% |
| factorising by the sieve, the stream, writing the file | under 2% | under 1% |
| edges per second, one thread | 83,000 | 8,500 |

Taken at its word, the profile says almost all the time is the square root, and nearly none
is factorising: sieving the edges costs less than the sort that puts their legs in order. The
second half is right; the first is not, as the microbenchmarks below show. Allocation doesn't matter either: the
JVM allocates about 200 MB a second, mostly boxed `Long`s from `Option[Long]` results, but the
garbage collector paused for 0.07 s in all of the first slice's 13 s.

Why so many square roots? Counting the pairs answers it:

| per smallest edge | edges 1–2 million | edges near 10⁹ |
| --- | ---: | ---: |
| legs above the edge | 24.7 | 51.5 |
| pairs of legs | 1,358 | 12,272 |
| pairs passing the filter mod 45,045 | 14.6% | 16.3% |
| … and then mod 64, so on to the exact test | 8.4% | 9.9% |

For random numbers, only 0.84% would get through both. These are not random. Every
Pythagorean triple has a leg divisible by 3 and one divisible by 4, so if the edge is not a
multiple of 3, every leg paired with it is, and b² + c² is a multiple of 9, which passes the
test mod 9 every time. In the same way, an odd edge makes every leg a multiple of 4, and the
test mod 64 then passes far more often than for random sums; 5 does something similar. Near 10⁹ that leaves about 1,200
exact square roots per edge, almost all of them on 128-bit numbers.

### Microbenchmarks

```sh
scala-cli --power run bench   # about a minute
```

[JMH](https://github.com/openjdk/jmh), the JDK's benchmark harness, times small pieces of code
in a fresh JVM after warming it up. The inputs in `bench/` are those of the search near 10⁹:
the 100 edges up to 10⁹, their legs, and the pairs of legs that pass the filter mod 45,045.
Each kernel runs over many inputs, since a single one can be unusually easy or hard.

| kernel, near 10⁹ | time per call |
| --- | ---: |
| `Arith.isqrt`, a 62-bit number | 6.3 ns |
| `Wide.isqrt`, a 128-bit sum of two squared legs | 18 ns |
| the exact test on a pair that passed the filter mod 45,045 | 17 ns |
| `Montgomery.mulMod`, 64-bit numbers | 16 ns |

| step of the search, near 10⁹ | time per edge |
| --- | ---: |
| factorising, by the sieve | 0.24 µs |
| listing the legs | 1.9 µs |
| the whole search | 97 µs |
| … the same pair loop and filters, with no exact test | 47 µs |

The last row is a control: the search's own loop with the exact test left out. It takes half
the time. So the exact tests take the other half, about 50 µs per edge, not the 93% the
profile reported. JFR can only say where a thread was at its last debug point, and with the
square root inlined into the pair loop by the JIT compiler, the loop's samples were charged to
the square root.

That makes the filter as costly as the tests it saves: 3.8 ns a pair, though it is only an
addition, a comparison and a table lookup. The likely reason is the table: 45,045 `Boolean`s
take 44 KB, about all of a core's fastest cache on this processor (32 or 48 KB), and pairs look
it up at random.
So there are two targets, a cheaper exact test and a cheaper, sharper filter.

### A cheaper square test

The exact test used to find the floor of the square root, correcting a floating-point guess
step by step, and then compare its square. But to know whether b² + c² is a square, rounding
is enough: when the root r is below 2⁵¹, converting the sum to a `Double` and taking
`math.sqrt` lands within a quarter of r, so rounding gives r exactly, and one exact
multiplication confirms it or not. Above 2⁵¹ (bricks far beyond any search here) the old way
takes over. The answer is never a guess, since the candidate is always squared and compared.

Property tests try both sides of 2⁵¹, and a one-off check of 4 million Pythagorean pairs and
their neighbours against `BigInt` found no difference.

| near 10⁹, one thread | before | after |
| --- | ---: | ---: |
| the exact test, per pair passing the filter (JMH) | 17 ns | 9.0 ns |
| the whole search, per edge (JMH) | 97 µs | 74 µs |
| hunting edges 999,000,001 to 999,300,000 | 31 s | 24 s |
| hunting edges 1,000,001 to 2,000,000 | 15 s | 12 s |

Both hunts found the same bricks as before. The filter is now the larger half.

### A sharper filter, and a smaller one

Which moduli are worth testing? Counting, on the 14.5 million pairs of legs of the 300 edges
up to 10⁹, how many pass a test for squares by each modulus on its own:

| modulus | real pairs passing | random sums passing |
| --- | ---: | ---: |
| 64 | 64.5% | 18.8% |
| 9 | 59.3% | 44.4% |
| 25 | 62.4% | 44.0% |
| 7 | 89.4% | 57.1% |
| 17, 41, 61 | 73–77% | 51–53% |
| 11 | 40.1% | 54.5% |
| 43, 59, 67, 83 | 48–50% | 51% |
| other primes up to 97 | 51–60% | 51% |

The powers of 2, 3 and 5, which the old modulus 45,045 = 9 · 5 · 7 · 11 · 13 and the test mod
64 lean on, are the weakest, and so is 7. Most other primes let through about half, and
almost independently of each other, so choosing them one at a time, each time the one that
leaves fewest pairs, gives 11, 43, 59, 19, 53, 37, 13, 47 and 29.

Each table of square remainders now holds one bit per remainder, and three primes share a
table: mod 27,907 = 11 · 43 · 59, then 37,259 = 19 · 53 · 37, then 17,719 = 13 · 47 · 29.
Each table takes 2 to 5 KB, so all three stay in the fastest cache, where the old 44 KB table
did not. A pair costs an addition and a bit looked up per table, and tables after the first
are only consulted for the pairs that pass it.

| share of pairs left | edges near 10⁹ | edges near 2 million |
| --- | ---: | ---: |
| after the old filter (mod 45,045, then 64) | 5.43% | 8.81% |
| after mod 27,907 | 8.96% | 11.09% |
| … then mod 37,259 | 1.22% | 1.63% |
| … then mod 17,719 | 0.20% | 0.29% |

Benchmarks picked the number of tables: with two, the search took 41 µs per edge, with three
35 µs, and with four 39 µs, since a fourth table costs every surviving pair more than its
exact tests do. A first table of only 11 · 43 was slower again, at 58 µs.

| one thread | before phase 43 | after phase 43 | now |
| --- | ---: | ---: | ---: |
| the whole search, per edge near 10⁹ (JMH) | 97 µs | 74 µs | 35 µs |
| hunting edges 999,000,001 to 999,300,000 | 31 s | 24 s | 10 s |
| hunting edges 1,000,001 to 2,000,000 | 15 s | 12 s | 6 s |

Three times as fast near 10⁹. The hunts found the same primitive bricks as the 10⁹ hunt did
in the same ranges, and every Euler brick with smallest edge from 5,000,001 to 5,300,000,
27,813 of them, came out byte for byte the same as before.

### Threads

```sh
scala-cli run . -J -Xmx1g -- scaling --chart docs/threads.svg   # about two minutes
```

`scaling` searches the same million edges just below 10⁹ on 1 thread, then 2, and so on up to
every core, after a warm-up.

![Search speed by number of threads](docs/threads.svg)

| threads | time | edges per second | speed-up |
| ---: | ---: | ---: | ---: |
| 1 | 32.8 s | 30,516 | 1.00 |
| 2 | 16.9 s | 59,031 | 1.93 |
| 4 | 11.2 s | 89,197 | 2.92 |
| 6 | 9.6 s | 103,951 | 3.41 |
| 8 | 7.7 s | 129,528 | 4.24 |
| 10 | 7.2 s | 138,971 | 4.55 |
| 12 | 6.9 s | 144,961 | 4.75 |

Twelve threads are not twelve times as fast, and this processor (an Intel Core i5-13420H)
explains most of it. It has 4 performance cores that each run two threads, sharing one core's
units between them, and 4 slower efficiency cores. A lone thread also runs at a higher clock
than a dozen do, and the laptop has to stay cool. Every thread still adds something, so the
default stays at every core.

These numbers move. While they were measured, other programs (an editor or two and a browser)
kept a load of 6 to 10 on the machine, and repeating the run with 12 threads gave anything
from 6.9 to 11.2 s. One thread is steadier, which is why the earlier comparisons use one.

**Block size.** A hunt hands each thread a block of edges at a time and writes a checkpoint
after each. Timed in one JVM, three rounds in turn so that changes in the load fall on every
size alike, with 12 threads on the same million edges:

| block size | three runs | median |
| ---: | --- | ---: |
| 2,000 | 9.36, 9.18, 9.67 s | 9.36 s |
| 10,000 | 8.49, 8.78, 8.58 s | 8.58 s |
| 100,000 | 8.83, 9.41, 8.80 s | 8.83 s |

Small blocks pay a little more for starting each block and for keeping them in order, large
ones leave threads idle at the end, and 10,000, the size the hunt already used, is best. It
can be changed with `hunt --block-size`.

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
- **Several threads break native runs**, and not only by crashing: some runs that finish
  give wrong answers. Native runs now refuse `--threads` above 1; the next section says why.
- **Lazy lists** still keep too much alive on native: its collector scans the stack
  conservatively, so it can hold on to the start of a `LazyList` and every element computed
  after it. The reports built on `LazyList`s and `Future`s, like `stats`, still crash.
- **One loss on both platforms:** Scala Native's `Runtime` has no `maxMemory`, so the
  benchmark no longer prints the heap cap. It is in each command instead, as `-J -Xmx2g`.

So the JVM is the platform for this project, and the native build is for short commands
and patient single-threaded hunts.

### Why several threads break native runs

Phase 46 looked for the cause. With the faster search of phases 43 and 44, `unbounded 300000`
on 4 threads, run five times as a native executable, crashed three times: twice with an array
index of tens of millions into the filter's table of 437 Longs, once with a segmentation
fault. Of the two runs that finished, neither printed the JVM's 27,387 lines. One thread gave
the JVM's output exactly, every time.

The index comes from a table of remainders that the search fills a moment earlier with
numbers below 27,907, in an array no other thread can see. So the array's memory changed
underneath the code. Narrowing it down, each experiment five or six runs of 100,000 or
300,000 edges on 4 threads:

| experiment | result |
| --- | --- |
| as built, 1 thread | right every time |
| as built, 4 threads | crashes or wrong answers in most runs |
| multithreading support forced on when linking | the same (it was on already) |
| the `commix` garbage collector instead of `immix` | the same |
| no Cats Effect or fs2: 4 plain `Thread`s calling the search | the same |
| 4 plain threads, 2 GB heap, so the collector never needs to run | right every time |
| 4 plain threads, 256 MB heap | 3 runs of 4 wrong |
| 4 plain threads, 256 MB heap, an unoptimised debug build | right every time, 4 times slower |

So the search's code is not the cause, and neither are Cats Effect and fs2: nothing is
shared between the threads, and the same code is right on one thread and on the JVM. What
breaks it is a garbage collection while several threads are running optimised native code.
That is a fault inside Scala Native 0.5.12 (the newest release), whose collector scans
stacks conservatively. One plausible mechanism: optimised code can keep a pointer into the
middle of an array and drop the pointer to its start, and a collector that only recognises
the start frees the array while it is still in use.

A wrong answer is worse than a crash in a search like this one, so native runs now refuse
`--threads` above 1, with a message saying why. To use every core with the native build, run
several single-threaded processes on separate ranges of edges (phase 47).

| hunt, by its own timer | to 10⁶ | 300,000 edges near 10⁹ | memory |
| --- | ---: | ---: | ---: |
| native, 1 thread | 9 s | 15 s | 94–111 MB |
| JVM, 1 thread | 5 s | 11 s | 280 MB |
| JVM, 12 threads | 2 s | 4 s | 400–500 MB |

All three wrote the same files. The JVM is faster even on one thread, and the native build
uses a third of the memory.

### One test suite, three platforms

```sh
scala-cli test cross            # the JVM
scala-cli test cross --js       # JavaScript, on Node
scala-cli test cross --native   # a native executable
```

`cross/` builds the pure core, from `Arith` and `Wide` to `FastSearch` and `Parallelepiped`,
with the 16 test suites that need nothing else, for any of the three platforms. All 114 tests
pass on each, CI runs them on JavaScript and Scala Native at every push, and the root project
keeps testing everything on the JVM. The core has to be the same code everywhere: a `Long`
on JavaScript is emulated in software, and on Scala Native it is a machine word, and the
128-bit arithmetic of `Wide` gives the same answers on both. Native tests are built optimised:
unoptimised, the search for perfect parallelepipeds up to 300 took longer than munit's 30
seconds.

### Every core, as separate processes

Since phase 47 a hunt can cover one range of edges, a shard: `hunt FILE UPTO --from FIRST`
starts a new file at FIRST, with a first line that says so, and `merge` joins shards whose
ranges follow on from each other into one file, refusing any gap or overlap. So the native
build can use several cores after all, as several processes:

```sh
for k in 0 1 2 3; do
  from=$((999000001 + k * 75000))
  GC_MAXIMUM_HEAP_SIZE=512m ./perfectcuboid hunt part$k.txt $((from + 74999)) --from $from &
done; wait
./perfectcuboid merge near1e9.txt part0.txt part1.txt part2.txt part3.txt
./perfectcuboid verify near1e9.txt
```

Four native processes searched the 300,000 edges near 10⁹ in 5.9 s, where one took 15 s. The
merged file passed `verify`, and held every primitive brick the 10⁹ hunt had found there.
