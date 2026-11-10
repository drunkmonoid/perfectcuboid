#!/bin/sh
# The test command of mutation testing (stryker4s.conf), run in Stryker4s's copy of the project.
#
# Stryker4s switches each mutant on with `sys.env.get("ACTIVE_MUTATION")` wherever it changed
# the code, and `sys.env` builds a new map of the whole environment at every call: in the
# search's inner loops that makes the tests a hundred times slower. So, the first time, those
# calls are pointed at a value read once per JVM instead. Then the core's tests, and the JVM
# tests of the search, on two of Mill's jobs, leaving the rest of the machine alone.
set -e
switch='_root_.scala.sys.env.get("ACTIVE_MUTATION")'
if grep -rlqF "$switch" core/src; then
  grep -rlF "$switch" core/src | xargs sed -i 's/_root_\.scala\.sys\.env\.get("ACTIVE_MUTATION")/_root_.perfectcuboid.ActiveMutation.value/g'
  cat > core/src/ActiveMutation.scala <<'SCALA'
package perfectcuboid

/** Which mutant is on, read once: written by mutation/tests.sh, in Stryker4s's copy only. */
object ActiveMutation:
  val value: Option[String] = sys.env.get("ACTIVE_MUTATION")
SCALA
fi
exec ./mill -j 2 core.jvm.test + cli.jvm.test.testOnly perfectcuboid.FastSearchTest perfectcuboid.SearchTest
