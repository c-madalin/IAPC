#!/usr/bin/env python3
"""CI gate for the eval harness (see .claude/skills/eval-regression/SKILL.md).

Fails when:
  1. eval/reports/latest.json was produced for different pipeline code than what is being checked, or
  2. a metric dropped more than the tolerance below eval/baseline.json.
Passes with a warning when there is no baseline yet.
"""
import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
REPORT = ROOT / "eval" / "reports" / "latest.json"
BASELINE = ROOT / "eval" / "baseline.json"
TOLERANCE = 0.02
# Higher is better for all of these. "errors" is checked separately.
GATED = ["recall_at_k", "mrr", "refusal_accuracy", "citation_faithfulness"]


def main() -> int:
    current = subprocess.check_output([str(ROOT / "scripts" / "pipeline-hash.py")], text=True).strip()
    if not REPORT.exists():
        print(f"FAIL: {REPORT.relative_to(ROOT)} is missing. Run `make eval` and commit the report.")
        return 1
    report = json.loads(REPORT.read_text())
    if report.get("pipelineHash") != current:
        print(
            f"FAIL: the eval report is for pipeline {report.get('pipelineHash')}, the code is {current}.\n"
            "Pipeline code or the golden set changed since the last eval. Run `make eval` and commit the report."
        )
        return 1
    metrics = report.get("metrics", {})
    if metrics.get("errors", 0) > 0:
        print(f"FAIL: {int(metrics['errors'])} golden-set questions errored in the committed report.")
        return 1
    if not BASELINE.exists():
        print("WARN: no eval/baseline.json yet; skipping the regression comparison. Run `make eval-baseline`.")
        return 0
    baseline = json.loads(BASELINE.read_text()).get("metrics", {})
    failed = False
    for name in GATED:
        if name not in baseline or name not in metrics:
            continue
        delta = metrics[name] - baseline[name]
        status = "ok"
        if delta < -TOLERANCE:
            status = f"REGRESSION (more than {TOLERANCE} below baseline)"
            failed = True
        print(f"{name:24} baseline={baseline[name]:.3f} current={metrics[name]:.3f} delta={delta:+.3f} {status}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
