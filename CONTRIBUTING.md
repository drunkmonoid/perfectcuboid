# Contributing

Everything needs only [scala-cli](https://scala-cli.virtuslab.org) and Java 17 or later. Clang is
needed only for the Scala Native build, and Node only for the JavaScript tests.

## Before every commit

CI runs all of these on every push, and they should all pass first:

```sh
scala-cli fmt --check .                       # or `scala-cli fmt .` to format
scala-cli test . --server=false               # the tests, on the JVM
scala-cli test web --server=false             # the web page's tests, on Node
scala-cli test cross --js --server=false      # the core's tests on JavaScript
scala-cli test cross --native --server=false  # ... and on Scala Native (slow to link)
for sheet in docs/tour/*.sc; do scala-cli run "$sheet" --server=false || break; done
scala-cli --power compile bench --server=false
scala-cli doc . -o api --server=false -O -project:perfectcuboid
```

Use `--server=false` when the result matters: with the build server, scala-cli 1.18 prints
warnings that `-Werror` turns into errors, but still exits with success.

The compiler is strict: every warning is an error, unused code and discarded values are
warnings, `null` is not a value of every type, and reading a field before it is set is caught.
Every public definition has a doc comment.

**Watch the file lists.** The worksheets in `docs/tour`, the web page in `web/` and the
cross-platform tests in `cross/` don't take all of `src/`: each lists the files it needs, with
`//> using file`. If a file in `src` starts using another one, add that one to every list that
has the first, or the build that uses the list breaks. CI catches it; a full local run does too.

## Adding a report

A report is a command that only prints lines, like `legs` or `crosscheck`.

1. Write the work as pure functions in `src/`, with tests.
2. Add a case to `Report` in `src/CommandLine.scala`, and a `report(...)` entry to `reports`
   there, with the arguments, their defaults and their help.
3. In `src/Main.scala`, add the case to `Cli.run` and write the report as a function returning
   the lines, a `LazyList[String]` if it is long.
4. Add a golden test in `test/GoldenTest.scala`, a small run of the command whose output is
   kept in `test/golden`, and create the file with `UPDATE_GOLDEN=1 scala-cli test .`. Read the
   new file before committing it: it is now what the command must print. The `--help` output
   is golden too, so it changes with every new command.

A command that reads files, uses the clock or runs for long gets a case in `Command` instead,
carried out in `Main.execute` as an `IO`.

## Adding a hunt kind

A hunt kind is a `Quarry` (`src/Quarry.scala`): a name, what the search counts up through (its
key), the search of a block of keys, how to write a find as a line and read it back, how to sum
finds up, and how to check a find independently, for `verify`. The line format is best derived:
`final case class MyLine(...) derives LineFormat` in `src/LineFormat.scala`. Give the kind its
`maxKey`, the largest key its search handles, and add it to `Quarry.byName`, so that `hunt`,
`verify` and `merge` all take `--kind` with its name. Test that a hunt of it writes what its
search finds, and that it resumes to the same file.

## Adding a chart

`src/Charts.scala` writes SVG as plain text: `Charts.lines` for series on log or linear axes,
`Charts.scatter` for dots. Give it the data and the words; it sets the colours, which work in
light and dark mode, and the hover titles. Put the code that gathers the data in
`src/Pictures.scala` or next to the analysis it belongs to, and save charts meant for the
README in `docs/`.

## Running a shard

A long range can be split between processes or machines. Each searches its own range into its
own file, starting where the one before ends:

```sh
scala-cli run . -J -Xmx1g -- hunt part1.txt 2e9 --from 1000000001 --kind primitive
scala-cli run . -J -Xmx1g -- hunt part2.txt 3e9 --from 2000000001 --kind primitive
scala-cli run . -- verify part1.txt --kind primitive   # check each shard on its own
scala-cli run . -- merge all.txt part1.txt part2.txt --kind primitive
scala-cli run . -- verify all.txt --kind primitive
```

Ctrl+C stops a shard and the same command resumes it. `merge` refuses shards with gaps or
overlaps between them, and never overwrites a file. On the native build, give each process one
thread (it refuses more) and run several processes instead.

## Writing

`RESULTS.md` records what was found, each figure with the command that reproduces it. Say what
was measured, on what, and how far it can be trusted; when a result turns out to be wrong,
correct it there and say so.
