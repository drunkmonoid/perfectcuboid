package perfectcuboid

class OeisFilesTest extends munit.FunSuite:

  val bricks: List[Cuboid] = FastSearch.eulerBricksBetween(1, 60_000)

  /** The first 37 rows of A031173-5, as `OeisTest` keeps them, in their published order. */
  val published: List[OeisFiles.Row] =
    val test = OeisTest()
    test.longest
      .lazyZip(test.middle)
      .lazyZip(test.shortest)
      .toList
      .map: (l, m, s) =>
        (longest = l.toLong, middle = m.toLong, shortest = s.toLong)

  test("the published rows come first, as they are, and the rest after them in order"):
    val rows = OeisFiles.rows(bricks, 60_000, published).toOption.get
    assertEquals(rows.take(published.size), published)
    val later = rows.drop(published.size)
    assert(later.nonEmpty)
    assert(later.forall(_.longest > published.last.longest))
    assertEquals(later, later.sortBy(r => (r.longest, r.middle)))
    assertEquals(rows.toSet, bricks.filter(b => b.isPrimitive && b.c <= 60_000).map(OeisFiles.row).toSet)

  test("a file that disagrees with the published rows makes no b-files"):
    val missing = bricks.filterNot(b => (b.a: Long) == 44 && (b.b: Long) == 117)
    assert(OeisFiles.rows(missing, 60_000, published).isLeft)

  test("b-file lines are numbered from 1, after their comments"):
    val lines = OeisFiles.bfile("031173", "longest edges", List(240L, 275L), List("a note"))
    assertEquals(
      lines,
      List("# A031173: longest edges, in order of longest edge, for n = 1..2.", "# a note", "1 240", "2 275")
    )

  test("published rows are read from b-file lines"):
    val rows = OeisFiles.published(List("1 240", "2 275"), List("1 117", "2 252"), List("# x", "1 44", "2 240"))
    assertEquals(
      rows,
      List((longest = 240L, middle = 117L, shortest = 44L), (longest = 275L, middle = 252L, shortest = 240L))
    )
