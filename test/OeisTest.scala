package perfectcuboid

/** The published sequences of primitive Euler bricks, from the On-Line Encyclopedia of Integer
  * Sequences, as fixtures. A031173, A031174 and A031175 give the longest, middle and shortest
  * edges of the primitive bricks in order of longest edge; the three lists have 37, 38 and 42
  * terms, so the first 37 bricks are complete.
  */
class OeisTest extends munit.FunSuite:

  val longest = List(240, 275, 693, 720, 792, 1155, 1584, 2340, 2640, 2992, 3120, 5984, 6325, 6336, 6688, 6732, 8160,
    9120, 9405, 10725, 11220, 12075, 13860, 14560, 16800, 17472, 17748, 18560, 19305, 21476, 23760, 23760, 24684, 25704,
    26649, 29920, 30780) // A031173

  val middle = List(117, 252, 480, 132, 231, 1100, 1020, 880, 855, 2475, 2035, 2295, 5796, 748, 6300, 4576, 4888, 1672,
    9152, 9828, 3536, 1100, 4901, 1881, 11753, 8820, 10560, 16929, 15400, 14160, 18368, 17157, 24080, 17472, 15232,
    23751, 10725, 13728) // A031174

  val shortest = List(44, 240, 140, 85, 160, 1008, 187, 429, 832, 780, 828, 1560, 528, 195, 1155, 1755, 495, 1575, 2964,
    7840, 2925, 1008, 4368, 1080, 10296, 7579, 8789, 6072, 14112, 5643, 4599, 4900, 6435, 935, 7920, 7800, 4928, 7560,
    23760, 1105, 2163, 2964) // A031175

  val published: List[Cuboid] =
    longest.lazyZip(middle).lazyZip(shortest).toList.map((a, b, c) => Cuboid.of(a, b, c).get)

  test("every published brick is a primitive Euler brick"):
    published.foreach(brick => assert(brick.isEuler && brick.isPrimitive, brick))

  test("the bounded search finds exactly the published bricks, up to the 37th longest edge"):
    val found = Search.eulerBricks(longest.last).filter(_.isPrimitive)
    // Two bricks share the longest edge 23760, listed in a different order there, so compare sets.
    assertEquals(found.toSet, published.toSet)
    assertEquals(found.map(_.c: Long).toList, longest.map(_.toLong))

  test("the unbounded search finds them too, with others whose longest edge is larger"):
    val found = FastSearch.eulerBricksBetween(1, 30_780).filter(_.isPrimitive).toSet
    assert(published.toSet.subsetOf(found))
