// A project outside the build that uses the library as any other project would: as a
// dependency. Fill the local repository first, from the repository's root:
//
//   ./mill core.__.publishLocal
//   scala-cli run examples/library
//   scala-cli test examples/library
//
//> using scala 3.9.0
//> using repository ivy2Local
//> using dep io.github.drunkmonoid::perfectcuboid-core::3.0.0-SNAPSHOT
//> using test.dep org.scalameta::munit::1.3.6
