package perfectcuboid

/** Families of Euler bricks built forwards, from Pythagorean triples, and measured against the
  * bricks a search found: how many of those each family gives, and whether the search found
  * every member it should have.
  *
  * The only family with formulas in the usual sources (Wikipedia, MathWorld) is Saunderson's
  * of 1740; Euler's two of 1770 and 1772 are mentioned there without them. So the families
  * here are Saunderson's, the derived bricks of his, and the bricks at 2P, 3P and 4P on the
  * curves of all of those: each one a formula in the triple, though a long one.
  */
object Coverage:

  /** A family of bricks: its name, and its members in primitive form. */
  final case class Family(name: String, members: Set[BigCuboid])

  /** Every member is reduced to its primitive form. `maxHypotenuse` bounds the triples used. */
  def families(maxHypotenuse: Long): List[Family] =
    val triples = Families.primitiveTriplesUpTo(maxHypotenuse)
    val saunderson = triples.map(t => Families.saundersonBig(t).primitive).toSet
    val derived = saunderson.map(box => Derived.derived(box).primitive)
    val multiples = (saunderson ++ derived).flatMap(Origins.curveMultiples(_, 4)).map(_.primitive)
    List(
      Family("Saunderson's bricks", saunderson),
      Family("their derived bricks", derived),
      Family("2P–4P on the curves of both", multiples)
    )

  /** One family against the primitive bricks found with every edge up to `maxEdge`. */
  final case class Row(name: String, inRange: Int, found: Int)

  /** For each family, and all of them together: its members in range, and how many the search found. */
  def rows(families: List[Family], catalogue: Set[BigCuboid], maxEdge: BigInt): List[Row] =
    def inRange(members: Set[BigCuboid]) = members.filter(_.c <= maxEdge)
    val all = Family("any of these", families.flatMap(_.members).toSet)
    (families :+ all).map: family =>
      val members = inRange(family.members)
      Row(family.name, members.size, members.count(catalogue))

  /** The table, and the members the search should have found but did not, if any. */
  def lines(catalogue: Set[BigCuboid], maxEdge: BigInt, maxHypotenuse: Long): List[String] =
    val families = this.families(maxHypotenuse)
    val table = rows(families, catalogue, maxEdge)
    val missing = families.flatMap(_.members).toSet.filter(b => b.c <= maxEdge && !catalogue(b))
    List(
      s"Primitive Euler bricks with every edge <= $maxEdge: ${catalogue.count(_.c <= maxEdge)}",
      s"Families from the primitive Pythagorean triples with hypotenuse <= $maxHypotenuse:",
      "| family | members in range | found by the search |",
      "| --- | ---: | ---: |"
    ) ++ table.map(r => f"| ${r.name} | ${r.inRange}%,d | ${r.found}%,d |") ++
      (if missing.isEmpty then List("Every member in range was found by the search.")
       else
         s"${missing.size} members were NOT found by the search:" :: missing.toList.map(b =>
           s"  ${b.a} × ${b.b} × ${b.c}"
         ))
