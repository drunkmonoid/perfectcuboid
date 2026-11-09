// The page in docs/check: the core of the library, compiled to JavaScript by Scala.js.
// Build it with
//
//   scala-cli --power package web --js --js-mode release -o docs/check/main.js -f
//
// Only the files the page needs are listed, so the JavaScript stays small and none of the
// JVM-only parts (files, threads, the command line) come along.
//> using scala 3.9.0
//> using platform js
//> using options -deprecation -feature -Werror -Wunused:all -Wvalue-discard -Wnonunit-statement -Wsafe-init -Yexplicit-nulls
//> using dep org.scala-js::scalajs-dom::2.8.1
//> using file ../src/Arith.scala ../src/Lengths.scala ../src/Wide.scala ../src/Cuboid.scala ../src/Box.scala
//> using file ../src/NearMisses.scala ../src/Pythagorean.scala ../src/NumberTheory.scala ../src/Montgomery.scala
//> using file ../src/PollardRho.scala ../src/Elliptic.scala
//> using file ../src/Search.scala ../src/Sieve.scala ../src/FastSearch.scala ../src/Platform.scala
//> using test.dep org.scalameta::munit::1.3.6
