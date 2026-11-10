package perfectcuboid

/** A named statement about a cuboid. The check is an ordinary function stored as a value. */
final case class Rule(name: String, holds: Cuboid => Boolean)

/** Known facts about Euler bricks, written as rules so they can be checked against
  * everything the searches find. A perfect cuboid is an Euler brick, so it must obey them too,
  * which is how real searches prune their candidates.
  */
object Theory:

  private def edges(c: Cuboid): List[Long] = List(c.a, c.b, c.c)

  /** True when two different edges are divisible by p and q respectively. */
  private def twoEdgesDivisibleBy(p: Long, q: Long)(c: Cuboid): Boolean =
    val es = edges(c)
    es.indices.exists(i => es.indices.exists(j => i != j && es(i) % p == 0 && es(j) % q == 0))

  private def someEdgeDivisibleBy(d: Long)(c: Cuboid): Boolean =
    edges(c).exists(_ % d == 0)

  /** 2⁶·3³·5·11: what the divisibility rules below force into the product of the edges. */
  val EdgeProductDivisor: Long = 95040L

  /** Rules for every Euler brick. Scaling a brick up keeps them true. */
  val eulerBrickRules: List[Rule] = List(
    Rule("one edge divisible by 16, another by 4", twoEdgesDivisibleBy(16, 4)),
    Rule("one edge divisible by 9, another by 3", twoEdgesDivisibleBy(9, 3)),
    Rule("some edge divisible by 5", someEdgeDivisibleBy(5)),
    Rule("some edge divisible by 11", someEdgeDivisibleBy(11)),
    Rule(
      s"product of the edges divisible by $EdgeProductDivisor",
      c => edges(c).map(BigInt(_)).product % EdgeProductDivisor == 0
    )
  )

  /** Rules for primitive Euler bricks only: scaling by 2 would make every edge even. */
  val primitiveEulerBrickRules: List[Rule] = List(
    Rule("exactly one edge is odd", c => edges(c).count(_ % 2 != 0) == 1),
    Rule("exactly one face diagonal is even", c => c.faceDiagonals.flatten.count(_ % 2 == 0) == 1)
  )

  /** The rules that apply to this cuboid, given that it is an Euler brick. */
  def rulesFor(c: Cuboid): List[Rule] =
    if c.isPrimitive then eulerBrickRules ++ primitiveEulerBrickRules else eulerBrickRules

  /** The rules a cuboid breaks. */
  def violations(rules: List[Rule], c: Cuboid): List[Rule] =
    rules.filterNot(_.holds(c))
