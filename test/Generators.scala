package perfectcuboid

import org.scalacheck.Gen

/** Random inputs for the property tests. A `Gen[A]` describes how to pick random values of type A. */
object Generators:

  /** The largest r whose square still fits in a Long. */
  val MaxRoot: Long = 3_037_000_499L

  val nonNegativeLong: Gen[Long] = Gen.chooseNum(0L, Long.MaxValue)

  val root: Gen[Long] = Gen.chooseNum(0L, MaxRoot)

  /** Edges small enough that every diagonal of the cuboid fits in a Long. */
  val edge: Gen[Long] = Gen.chooseNum(1L, 1_000_000_000L)

  def cuboidWithEdgesUpTo(max: Long): Gen[Cuboid] =
    val edge = Gen.chooseNum(1L, max)
    for
      a <- edge
      b <- edge
      c <- edge
    yield Cuboid.of(a, b, c).get

  val cuboid: Gen[Cuboid] = cuboidWithEdgesUpTo(1_000_000_000L)

  /** Cuboids that can be scaled up by 1000 and still have diagonals that fit in a Long. */
  val smallCuboid: Gen[Cuboid] = cuboidWithEdgesUpTo(1_000_000L)

  /** Known Euler bricks and their multiples, since random cuboids are almost never interesting. */
  val eulerBrick: Gen[Cuboid] =
    for
      (a, b, c) <- Gen.oneOf((44L, 117L, 240L), (85L, 132L, 720L), (140L, 480L, 693L), (160L, 231L, 792L), (240L, 252L, 275L))
      k <- Gen.chooseNum(1L, 1000L)
    yield Cuboid.of(k * a, k * b, k * c).get
