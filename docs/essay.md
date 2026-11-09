# Looking for a perfect box

A box has twelve edges, but only three lengths among them: call them a, b and c. Across each
face runs a diagonal, and through the middle, from one corner to the opposite one, runs the
longest line in the box, the space diagonal. That makes seven lengths. Pythagoras tells us
what the diagonals are: the face diagonals are √(a² + b²), √(a² + c²) and √(b² + c²), and the
space diagonal is √(a² + b² + c²).

Can all seven be whole numbers? Nobody knows. This is the perfect cuboid problem, and it has
been open for about three hundred years. Nobody has found a perfect box, and nobody has
proved there isn't one.

## Six out of seven

Getting six of the seven whole is not hard. In 1719 Paul Halcke found the box 44 × 117 × 240.
Its face diagonals are 125, 244 and 267, all whole. Only the space diagonal, √73225 ≈ 270.6,
misses. A box like that is called an Euler brick, and there are infinitely many of them.

Some are just bigger copies of others: 88 × 234 × 480 is Halcke's box doubled. The ones that
are not copies of anything smaller are called primitive. They are what a search needs to look
at, because a perfect box, divided down to its smallest copy, would still be perfect.

## How to look

You could try every box with edges up to some limit, but that is slow. The search here works
the other way round. Fix the shortest edge a. The other edges b and c must each make a whole
diagonal with a, so each of them must be the other leg of a right triangle with leg a. For any
number a there are only a handful of such legs, and they can be listed exactly from the factors
of a². Then the search only has to try pairs of those legs, and ask whether b² + c² is a square
too. If it is, that is an Euler brick, and then one more question settles whether it is perfect.

Done on every shortest edge from 1 up to three billion, this found 15,101 primitive Euler
bricks. None of them is perfect.

![How the number of Euler bricks grows](growth.svg)

They thin out as the numbers grow. Up to a thousand there are 14 primitive bricks; up to a
million, 608; up to a billion, 10,220; up to three billion, 15,101. Ten times the range gives
about two and a half times the bricks, and a little less each time.

![The primitive bricks up to three billion, by shortest and longest edge](bricks-3e9.svg)

Some of them are very flat: one is 87,362 times as long as it is short. One is almost a cube,
its longest edge only 2.2% longer than its shortest. The nearest miss is 640347864 ×
1045085760 × 1939671305. Its space diagonal comes within about four hundred-thousandths of the
whole number 2,294,466,039, but is not whole.

## What a computer can prove

A search can only say "none so far". But a computer can also prove things about every brick,
including the ones no search will ever reach. Take the remainders when the edges are divided
by 16, say. A real brick's edges leave some remainders, and the rules of a brick (three
diagonals whole) only allow certain combinations. There are only so many combinations to try,
so a computer can try them all. Whatever every allowed combination has in common, every brick
has too.

That is how the old rules come out by themselves: in every primitive brick one edge is divisible
by 16, another by 4, some edge by 9, some by 5, some by 11, and exactly one edge is odd. These
were found by hand long ago; here a program finds them again, as proofs. And for a perfect box
the same method shows more: one of its edges would also have to be divisible by 7, one by 19,
and some of its seven lengths by 13, 17, 29 and 37. That rules out many candidates. It does not
rule out all of them.

## How unlucky are we?

Suppose the space diagonal of each brick fell on a whole number only by chance. Near a number
N, whole squares are about 2√N apart, so a random number near N is a square with a chance of
about one in 2√N. Remainders help: some bricks have space diagonals that leave remainders no
square can leave, and could never have been perfect (more than a quarter of them); others
leave remainders that squares leave unusually often. Add up every brick's chance, and the
bricks up to three billion should have held a perfect box with a chance of about one in five.

So finding none is no surprise. More telling is where that chance sits. Most of it was spent
on the small bricks. Each tenfold increase in range adds about a quarter of what the previous
one added: there are more bricks, but their diagonals are far longer, and long numbers are
rarely squares. If that goes on, all the bricks beyond a billion together add a chance of about
three in a hundred thousand; the ones from one to three billion, searched since, added a little
over one in a hundred thousand of it. On this reckoning, a perfect box, if there is one, should have been
small enough to find by now, and probably there is none.

That is a heuristic, not a proof. Whether a diagonal is whole is not decided by chance, and a
hidden structure that made perfect boxes exist would be exactly what such a reckoning cannot
see. Some evidence that structure matters: let the box lean, so its faces are parallelograms,
and perfect ones turn up at once. Sawyer and Reiter found the first in 2009, with edges 271,
106 and 103 and all thirteen of its lengths whole; a search here finds four with edges up to
700. Whatever stops a perfect cuboid, if anything does, needs the right angles.

## What the search taught about searching

The program is written in Scala, and most of what it does is ordinary bookkeeping done
carefully. A few lessons were not obvious.

**Measure before making anything faster.** A profiler, a tool that samples what a program is
doing, said 93% of the time went into one square-root test. A more careful experiment, timing
the same loop with that test taken out, showed it was only half. The other half went into a
quick filter meant to save time: a table looked up for every pair, too big for the processor's
fastest memory. Fixing both made the search about three times as fast.

**Real data is not random.** That filter rejects pairs whose remainders cannot be a square's.
On random numbers it would reject 99%; on real pairs it rejected far fewer, because legs that
belong to the same edge share factors of 2, 3 and 5. Choosing the remainders by measuring real
pairs, not by theory, fixed it.

**Exact means exact.** The numbers involved are too big for ordinary computer arithmetic, which
silently gives wrong answers when a number gets too large. So the program checks squares with
exact 128-bit arithmetic, and a separate `verify` step checks every brick found again with
numbers of unlimited size, sharing no code with the search. Families of bricks built from
formulas (Saunderson's, from 1740, and others made from it) were checked against the search
too: it had found every one of them.

**Long runs must survive interruption.** A search of three billion edges takes half a day on a
laptop.
It writes its results in blocks, each followed by a checkpoint, so it can be stopped at any
moment, or crash, and carry on later from the last checkpoint. Tests stop it at random moments,
and cut its file at random bytes, and check that it always ends with exactly the same file.

**Fast is not the same as right.** Compiled to native machine code, the program was quick, until
it ran on several threads at once and started giving wrong answers, silently. The cause was in
the native platform's memory management, not in the program. The fix was to refuse several
threads there, and to split the work between separate programs instead.

## Where it stands

The search has gone through every primitive Euler brick with shortest edge up to three
billion, and published searches have gone much further: a perfect box would need its shortest
edge above 5·10¹¹. Nobody has found one. Nobody has proved there are none. The question is
easy to state, easy to check for any one box, and still open.

You can check a box yourself, of any size, on the project's page, or browse every brick the
search found. The code, the results and how to repeat them are in the repository.
