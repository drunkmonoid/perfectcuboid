package perfectcuboid

/** The program's entry point, and the only place with side effects (printing).
  * Everything it calls is pure.
  *
  *   scala-cli run .                    Euler bricks with edges up to 1000
  *   scala-cli run . -- bricks 5000     ... up to another limit
  *   scala-cli run . -- rules 100000    check the known rules against every brick found
  */
@main def explore(args: String*): Unit =
  println(Cli.run(args.toList))

object Cli:

  /** Turns the command line arguments into the text to print. Pure, so it can be tested. */
  def run(args: List[String]): String = args match
    case "rules" :: rest  => rulesReport(limit(rest))
    case "bricks" :: rest => bricksReport(limit(rest))
    case rest             => bricksReport(limit(rest))

  private def limit(args: List[String]): Long =
    args.headOption.flatMap(_.toLongOption).getOrElse(1000L)

  def bricksReport(limit: Long): String =
    val bricks = Search.eulerBricks(limit)
    val lines =
      s"Euler bricks with every edge <= $limit: ${bricks.size} (* = primitive)"
        +: bricks.map(describe)
        :+ verdict(bricks.filter(_.isPerfect))
    lines.mkString("\n")

  def rulesReport(limit: Long): String =
    val bricks = Search.eulerBricks(limit)
    val broken =
      for
        brick <- bricks
        rule <- Theory.violations(Theory.rulesFor(brick), brick)
      yield s"  ${describe(brick)} breaks: ${rule.name}"
    val ruleNames = (Theory.eulerBrickRules ++ Theory.primitiveEulerBrickRules).map(r => s"  - ${r.name}")
    val summary =
      if broken.isEmpty then s"All rules hold for all ${bricks.size} Euler bricks with every edge <= $limit."
      else s"${broken.size} violations:"
    ("Rules checked:" +: ruleNames :+ summary :++ broken).mkString("\n")

  def describe(cuboid: Cuboid): String =
    val edges = s"${cuboid.a} × ${cuboid.b} × ${cuboid.c}"
    val faces = cuboid.faceDiagonals.flatten.mkString(", ")
    val mark = if cuboid.isPrimitive then "*" else " "
    f"$mark $edges%-20s face diagonals $faces"

  def verdict(perfect: Vector[Cuboid]): String =
    if perfect.isEmpty then "No perfect cuboid (as expected)."
    else s"PERFECT CUBOID FOUND: ${perfect.mkString(", ")}"
