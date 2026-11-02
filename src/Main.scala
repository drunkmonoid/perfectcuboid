package perfectcuboid

/** The program's entry point, and the only place with side effects (printing).
  * Everything it calls is pure.
  *
  *   scala-cli run .                          Euler bricks with edges up to 1000
  *   scala-cli run . -- bricks 5000           ... up to another limit
  *   scala-cli run . -- unbounded 100000      Euler bricks by smallest edge, however big the others
  *   scala-cli run . -- rules 100000          check the known rules against every brick found
  *   scala-cli run . -- saunderson 10000000   Saunderson's bricks, from a formula, no search
  *   scala-cli run . -- legs 240              factorise 240 and list every leg it pairs with
  */
@main def explore(args: String*): Unit =
  // Going through an iterator lets each line be forgotten once printed, so a long
  // search never holds all of its output in memory.
  Cli.run(args.toList).iterator.foreach(println)

object Cli:

  /** Turns the command line arguments into the lines to print. Pure, so it can be tested.
    *
    * The lines form a LazyList: each one is only computed when it is about to be printed,
    * so long searches show their results as they go.
    */
  def run(args: List[String]): LazyList[String] = args match
    case "rules" :: rest      => rulesReport(limit(rest))
    case "bricks" :: rest     => bricksReport(limit(rest))
    case "unbounded" :: rest  => unboundedReport(limit(rest))
    case "saunderson" :: rest => saundersonReport(limit(rest))
    case "legs" :: rest       => legsReport(limit(rest))
    case rest                 => bricksReport(limit(rest))

  private def limit(args: List[String]): Long =
    args.headOption.flatMap(_.toLongOption).getOrElse(1000L)

  def bricksReport(limit: Long): LazyList[String] =
    val bricks = Search.eulerBricks(limit)
    s"Euler bricks with every edge <= $limit: ${bricks.size} (* = primitive)"
      #:: bricks.map(describe).to(LazyList)
      :+ verdict(bricks.filter(_.isPerfect))

  /** Nothing here keeps the bricks once they are described, so memory use stays flat. */
  def unboundedReport(maxSmallestEdge: Long): LazyList[String] =
    val bricks = Search.allEulerBricks.takeWhile(_.a <= maxSmallestEdge)
    s"Euler bricks with smallest edge <= $maxSmallestEdge, other edges up to ${Edge.Max} (* = primitive)"
      #:: bricks.map(brick => if brick.isPerfect then s"${describe(brick)}  PERFECT!" else describe(brick))
      :+ s"Done: every smallest edge up to $maxSmallestEdge searched."

  def rulesReport(limit: Long): LazyList[String] =
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
    ("Rules checked:" +: ruleNames :+ summary :++ broken).to(LazyList)

  def saundersonReport(limit: Long): LazyList[String] =
    val bricks = Families.saundersonBricksUpTo(limit)
    s"Saunderson bricks with every edge <= $limit: ${bricks.size} (* = primitive)"
      #:: bricks.map(describe).to(LazyList)
      :+ verdict(bricks.filter(_.isPerfect).toVector)

  def legsReport(a: Long): LazyList[String] =
    if a < 1 || a > Edge.Max then LazyList(s"legs: give a number from 1 to ${Edge.Max}")
    else
      val factors = NumberTheory
        .factorize(a)
        .map((p, k) => if k == 1 then s"$p" else s"$p^$k")
        .mkString(" · ")
      val legs = Pythagorean.legsPairedWith(a)
      // y can be up to a²/2, so y² needs BigInt.
      def hypotenuse(y: Long) = (BigInt(a).pow(2) + BigInt(y).pow(2)).bigInteger.sqrt
      s"$a = $factors"
        #:: s"${legs.size} legs pair with $a:"
        #:: legs.map(y => s"  $a² + $y² = ${hypotenuse(y)}²").to(LazyList)

  def describe(cuboid: Cuboid): String =
    val edges = s"${cuboid.a} × ${cuboid.b} × ${cuboid.c}"
    val faces = cuboid.faceDiagonals.flatten.mkString(", ")
    val mark = if cuboid.isPrimitive then "*" else " "
    f"$mark $edges%-20s face diagonals $faces"

  def verdict(perfect: Vector[Cuboid]): String =
    if perfect.isEmpty then "No perfect cuboid (as expected)."
    else s"PERFECT CUBOID FOUND: ${perfect.mkString(", ")}"
