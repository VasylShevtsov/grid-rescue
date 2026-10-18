#!/usr/bin/env python3
"""Instructor-controlled grading: 53 fixed JUnit tests, grade from 1 to 3.

Run after a CLEAN Maven build. Missing, errored, failed and skipped tests earn
no credit. This script does not award the manual PR/code-quality point.
"""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET

EXPECTED = {"GameTest": 10, "TeamTest": 8, "TileTest": 8, "UnitTest": 5,
            "ScoutTest": 7, "MedicTest": 8, "TruckTest": 3, "CommandTest": 4}
TOTAL = 53


def calculate(reports, build_outcome):
    passed = 0
    problems = []
    suites = {}
    if build_outcome != "success":
        return {"passed": 0, "total": TOTAL, "grade": "1.00", "complete": False,
                "problems": ["Build did not complete successfully; test reports cannot earn credit."],
                "suites": {}}
    for suite, expected in EXPECTED.items():
        path = reports / ("TEST-edu.whitman." + suite + ".xml")
        credit = 0
        try:
            root = ET.parse(path).getroot()
            cases = root.findall("testcase")
            identities = {(c.get("classname"), c.get("name")) for c in cases}
            if (root.tag != "testsuite" or root.get("name") != "edu.whitman." + suite
                    or len(cases) != expected or len(identities) != expected
                    or any(c.get("classname") != "edu.whitman." + suite or not c.get("name")
                           for c in cases)):
                raise ValueError("report must contain the expected number of distinct test cases")
            totals = {k: int(root.get(k, "0")) for k in ("tests", "failures", "errors", "skipped")}
            actual = {k: sum(c.find(tag) is not None for c in cases)
                      for k, tag in (("failures", "failure"), ("errors", "error"), ("skipped", "skipped"))}
            if totals["tests"] != expected or any(totals[k] != actual[k] for k in actual):
                raise ValueError("inconsistent JUnit report totals")
            credit = sum(not any(c.find(tag) is not None for tag in ("failure", "error", "skipped"))
                         for c in cases)
        except (OSError, ET.ParseError, ValueError) as exc:
            problems.append(suite + ": " + str(exc))
        suites[suite] = {"passed": credit, "expected": expected}
        passed += credit
    return {"passed": passed, "total": TOTAL, "grade": format(1 + 2 * passed / TOTAL, ".2f"),
            "complete": passed == TOTAL and not problems, "problems": problems, "suites": suites}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reports", type=Path, default=Path("target/surefire-reports"))
    parser.add_argument("--build-outcome", choices=("success", "failure", "cancelled", "skipped"),
                        required=True, help="Use the actual Maven outcome, not the grader's exit status.")
    parser.add_argument("--require-all", action="store_true", help="Exit nonzero unless all 53 pass.")
    args = parser.parse_args()
    result = calculate(args.reports, args.build_outcome)
    lines = ["Automated grade: " + result["grade"] + " / 3.00",
             str(result["passed"]) + "/53 grading tests passed.",
             "The remaining milestone 2 point is assessed manually for PR requirements and code quality."]
    lines += ["Report issue: " + message for message in result["problems"]]
    text = "\n".join(lines) + "\n"
    Path("grade.txt").write_text(text, encoding="utf-8")
    Path("grade.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(text, end="")
    return 1 if args.require_all and not result["complete"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
