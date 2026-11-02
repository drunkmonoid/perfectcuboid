package perfectcuboid

import org.scalacheck.Prop.forAll
import Arith.*
import Generators.*

/** Properties: statements that must hold for every input. ScalaCheck checks each one
  * against a hundred random inputs per run, and shrinks any failure to a small example.
  */
class ArithProperties extends munit.ScalaCheckSuite:

  // BigInt never overflows, so it is a trustworthy (if slow) reference.
  property("isqrt(n) is the floor of the square root"):
    forAll(nonNegativeLong): n =>
      val r = BigInt(isqrt(n))
      r * r <= n && (r + 1) * (r + 1) > n

  property("isqrt agrees with BigInt's square root"):
    forAll(nonNegativeLong)(n => BigInt(isqrt(n)) == BigInt(BigInt(n).bigInteger.sqrt))

  property("exactSqrt recognises every square"):
    forAll(root)(r => exactSqrt(r * r) == Some(r))

  property("exactSqrt rejects the number just after a square"):
    forAll(root.filter(_ > 0).filter(_ < MaxRoot))(r => exactSqrt(r * r + 1) == None)

  property("ceilSqrt(n) is the ceiling of the square root"):
    forAll(nonNegativeLong.filter(_ <= MaxRoot * MaxRoot)): n =>
      val r = BigInt(ceilSqrt(n))
      r * r >= n && (r == 0 || (r - 1) * (r - 1) < n)

  property("gcd agrees with BigInt's gcd"):
    forAll(nonNegativeLong, nonNegativeLong)((a, b) => BigInt(gcd(a, b)) == BigInt(a).gcd(BigInt(b)))

  property("sumOfSquares is exact when it does not overflow"):
    forAll(edge, edge, edge): (a, b, c) =>
      BigInt(sumOfSquares(a, b, c)) == BigInt(a).pow(2) + BigInt(b).pow(2) + BigInt(c).pow(2)
