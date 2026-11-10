package perfectcuboid

import cats.effect.IO
import fs2.io.file.Files

/** Analysing a hunt's file gives what the reports give from a fresh search. */
class AnalysisTest extends munit.CatsEffectSuite:

  test("a hunt's file is analysed like the same bricks found by a search"):
    Files[IO].tempFile.use: path =>
      for
        _ <- Hunt.run(Quarry.eulerBricks, path, 30_000, blockSize = 1000, cores = 4)
        lines <- Analysis.ofFile(Quarry.eulerBricks, path, primitiveOnly = false, charts = None)
      yield
        assertEquals(lines.head, "2,636 Euler bricks with smallest edge <= 30,000")
        assert(lines.contains("| 10^3 | 69 | 14 |") && lines.contains("| 10^4 | 849 | 56 |"), lines)
        assert(lines.contains("All rules hold for all 2636 Euler bricks with smallest edge <= 30,000."), lines)
        // The same rows as the reports that search for themselves, under their own headings.
        def rows(report: Seq[String]) = report.filter(_.startsWith("  "))
        val closest = rows(Cli.closestReport(30_000))
        assert(lines.containsSlice(closest), lines)
        assert(lines.containsSlice(rows(Cli.originsReport(30_000))), lines)

  test("a shard is analysed without the growth table, which needs edges from 1"):
    val bricks = FastSearch.eulerBricksBetween(10_001, 20_000).toVector
    val lines = Analysis.report(bricks, 10_001, 20_000, primitiveOnly = false)
    assertEquals(lines.head, s"${bricks.size} Euler bricks with smallest edge from 10,001 to 20,000")
    assert(!lines.exists(_.startsWith("| 10^")))

  test("a primitive hunt's growth table has only the primitive column"):
    val bricks = FastSearch.eulerBricksBetween(1, 10_000).filter(_.isPrimitive).toVector
    assertEquals(
      Analysis.growth(bricks, 10_000, primitiveOnly = true),
      List("| smallest edge <= N | primitive |", "| --- | ---: |", "| 10^3 | 14 |", "| 10^4 | 56 |") :+
        f"Growth exponent k of the primitive count: ${Stats.exponent(1000, 14, 10_000, 56)}%.2f"
    )
