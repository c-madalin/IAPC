#!/usr/bin/env python3
"""Prints a Markdown table of line coverage per module from the JaCoCo XML reports. Used by CI's job summary."""
import pathlib
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parent.parent
rows = []
for report in sorted(ROOT.glob("*/target/site/jacoco/jacoco.xml")):
    tree = ET.parse(report)
    line = next((c for c in tree.getroot().findall("counter") if c.get("type") == "LINE"), None)
    if line is None:
        continue
    covered, missed = int(line.get("covered")), int(line.get("missed"))
    total = covered + missed
    rows.append((report.parts[-5], covered, total, 100 * covered / total if total else 0))

print("### Line coverage\n")
print("| Module | Covered | Lines | % |")
print("|---|---:|---:|---:|")
for module, covered, total, pct in rows:
    print(f"| {module} | {covered} | {total} | {pct:.0f}% |")
if not rows:
    print("| (no JaCoCo reports found) | | | |")
