// Microbenchmarks of the search's kernels, run by JMH, the Java benchmark harness:
//
//   scala-cli --power run bench                # all of them, about a minute
//   scala-cli --power run bench -- isqrtLong   # those whose names match
//
// (Not with --server=false: JMH generates Java sources, and only the build server compiles them.)
//
// JMH runs each benchmark in a fresh JVM, warms it up, and reports the average time per call
// with an error margin. The library itself comes from ../src.
//> using scala 3.9.0
//> using options -deprecation -feature -Werror -Wunused:all -Wvalue-discard -Wnonunit-statement -Wsafe-init -Yexplicit-nulls
//> using file ../src
//> using jmh true
//> using dep org.typelevel::cats-effect::3.7.1
//> using dep co.fs2::fs2-io::3.14.0
//> using dep com.monovore::decline-effect::2.6.2
