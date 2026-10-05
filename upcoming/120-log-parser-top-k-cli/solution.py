"""log-top-k: parse web access logs (Common/Combined Log Format) and report
the top-k IPs, paths, status codes, or user agents.

Usage:
    python solution.py access.log                     # top 10 paths
    python solution.py access.log --by ip -k 5
    cat *.log | python solution.py - --by status --status 5xx --json
"""

from __future__ import annotations

import argparse
import heapq
import json
import re
import sys
from collections import Counter
from dataclasses import dataclass
from typing import Iterable, TextIO

# 127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] "GET /a.gif HTTP/1.0" 200 2326 "referer" "agent"
LOG_RE = re.compile(
    r'^(?P<ip>\S+) \S+ (?P<user>\S+) \[(?P<time>[^\]]+)\] '
    r'"(?P<method>[A-Z]+) (?P<path>\S+)(?: (?P<proto>[^"]+))?" '
    r'(?P<status>\d{3}) (?P<size>\d+|-)'
    r'(?: "(?P<referer>[^"]*)" "(?P<agent>[^"]*)")?\s*$'
)


@dataclass(frozen=True)
class Entry:
    ip: str
    method: str
    path: str
    status: int
    size: int
    agent: str | None


def parse_line(line: str) -> Entry | None:
    """Parses one log line; returns None for malformed lines."""
    m = LOG_RE.match(line)
    if not m:
        return None
    path = m["path"].split("?", 1)[0]  # group /search?q=a and /search?q=b together
    size = 0 if m["size"] == "-" else int(m["size"])
    return Entry(m["ip"], m["method"], path, int(m["status"]), size, m["agent"])


def parse(lines: Iterable[str]) -> tuple[list[Entry], int]:
    """Returns (entries, malformed_count)."""
    entries, bad = [], 0
    for line in lines:
        if not line.strip():
            continue
        e = parse_line(line)
        if e is None:
            bad += 1
        else:
            entries.append(e)
    return entries, bad


def status_filter(spec: str | None):
    """'404' -> exact, '5xx' -> class, '4xx,5xx' -> union, None -> all."""
    if not spec:
        return lambda s: True
    tests = []
    for part in spec.split(","):
        part = part.strip().lower()
        if re.fullmatch(r"[1-5]xx", part):
            tests.append(lambda s, c=int(part[0]): s // 100 == c)
        elif re.fullmatch(r"\d{3}", part):
            tests.append(lambda s, c=int(part): s == c)
        else:
            raise ValueError(f"bad status filter {part!r}")
    return lambda s: any(t(s) for t in tests)


def top_k(counts: Counter, k: int) -> list[tuple[str, int]]:
    """Top k by count desc, ties broken by key asc. O(n log k) with a bounded heap."""
    return heapq.nsmallest(k, counts.items(), key=lambda kv: (-kv[1], str(kv[0])))


class SpaceSaving:
    """Streaming heavy hitters (Metwally et al.) with at most `capacity` counters.

    Any item with true frequency > N / capacity is guaranteed to be tracked.
    Reported counts overestimate by at most the recorded error.
    """

    def __init__(self, capacity: int):
        if capacity < 1:
            raise ValueError("capacity must be >= 1")
        self.capacity = capacity
        self.counts: dict[str, int] = {}
        self.errors: dict[str, int] = {}

    def add(self, item: str) -> None:
        if item in self.counts:
            self.counts[item] += 1
        elif len(self.counts) < self.capacity:
            self.counts[item] = 1
            self.errors[item] = 0
        else:
            # Replace the current minimum; the newcomer inherits its count as error bound.
            victim = min(self.counts, key=self.counts.__getitem__)
            c = self.counts.pop(victim)
            self.errors.pop(victim)
            self.counts[item] = c + 1
            self.errors[item] = c

    def top(self, k: int) -> list[tuple[str, int]]:
        return top_k(Counter(self.counts), k)


KEYS = {
    "path": lambda e: e.path,
    "ip": lambda e: e.ip,
    "status": lambda e: str(e.status),
    "agent": lambda e: e.agent or "-",
    "method": lambda e: e.method,
}


def report(entries: Iterable[Entry], by: str = "path", k: int = 10, status: str | None = None) -> dict:
    keep = status_filter(status)
    key = KEYS[by]
    counts: Counter = Counter()
    total = bytes_ = 0
    for e in entries:
        if keep(e.status):
            counts[key(e)] += 1
            total += 1
            bytes_ += e.size
    return {"by": by, "total": total, "bytes": bytes_, "unique": len(counts), "top": top_k(counts, k)}


def format_table(rep: dict, malformed: int) -> str:
    lines = [f"{rep['total']} requests, {rep['unique']} unique {rep['by']} values, {rep['bytes']} bytes"]
    if rep["top"]:
        width = max(len(str(c)) for _, c in rep["top"])
        for key, c in rep["top"]:
            pct = 100 * c / rep["total"]
            lines.append(f"{c:>{width}}  {pct:5.1f}%  {key}")
    if malformed:
        lines.append(f"({malformed} malformed lines skipped)")
    return "\n".join(lines) + "\n"


def main(argv: list[str] | None = None, stdin: TextIO | None = None, stdout: TextIO | None = None) -> int:
    p = argparse.ArgumentParser(prog="log-top-k", description="Top-k report over access logs.")
    p.add_argument("files", nargs="+", help="log files, or - for stdin")
    p.add_argument("--by", choices=sorted(KEYS), default="path")
    p.add_argument("-k", type=int, default=10)
    p.add_argument("--status", help="filter: 404, 5xx, or a comma list like 4xx,503")
    p.add_argument("--json", action="store_true", help="machine-readable output")
    args = p.parse_args(argv)
    stdin, stdout = stdin or sys.stdin, stdout or sys.stdout
    if args.k < 1:
        print("log-top-k: -k must be >= 1", file=sys.stderr)
        return 2

    entries: list[Entry] = []
    malformed = 0
    for path in args.files:
        try:
            src = stdin if path == "-" else open(path, encoding="utf-8", errors="replace")
        except OSError as e:
            print(f"log-top-k: {e.strerror}: {path}", file=sys.stderr)
            return 1
        try:
            es, bad = parse(src)
        finally:
            if src is not stdin:
                src.close()
        entries.extend(es)
        malformed += bad

    try:
        rep = report(entries, by=args.by, k=args.k, status=args.status)
    except ValueError as e:
        print(f"log-top-k: {e}", file=sys.stderr)
        return 2
    if args.json:
        rep["top"] = [{"key": key, "count": c} for key, c in rep["top"]]
        rep["malformed"] = malformed
        stdout.write(json.dumps(rep) + "\n")
    else:
        stdout.write(format_table(rep, malformed))
    return 0


if __name__ == "__main__":
    sys.exit(main())
