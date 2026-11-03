package perfectcuboid

/** Factorising a whole block of consecutive numbers at once, with a segmented sieve.
  *
  * Trial division asks of each number in turn: is it divisible by 2? by 3? by 5? … and most
  * answers are no. A sieve turns the question round: for each prime p, it goes straight to
  * the multiples of p in the block, one every p numbers, and divides p out of those only.
  * Every division it makes succeeds.
  *
  * After every prime up to √last has been divided out, what is left of each number is either
  * 1 or a single prime above √last: two such primes would multiply to more than `last`.
  */
object Sieve:

  /** The largest `last` this can handle: its square root must be within the small primes. */
  val MaxLast: Long = NumberTheory.smallPrimes.last * NumberTheory.smallPrimes.last

  /** The factorisations of first, first + 1, …, last, in that order. Same results as calling
    * `NumberTheory.factorize` on each.
    *
    * Like the sieve of the small primes, this works in local mutable arrays and returns an
    * immutable result, so from the outside it is pure.
    */
  def factorizeBlock(first: Long, last: Long): IArray[NumberTheory.Factorization] =
    require(first >= 1 && first <= last && last <= MaxLast, s"cannot sieve $first to $last")
    require(last - first < Int.MaxValue, "block too long for an array")
    val size = (last - first + 1).toInt
    val rest = Array.tabulate(size)(first + _)
    // The factors found so far, largest prime first, as each new one is put in front.
    val found = Array.fill(size)(List.empty[(Long, Int)])
    val primes = NumberTheory.smallPrimes
    var i = 0
    while i < primes.length && primes(i) <= last / primes(i) do
      val p = primes(i)
      var multiple = (first + p - 1) / p * p // the first multiple of p that is >= first
      while multiple <= last do
        val j = (multiple - first).toInt
        var times = 0
        while rest(j) % p == 0 do
          rest(j) /= p
          times += 1
        found(j) = (p -> times) :: found(j)
        multiple += p
      i += 1
    IArray.tabulate(size): j =>
      val withLast = if rest(j) > 1 then (rest(j) -> 1) :: found(j) else found(j)
      withLast.reverse
