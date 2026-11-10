# Names every failed test in Mill's test reports as a GitHub annotation, so a failure shows on
# the run's page without opening its log. CI runs it only when a step has failed.
import glob
import xml.etree.ElementTree as ElementTree

for report in sorted(glob.glob("out/**/test-report.xml", recursive=True)):
    for case in ElementTree.parse(report).getroot().iter("testcase"):
        for problem in list(case.findall("failure")) + list(case.findall("error")):
            message = (problem.get("message") or problem.text or "").strip().splitlines()
            print(f"::error title={case.get('classname')}::{case.get('name')}: {message[0] if message else ''}")
