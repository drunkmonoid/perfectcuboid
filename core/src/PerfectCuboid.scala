package perfectcuboid

/** The library in a few functions, for programs outside this project, which can depend on
  * `io.github.drunkmonoid::perfectcuboid-core` on the JVM, JavaScript or Scala Native. All the
  * rest stays public too, but these are the ones meant to stay as they are; `examples/library`
  * is a project that uses them, and CI builds it against the library as published.
  */
object PerfectCuboid:

  /** What kind of box a × b × c is, for edges of any size: perfect, an Euler brick (`Body`, only
    * its space diagonal irrational), a face cuboid, or other. None if an edge is not positive.
    */
  def kind(a: BigInt, b: BigInt, c: BigInt): Option[Kind] = BigCuboid.of(a, b, c).map(_.kind)

  /** Whether a × b × c is a perfect cuboid: all seven lengths whole. None has ever been found. */
  def isPerfect(a: BigInt, b: BigInt, c: BigInt): Boolean = BigCuboid.of(a, b, c).exists(_.isPerfect)

  /** Every Euler brick with smallest edge from `first` to `last`, however long its other edges,
    * in order of smallest edge: the search itself.
    */
  def eulerBricks(first: Long, last: Long): List[Cuboid] = FastSearch.eulerBricksBetween(first, last)

  /** The same, primitive ones only: those whose edges share no factor. */
  def primitiveEulerBricks(first: Long, last: Long): List[Cuboid] = eulerBricks(first, last).filter(_.isPrimitive)

  /** The primitive Euler bricks with their odd edge from `first` to `last`, by odd edge. */
  def primitiveEulerBricksByOddEdge(first: Long, last: Long): List[Cuboid] =
    OddSearch.primitiveBricksIterator(first, last).toList

  /** Every y with a² + y² a perfect square, in order: the other legs of the right triangles with
    * a leg a, from a's factors.
    */
  def legs(a: Long): List[Long] = Pythagorean.legsPairedWith(a)

  /** How far a brick's space diagonal is from the nearest whole number: 0 for a perfect cuboid. */
  def miss(brick: Cuboid): Double = ClosestCalls.miss(brick)
