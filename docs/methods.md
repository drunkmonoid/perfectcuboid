# Methods

How this project searches for perfect cuboids and checks what it finds. It is written for
readers who know the problem; [the essay](essay.md) is for those who do not, and
[RESULTS.md](../RESULTS.md) has every figure with the command that reproduces it.

## The search

A primitive Euler brick a < b < c is found from its smallest edge a. Every b with a² + b² a
square is a leg of a right triangle with leg a, so b = (e − d) / 2 for a factorisation a² = d·e
with d < e of the same parity, and b > a exactly when d < a(√2 − 1). For each a, then, the legs
above a are listed exactly from a's factorisation, and each pair of them (b, c) is tested:
a × b × c is an Euler brick exactly when b² + c² is a square.

- **Factorising.** Blocks of consecutive edges are factorised together by a segmented sieve,
  up to 10¹²; beyond it, edge by edge, by trial division and Pollard's rho (with Brent's
  improvements and Montgomery multiplication).
- **Legs past the `Long` square.** Above 3,037,000,499 the square a² overflows a 64-bit integer.
  Each divisor d < a of a² is then kept as u·v with u and v both dividing a, so that
  a²/d = (a/u)(a/v) is a product of two `Long`s, which fits in 64 unsigned bits for every leg
  the search keeps.
- **Pairs.** Most pairs are ruled out by remainders: b² + c² is tested mod 11·43·59, then
  19·53·37, then 13·47·29, each a table of square residues small enough for the L1 cache. The
  moduli were chosen by measuring real pairs of legs, not random numbers: legs of one edge share
  factors of 2, 3 and 5, and some primes that filter random numbers well barely filter these.
  About one pair in 300 reaches the exact test.
- **The exact test.** b² + c² is formed in 128 bits. Below 2¹⁰², the double-precision square root
  rounded is the exact root of any square, so one rounding and one exact squaring decide it;
  above, an integer square root with one Newton step does.
- **Coverage.** Edges are `Long`s up to `Edge.Max` = ⌊(2⁶³ − 1)/√3⌋ ≈ 5.3·10¹⁸, the largest for
  which the space diagonal fits in 64 bits. Up to a = 3,263,469,420 every leg of a is below
  that, so the search finds every brick with smallest edge a. Beyond, the legs from the
  smallest divisors (d < a² / (2·`Edge.Max`)) are longer than any `Cuboid` holds, and the search
  finds every brick with smallest edge a and longest edge up to `Edge.Max`. No brick with
  smallest edge up to 3·10⁹ has a longest edge above 2.4·10¹⁴.

A second search goes by the odd edge instead: every primitive brick has exactly one, every
divisor pair of u² gives a leg when u is odd, and the other two edges are legs on either side
of u. It covers the bricks with odd edge up to N, a subset of those with smallest edge up to N.

## Long runs

A hunt writes each block of edges, its finds and then a checkpoint line, to a text file whose
first line names its kind and format. Stopped at any moment, by Ctrl+C or a crash, it resumes
from the last complete checkpoint, throwing away a half-written block; a property test stops
hunts at random moments and cuts finished files at random bytes, and every resumed run ends in
the same file, byte for byte. Ranges split into shards, on several processes or machines, and
`merge` joins shards whose ranges follow on from each other, refusing gaps and overlaps.

## Checking what was found

- **Without the search's code.** `verify` re-checks every brick in a file with arbitrary-precision
  arithmetic, checks that the checkpoints rise and that every brick lies between the right ones,
  and searches a few blocks spread through the file again.
- **Against published tables.** The 3,556 primitive bricks with longest edge below 5·10⁸ in the
  OEIS (A031173–5) are exactly those in the hunt's file. The file then carries the tables on to
  7,004 bricks, every one with longest edge up to 3·10⁹.
- **Against a second search.** The search by odd edge, to 10⁷, finds exactly the bricks of the
  hunt's file with odd edge up to 10⁷.
- **Against families built forwards.** Saunderson's bricks from every primitive triple with
  hypotenuse up to 3,000, their derived bricks, and the multiples 2P to 4P on the elliptic curves
  of both, reduced to primitive form: every one with all edges up to 10⁹ is in the file.
- **Divisibility rules, proved and certified.** Every residue triple mod pᵏ that a primitive
  brick, or a perfect cuboid, can reduce to is listed by computer, and what they all share is a
  rule that every brick obeys. The listings are written out as certificates and checked by a
  separate program that shares no code with the prover. Mod 3, 5, 7, 11, 13, 17, 19, 29 and 37
  a perfect cuboid must have a length divisible by p; counting the solutions of the equations mod
  every prime up to 1000 finds the same primes and no others.

## What the numbers say

No perfect cuboid has a smallest edge up to 3·10⁹ (15,101 primitive Euler bricks). Published
searches have gone much further; this one is an independent check of part of that range, with
every step recorded.

If each space diagonal fell on a whole number by chance, with the chance s(r)/2√N corrected for
the residues r that squares can take mod 2⁶·3²·5·7·11·13, the bricks up to 3·10⁹ would have held a
perfect cuboid with probability about 0.21, most of it from the smallest bricks. The space
diagonals' fractional parts are spread as such a model needs (Kolmogorov–Smirnov p = 0.22 over
all 15,101), and the model, fitted to the bricks up to 10⁹, forecast the count, the chance and
the residue shares of the bricks from 10⁹ to 3·10⁹ within their ranges. It is a heuristic: the
structure that would make a perfect cuboid exist is exactly what it cannot see.

Two further results came out of the counts mod p. For p ≡ 7 (mod 8) the perfect cuboid
equations have exactly p³ + 9p² − 9p solutions, and for the other classes the count is given
exactly by how p is written as x² + 2y² or u² + v²: fitted here and checked for every odd prime
up to 1000, not proved. And over the Gaussian integers, perfect boxes with a length 0 are
plentiful, but none with all seven lengths nonzero has real and imaginary parts up to 150.
