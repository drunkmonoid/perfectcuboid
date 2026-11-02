// Build configuration for scala-cli. These `//> using` lines are the whole build.
// `::` before a version picks the library built for the current platform (JVM or Native).
//> using scala 3.9.0
//> using options -deprecation -feature -Wunused:all
//> using dep org.typelevel::cats-effect::3.7.1
//> using dep co.fs2::fs2-io::3.14.0
//> using test.dep org.scalameta::munit::1.3.6
//> using test.dep org.scalameta::munit-scalacheck::1.3.1
//> using test.dep org.typelevel::munit-cats-effect::2.2.1
