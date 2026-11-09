// Build configuration for scala-cli. These `//> using` lines are the whole build.
// `::` before a version picks the library built for the current platform (JVM or Native).
//> using scala 3.9.0
// web/ is a separate project, the page compiled to JavaScript (see web/project.scala), bench/
// holds JMH microbenchmarks (see bench/project.scala), and
// docs/ holds worksheets that each run on their own.
//> using exclude web docs bench
// Compiler options: every warning is an error (-Werror); unused code, discarded values and
// ignored results are warnings; reading a field before it is set is caught (-Wsafe-init);
// and `null` is not a value of every type, only of `T | Null` (-Yexplicit-nulls).
//> using options -deprecation -feature -Werror -Wunused:all -Wvalue-discard -Wnonunit-statement -Wsafe-init -Yexplicit-nulls
//> using dep org.typelevel::cats-effect::3.7.1
//> using dep co.fs2::fs2-io::3.14.0
//> using dep com.monovore::decline-effect::2.6.2
//> using test.dep org.scalameta::munit::1.3.6
//> using test.dep org.scalameta::munit-scalacheck::1.3.1
//> using test.dep org.typelevel::munit-cats-effect::2.2.1
//> using test.dep org.typelevel::discipline-munit::2.0.0
//> using test.dep org.typelevel::cats-kernel-laws::2.13.0
