#!/usr/bin/env python3
"""Prints a hash of everything that can change eval results.

Covers the main sources and config of the pipeline modules, the build files (dependency versions), the PDF corpus
and the golden set. The eval report stores it; CI recomputes it to check that the committed report matches the code.
Only git-tracked files count, so `git add` new files before running `make eval`. Pure Python so it gives the same
result on Linux, macOS and CI.
"""
import hashlib
import pathlib
import subprocess

ROOT = pathlib.Path(__file__).resolve().parent.parent
PATHS = [
    "pom.xml",
    "*/pom.xml",
    "resources",
    "eval/data/golden-set.json",
    *(f"{m}/src/main" for m in (
        "rag-common", "rag-platform", "rag-store", "rag-api",
        "ingest-static", "ingest-dynamic", "retrieval-pre", "retrieval-post",
    )),
]


def main() -> None:
    out = subprocess.check_output(["git", "ls-files", "-z", "--", *PATHS], cwd=ROOT)
    files = sorted(f for f in out.decode().split("\0") if f)
    digest = hashlib.sha256()
    for name in files:
        digest.update(name.encode() + b"\0")
        digest.update(hashlib.sha256((ROOT / name).read_bytes()).digest())
    print(digest.hexdigest()[:16])


if __name__ == "__main__":
    main()
