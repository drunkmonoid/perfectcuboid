#!/bin/sh
# Runs mutation testing (stryker4s.conf) for hours, outside Mill, so that Mill stays free for
# other work meanwhile: Mill only works out Stryker4s's classpath. Needs a `java` on the path.
set -e
cd "$(dirname "$0")/.."
classpath=$(./mill show mutation.runClasspath | python3 -c '
import json, sys
# Mill shows each path as "ref:v0:<hash>:/the/path"; keep the path.
print(":".join(p.split(":", 3)[3] if p.startswith(("ref:", "qref:")) else p for p in json.load(sys.stdin)))')
exec java -cp "$classpath" stryker4s.command.Stryker4sMain
