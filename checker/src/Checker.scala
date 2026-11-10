package checker

import scala.io.Source

/** Checks the certificates that `perfectcuboid certificates` writes for the rules of `local`,
  * without any of the program's code: this module depends on nothing else in the project.
  *
  *   ./mill checker.run docs/local-certificates.txt
  *
  * For each case, p, k and brick or perfect, it tries every triple of remainders a ≤ b ≤ c mod
  * m = pᵏ, not all divisible by p, keeps those whose face diagonals' squares (and, for a perfect
  * cuboid, the space diagonal's) are squares mod m, and writes down how often p divides each
  * length. That set must be exactly the certificate's patterns. Then each rule must hold for
  * every pattern; since the patterns are all a brick can show mod m, it holds for every brick.
  */
object Checker:

  /** How often p divides x, taken mod m = pᵏ: 0 counts k times. */
  def times(x: Int, p: Int, k: Int): Int =
    var n = 0
    var y = x
    while n < k && y % p == 0 do
      y /= p
      n += 1
    n

  /** Patterns as text, as the certificate writes them. */
  def patterns(p: Int, k: Int, perfect: Boolean): Set[String] =
    var m = 1
    for _ <- 1 to k do m *= p
    val square = new Array[Boolean](m)
    for x <- 0 until m do square((x.toLong * x % m).toInt) = true
    val sq = Array.tabulate(m)(x => (x.toLong * x % m).toInt)
    val v = Array.tabulate(m)(x => times(x, p, k))
    val found = scala.collection.mutable.Set.empty[String]
    for a <- 0 until m; b <- a until m do
      val ab = (sq(a) + sq(b)) % m
      if square(ab) then
        for c <- b until m do
          if v(a) == 0 || v(b) == 0 || v(c) == 0 then
            val ac = (sq(a) + sq(c)) % m
            val bc = (sq(b) + sq(c)) % m
            val abc = (ab + sq(c)) % m
            if square(ac) && square(bc) && (!perfect || square(abc)) then
              val edges = List(v(a), v(b), v(c)).sorted.reverse
              // A diagonal is known through its square, which p divides at least 2·t times.
              val faces = List(ab, ac, bc).map(s => times(s, p, k) / 2).sorted.reverse
              found += s"${edges.mkString(" ")} | ${faces.mkString(" ")} | ${times(abc, p, k) / 2}"
    found.toSet

  final case class Pattern(edges: List[Int], faces: List[Int], space: Int)

  def parse(text: String): Pattern =
    val List(e, f, s) = text.split('|').toList.map(_.trim): @unchecked
    Pattern(e.split(' ').toList.map(_.toInt), f.split(' ').toList.map(_.toInt), s.toInt)

  /** Whether a rule, in the certificate's words, holds for every one of the patterns. */
  def holds(rule: String, patterns: Set[Pattern]): Boolean = rule.split(' ').toList match
    case List("impossible") => patterns.isEmpty
    case "edges" :: vs      =>
      val need = vs.map(_.toInt)
      patterns.nonEmpty && patterns.forall(pt => pt.edges.zip(need).forall(_ >= _))
    case List("some", which) =>
      def lengths(pt: Pattern): List[Int] = which match
        case "edge"          => pt.edges
        case "edge-or-space" => pt.space :: pt.edges
        case "edge-or-face"  => pt.edges ++ pt.faces
        case "any"           => pt.space :: pt.edges ++ pt.faces
        case _               => Nil
      patterns.nonEmpty && patterns.forall(pt => lengths(pt).exists(_ > 0))
    case _ => false

  /** What is wrong with the certificate, case by case: nothing, if it is right. */
  def problems(lines: List[String]): (cases: Int, rules: Int, problems: List[String]) =
    val body = lines.filterNot(l => l.trim.isEmpty || l.startsWith("#"))
    val starts = body.zipWithIndex.collect { case (l, i) if l.startsWith("case ") => i } :+ body.size
    val cases = starts.zip(starts.drop(1)).map((from, until) => body.slice(from, until))
    val found = cases.map: c =>
      val List(_, p, k, kind) = c.head.split(' ').toList: @unchecked
      val claimed = c.collect { case l if l.startsWith("pattern ") => l.stripPrefix("pattern ") }.toSet
      val rules = c.collect { case l if l.startsWith("rule ") => l.stripPrefix("rule ") }
      val actual = patterns(p.toInt, k.toInt, kind == "perfect")
      val wrongPatterns =
        if actual == claimed then Nil
        else
          List(
            s"${c.head}: ${(actual -- claimed).size} patterns missing, ${(claimed -- actual).size} that cannot occur"
          )
      val wrongRules = rules.filterNot(r => holds(r, actual.map(parse))).map(r => s"${c.head}: rule $r does not hold")
      (rules = rules.size, problems = wrongPatterns ++ wrongRules)
    (cases = cases.size, rules = found.map(_.rules).sum, problems = found.flatMap(_.problems))

  def main(args: Array[String]): Unit =
    val file = args.headOption.getOrElse("docs/local-certificates.txt")
    val source = Source.fromFile(file, "UTF-8")
    val lines =
      try source.getLines().toList
      finally source.close()
    val result = problems(lines)
    if result.problems.isEmpty then println(s"$file: ${result.cases} cases and ${result.rules} rules, all checked: OK")
    else
      result.problems.foreach(println)
      sys.exit(1)
