package perfectcuboid

class OriginsTest extends munit.FunSuite:

  test("exact cube roots"):
    assertEquals(Origins.exactCbrt(BigInt(125)), Some(BigInt(5)))
    assertEquals(Origins.exactCbrt(BigInt(10).pow(30)), Some(BigInt(10).pow(10)))
    assertEquals(Origins.exactCbrt(BigInt(126)), None)

  test("Saunderson's bricks have a face diagonal w³"):
    for t <- Families.primitiveTriples.take(200) do
      val brick = Families.saundersonBig(t)
      val faces = brick.faceDiagonals.flatten
      assert(faces.exists(d => Origins.exactCbrt(d).contains(BigInt(t.c))), t)
      assert(Origins.isSaunderson(brick), t)

  test("the test for Saunderson bricks agrees with generating them"):
    val primitive = Search.eulerBricks(100_000).filter(_.isPrimitive).map(BigCuboid.from)
    val generated = Families.saundersonBricksUpTo(100_000).filter(_.isPrimitive).map(BigCuboid.from).toSet
    assertEquals(primitive.filter(Origins.isSaunderson).toSet, generated)
    assertEquals(generated.size, 7) // as RESULTS.md says for every edge <= 10⁵

  test("412425 × 4267872 × 4657400 is a curve multiple of 240 × 252 × 275"):
    val child = BigCuboid.of(412_425, 4_267_872, 4_657_400).get
    assert(Origins.curveMultiples(BigCuboid.of(240, 252, 275).get, 2).contains(child))
    assert(Origins.curveMultiples(BigCuboid.of(1008, 1100, 1155).get, 2).contains(child))

  test("classifying the first bricks"):
    val bricks = Vector((44, 117, 240), (240, 252, 275), (429, 880, 2340)).map((a, b, c) => BigCuboid.of(a, b, c).get)
    val origins = Origins.classify(bricks).toMap
    assert(origins(bricks(0)).saunderson && !origins(bricks(0)).derivedFromSmaller)
    assert(origins(bricks(2)).derivedFromSmaller) // the partner of 44 × 117 × 240
