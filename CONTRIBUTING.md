# Contributing

The build is [Mill](https://mill-build.org), in `build.mill`. The `./mill` script downloads
Mill and a Java 17 the first time, so nothing needs installing first, apart from clang for
Scala Native and Node for the JavaScript tests. The guided tour's worksheets run with
[scala-cli](https://scala-cli.virtuslab.org), and use the library as a dependency, from the
local repository that `./mill __.publishLocal` fills.

| module | what it is | its sources and tests |
| --- | --- | --- |
| `core` | the pure library, for the JVM, JavaScript and Scala Native | `core/src`, `core/test/src` |
| `cli` | the command line, hunts, files and reports, on Cats Effect | `cli/src`, `cli/test/src` |
| `web` | the page in `docs/check`, the core compiled to JavaScript | `web/src`, `web/test/src` |
| `bench` | JMH microbenchmarks | `bench/src` |
| `checker` | checks the certificates of `local`'s rules, sharing no code with the program | `checker/src` |
| `speed` | the speed guard: the search timed against a fixed reference, in CI | `speed/src` |
| `api` | the API docs of `core` and `cli` together | (theirs) |

A file in `core` may use only `core`, and must compile on all three platforms: no files, no
threads, no Cats Effect. Everything else goes in `cli`.

## Before every commit

CI runs all of these on every push, and they should all pass first:

```sh
./mill mill.scalalib.scalafmt/checkFormatAll   # or ./mill mill.scalalib.scalafmt/ to format
./mill __.test                                 # every test, on every platform (Native is slow to link)
./mill bench.compile
./mill api.docJar
scala-cli fmt --check docs/tour
./mill __.publishLocal                         # the worksheets use what this publishes
for sheet in docs/tour/*.sc; do scala-cli run "$sheet" --server=false || break; done
```

The compiler is strict: every warning is an error, unused code and discarded values are
warnings, `null` is not a value of every type, and reading a field before it is set is caught.
Every public definition has a doc comment.

## Adding a report

A report is a command that only prints lines, like `legs` or `crosscheck`.

1. Write the work as pure functions, in `core/src` if it can run anywhere and in `cli/src`
   otherwise, with tests.
2. Add a case to `Report` in `cli/src/CommandLine.scala`, and a `report(...)` entry to `reports`
   there, with the arguments, their defaults and their help.
3. In `cli/src/Main.scala`, add the case to `Cli.run` and write the report as a function
   returning the lines, a `LazyList[String]` if it is long.
4. Add a golden test in `cli/test/src/GoldenTest.scala`, a small run of the command whose output
   is kept in `cli/test/resources/golden`, and create the file with
   `UPDATE_GOLDEN=1 ./mill cli.jvm.test`. Read the new file before committing it: it is now
   what the command must print. The `--help` output is golden too, so it changes with every
   new command.

A command that reads files, uses the clock or runs for long gets a case in `Command` instead,
carried out in `Main.execute` as an `IO`.

## Adding a hunt kind

A hunt kind is a `Quarry` (`cli/src/Quarry.scala`): a name, what the search counts up through
(its key), the search of a block of keys, how to write a find as a line and read it back, how
to sum finds up, and how to check a find independently, for `verify`. The line format is best
derived: `final case class MyLine(...) derives LineFormat` in `core/src/LineFormat.scala`. Give
the kind its `maxKey`, the largest key its search handles, and add it to `Quarry.byName`, so
that `hunt`, `verify` and `merge` all take `--kind` with its name. Test that a hunt of it
writes what its search finds, and that it resumes to the same file.

## Adding a chart

`cli/src/Charts.scala` writes SVG as plain text: `Charts.lines` for series on log or linear
axes, `Charts.scatter` for dots. Give it the data and the words; it sets the colours, which
work in light and dark mode, and the hover titles. Put the code that gathers the data in
`cli/src/Pictures.scala` or next to the analysis it belongs to, and save charts meant for the
README in `docs/`.

## Running a shard

A long range can be split between processes or machines. Each searches its own range into its
own file, starting where the one before ends:

```sh
java -Xmx1g -jar perfectcuboid.jar hunt part1.txt 2e9 --from 1000000001 --kind primitive
java -Xmx1g -jar perfectcuboid.jar hunt part2.txt 3e9 --from 2000000001 --kind primitive
java -jar perfectcuboid.jar verify part1.txt --kind primitive   # check each shard on its own
java -jar perfectcuboid.jar merge all.txt part1.txt part2.txt --kind primitive
java -jar perfectcuboid.jar verify all.txt --kind primitive
```

`perfectcuboid.jar` is built by `./mill cli.jvm.assembly`, in `out/cli/jvm/assembly.dest/out.jar`.
On one machine, `hunt FILE UPTO --processes N` does all of this itself. Ctrl+C stops a shard
and the same command resumes it. `merge` refuses shards with gaps or
overlaps between them, and never overwrites a file. On the native build, give each process one
thread (it refuses more) and run several processes instead.

## Writing

`RESULTS.md` records what was found, each figure with the command that reproduces it. Say what
was measured, on what, and how far it can be trusted; when a result turns out to be wrong,
correct it there and say so.
