package perfectcuboid

/** Local obstructions: what an Euler brick or a perfect cuboid must look like modulo a power
  * of a prime, worked out by trying every case.
  *
  * Take a primitive brick and reduce its edges a, b, c mod m = pᵏ. Not all three are divisible
  * by p (that is what primitive means), and a² + b², a² + c², b² + c² are squares, so they
  * must be squares mod m as well. There are only m³ triples of remainders, so a computer can
  * list every one that passes, and see what they have in common. Whatever all of them share,
  * every primitive brick has too. That is a proof, not an observation: it covers bricks no
  * search will ever reach.
  *
  * What they share is told by valuations: how many times p divides each of the 7 lengths.
  */
object Local:

  /** How many times p divides x mod pᵏ, where 0 counts as k: then all we know is "at least k". */
  def valuation(x: Int, p: Int, k: Int): Int =
    if x == 0 then k else if x % p == 0 then 1 + valuation(x / p, p, k - 1) else 0

  /** The valuations of the lengths of one brick, each list largest first. A diagonal is
    * known only through its square s, and p divides it ⌊v(s)/2⌋ times or more.
    */
  final case class Pattern(edges: List[Int], faces: List[Int], space: Int)

  /** Every pattern that a primitive Euler brick (or, if `perfect`, a perfect cuboid) can show
    * modulo pᵏ. Every count is a lower bound, which is all the rules below need.
    *
    * Everything here depends on an edge x only through x² mod pᵏ and the valuation of x, so
    * the remainders are first sorted into classes by those two, and only one triple of
    * classes is tried for all the triples of remainders in them: about m/2 classes instead of
    * m remainders, eight times fewer triples.
    */
  def patterns(p: Int, k: Int, perfect: Boolean): Set[Pattern] =
    val m = BigInt(p).pow(k).toInt
    val isSquare = new Array[Boolean](m)
    for x <- 0 until m do isSquare((x.toLong * x % m).toInt) = true
    def diagonal(square: Int) = valuation(square, p, k) / 2
    // (x² mod m, valuation of x) for every remainder x, without repeats.
    val classes = (0 until m).map(x => ((x.toLong * x % m).toInt, valuation(x, p, k))).distinct.toVector
    val found =
      for
        i <- classes.indices
        j <- i until classes.size
        l <- j until classes.size // the edges in any order give the same pattern, so i <= j <= l is enough
        (aa, va) = classes(i)
        (bb, vb) = classes(j)
        (cc, vc) = classes(l)
        if va == 0 || vb == 0 || vc == 0
        faces = List((aa + bb) % m, (aa + cc) % m, (bb + cc) % m)
        if faces.forall(isSquare)
        space = ((aa.toLong + bb + cc) % m).toInt
        if !perfect || isSquare(space)
      yield Pattern(List(va, vb, vc).sorted.reverse, faces.map(diagonal).sorted.reverse, diagonal(space))
    found.toSet

  /** How often each pattern of edge valuations should occur among primitive bricks, if they
    * were spread like the solutions mod pᵏ: the local density.
    *
    * Mod m = pᵏ, the edges a, b, c have N(a² + b²) · N(a² + c²) · N(b² + c²) solutions for their
    * face diagonals, N(r) being the number of square roots of r. Adding that up over every
    * triple of remainders, not all divisible by p, sorted by how often p divides each edge
    * (largest first, and at most `cap`), and dividing by the total, gives each pattern's share.
    *
    * k must be well above `cap`: whether a sum of two squares divisible by p²ᵛ is a square shows
    * only in its digits beyond that. (Mod 2⁸, 2⁶ · 5 = 320 ≡ 64 looks like a square, but 5 is
    * not a square mod 8, so 2⁶ · 5 is not one.) So, as in `patterns`, the remainders are
    * grouped into classes with the same square and valuation, which keeps large k affordable.
    * A heuristic: nothing says the bricks found must follow it.
    */
  def densities(p: Int, k: Int, cap: Int): Map[List[Int], Double] =
    val m = BigInt(p).pow(k).toInt
    val roots = new Array[Long](m)
    for x <- 0 until m do roots((x.toLong * x % m).toInt) += 1
    // (x² mod m, capped valuation of x) -> how many remainders x have them.
    val classes = (0 until m)
      .groupMapReduce(x => ((x.toLong * x % m).toInt, valuation(x, p, k) min cap))(_ => 1L)(_ + _)
      .toVector
    val n = classes.size
    val (squares, capped, sizes) =
      (classes.map(_._1._1).toArray, classes.map(_._1._2).toArray, classes.map(_._2).toArray)
    val weights = scala.collection.mutable.Map.empty[List[Int], Double].withDefaultValue(0.0)
    for i <- 0 until n; j <- i until n do
      val ij = roots((squares(i) + squares(j)) % m)
      if ij > 0 then
        var l = j
        while l < n do
          if capped(i) == 0 || capped(j) == 0 || capped(l) == 0 then
            val faces = ij * roots((squares(i) + squares(l)) % m) * roots((squares(j) + squares(l)) % m)
            if faces > 0 then
              // The ordered triples of remainders that this unordered triple of classes stands for.
              val orders = if i == j && j == l then 1 else if i == j || j == l then 3 else 6
              val pattern = List(capped(i), capped(j), capped(l)).sorted.reverse
              weights(pattern) = weights(pattern) + orders.toDouble * sizes(i) * sizes(j) * sizes(l) * faces
          l += 1
    val total = weights.values.sum
    weights.view.mapValues(_ / total).toMap

  /** The same pattern for an actual brick: how often p divides each edge, at most `cap`. */
  def patternOf(brick: Cuboid, p: Int, cap: Int): List[Int] =
    def times(x: Long): Int = if x % p == 0 && cap > 0 then 1 + (times(x / p) min (cap - 1)) else 0
    List(brick.a: Long, brick.b, brick.c).map(times).sorted.reverse

  /** The primes whose patterns `densityLines` counts, each as (p, k, cap): solutions mod pᵏ,
    * and how often p divides an edge counted up to `cap`.
    */
  val densityCases: List[(Int, Int, Int)] = List((2, 13, 5), (3, 7, 2), (5, 5, 2), (7, 4, 1), (11, 3, 1), (13, 3, 1))

  /** `densities` for each of `densityCases`, worked out once: mod 2¹³ it takes seconds. */
  lazy val caseDensities: Map[(Int, Int, Int), Map[List[Int], Double]] =
    densityCases.map(c => c -> densities(c._1, c._2, c._3)).toMap

  /** For each prime, its patterns' predicted and observed shares among these primitive bricks. */
  def densityLines(bricks: Seq[Cuboid]): List[String] =
    val primitive = bricks.filter(_.isPrimitive)
    s"How often p divides each edge, among ${primitive.size} primitive bricks, against the local densities:" ::
      densityCases.flatMap: (p, k, cap) =>
        val predicted = caseDensities((p, k, cap))
        val observed = primitive.groupBy(patternOf(_, p, cap)).view.mapValues(_.size).toMap
        def show(pattern: List[Int]) =
          pattern.map(v => if v == cap then s"$v+" else v.toString).mkString("(", ", ", ")")
        val patterns =
          (predicted.keySet ++ observed.keySet).toList.sortBy(pattern => -predicted.getOrElse(pattern, 0.0))
        s"p = $p, from the solutions mod $p^$k:" ::
          patterns.map: pattern =>
            val share = observed.getOrElse(pattern, 0).toDouble / primitive.size
            f"  ${show(pattern)}%-14s predicted ${100 * predicted.getOrElse(pattern, 0.0)}%5.1f%%   found ${100 * share}%5.1f%%  (${observed.getOrElse(pattern, 0)}%,d)"

  /** Which lengths a rule talks about. */
  enum Lengths(val name: String, val of: Pattern => List[Int]):
    case Edges extends Lengths("edge", _.edges)
    case EdgesOrSpace extends Lengths("edge or the space diagonal", p => p.space :: p.edges)
    case EdgesOrFaces extends Lengths("edge or face diagonal", p => p.edges ++ p.faces)
    case Any extends Lengths("of the seven lengths", p => p.space :: p.edges ++ p.faces)

  /** What every pattern shares, as rules.
    *
    * First, edges: sort each pattern's edge valuations, largest first, and take the smallest
    * in each place. If that is (4, 2, 0), every brick has one edge divisible by 2⁴ and another
    * by 2². Then, if no edge has to be divisible by p, the narrowest set of lengths that always
    * contains a multiple of p.
    */
  def rules(p: Int, k: Int, perfect: Boolean): List[Rule] =
    val found = patterns(p, k, perfect)
    if found.isEmpty then List(Rule(s"impossible mod $p^$k", _ => false))
    else
      val forced = found.map(_.edges).reduce((x, y) => x.lazyZip(y).map(math.min))
      val edgeRule = Option.when(forced.head > 0)(edgeDivisibility(p, forced.filter(_ > 0)))
      val wider =
        if edgeRule.isDefined then None
        else
          Lengths.values
            .find(lengths => found.forall(pattern => lengths.of(pattern).exists(_ > 0)))
            .map(lengths => someLengthDivisibleBy(p, lengths))
      (edgeRule ++ wider).toList

  /** "One edge divisible by p^e₁, another by p^e₂, …", for valuations e₁ >= e₂ >= …. */
  private def edgeDivisibility(p: Int, valuations: List[Int]): Rule =
    val divisors = valuations.map(BigInt(p).pow(_).toLong)
    val name = divisors.zipWithIndex
      .map((d, i) => if i == 0 then s"one edge divisible by $d" else s"another by $d")
      .mkString(", ")
    Rule(
      name,
      cuboid =>
        // Some way of giving the edges to the divisors, a different edge to each, works.
        List(cuboid.a: Long, cuboid.b, cuboid.c).permutations.exists(edges => edges.zip(divisors).forall(_ % _ == 0))
    )

  private def someLengthDivisibleBy(p: Int, lengths: Lengths): Rule =
    Rule(
      s"some ${lengths.name} divisible by $p",
      cuboid =>
        val edges = List(cuboid.a: Long, cuboid.b, cuboid.c)
        val faces = cuboid.faceDiagonals.flatten.map(d => d: Long)
        val space = cuboid.spaceDiagonal.map(d => d: Long).toList
        val candidates = lengths match
          case Lengths.Edges        => edges
          case Lengths.EdgesOrSpace => space ++ edges
          case Lengths.EdgesOrFaces => edges ++ faces
          case Lengths.Any          => space ++ edges ++ faces
        candidates.exists(_ % p == 0)
    )

  /** A rule in the words of a certificate (`certificate`), which `checker` reads. */
  def ruleWords(p: Int, k: Int, perfect: Boolean): List[String] =
    val found = patterns(p, k, perfect)
    if found.isEmpty then List("impossible")
    else
      val forced = found.map(_.edges).reduce((x, y) => x.lazyZip(y).map(math.min))
      if forced.head > 0 then List(("edges" :: forced.filter(_ > 0).map(_.toString)).mkString(" "))
      else
        val words = Map(
          Lengths.Edges -> "edge",
          Lengths.EdgesOrSpace -> "edge-or-space",
          Lengths.EdgesOrFaces -> "edge-or-face",
          Lengths.Any -> "any"
        )
        Lengths.values
          .find(lengths => found.forall(pattern => lengths.of(pattern).exists(_ > 0)))
          .map(lengths => s"some ${words(lengths)}")
          .toList

  /** The rules for every prime up to `maxPrime`, written out with the evidence for them: every
    * pattern found mod pᵏ, for bricks and for perfect cuboids. `checker` (a separate module,
    * sharing no code with this one) finds the patterns again its own way and checks the rules.
    */
  def certificate(maxPrime: Int): List[String] =
    val primes = (2 to maxPrime).filter(n => (2 until n).forall(n % _ != 0)).toList
    List(
      s"# The rules of `perfectcuboid local` for every prime up to $maxPrime, with their evidence.",
      "# Each case: every pattern of valuations (edges | face diagonals | space diagonal), largest",
      "# first, that a primitive brick, or a perfect cuboid, shows mod p^k; then what all share.",
      "# Checked by `./mill checker.run FILE`, which shares no code with the program that wrote it."
    ) ++ primes.flatMap: prime =>
      val (q, k) = modulus(prime)
      List(false, true).flatMap: perfect =>
        val kind = if perfect then "perfect" else "brick"
        val lines = patterns(q, k, perfect).toList
          .map(pt => s"pattern ${pt.edges.mkString(" ")} | ${pt.faces.mkString(" ")} | ${pt.space}")
          .sorted
        (s"case $q $k $kind" :: lines) ++ ruleWords(q, k, perfect).map("rule " + _)

  /** The prime power tried for each prime: a high power of 2, and the square of the others.
    * Lower powers miss rules: mod 2⁷ only shows "8 and 4", mod 17 nothing at all.
    */
  def modulus(p: Int): (Int, Int) = if p == 2 then 2 -> 8 else if p == 3 then 3 -> 4 else p -> 2
