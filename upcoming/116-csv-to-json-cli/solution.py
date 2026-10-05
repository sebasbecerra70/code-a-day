"""csv-to-json: convert CSV to a JSON array or JSON Lines.

Usage:
    python solution.py data.csv                      # JSON array to stdout
    python solution.py data.csv --jsonl              # one object per line
    cat data.csv | python solution.py - --infer-types --select name,age
    python solution.py data.tsv --delimiter '\\t' --no-header -o out.json
"""

from __future__ import annotations

import argparse
import csv
import json
import sys
from typing import Any, Iterable, Iterator, TextIO

_NUMERIC_START = set("+-.0123456789")


def infer(value: str) -> Any:
    """'' -> None, true/false -> bool, ints, floats; everything else stays a string.

    Values with leading zeros (zip codes, IDs like '007') stay strings.
    """
    if value == "":
        return None
    low = value.lower()
    if low in ("true", "false"):
        return low == "true"
    if value[0] in _NUMERIC_START:
        digits = value.lstrip("+-")
        if len(digits) > 1 and digits[0] == "0" and digits[1] != ".":
            return value
        try:
            return int(value)
        except ValueError:
            pass
        try:
            f = float(value)
            if f == f and f not in (float("inf"), float("-inf")):  # reject nan/inf
                return f
        except ValueError:
            pass
    return value


def _dedupe(headers: list[str]) -> list[str]:
    """Blank or repeated headers get suffixes: ['a', 'a', ''] -> ['a', 'a_2', 'column_3']."""
    seen: dict[str, int] = {}
    out = []
    for i, h in enumerate(headers, 1):
        h = h.strip() or f"column_{i}"
        if h in seen:
            seen[h] += 1
            h = f"{h}_{seen[h]}"
        else:
            seen[h] = 1
        out.append(h)
    return out


def rows_to_records(
    rows: Iterable[list[str]],
    header: bool = True,
    infer_types: bool = False,
    select: list[str] | None = None,
    strict: bool = False,
) -> Iterator[dict[str, Any]]:
    """Turns CSV rows into dicts. Short rows are padded with None; long rows raise in strict mode."""
    it = iter(rows)
    if header:
        first = next(it, None)
        if first is None:
            return
        headers = _dedupe(first)
    else:
        headers = None
    if select is not None and headers is not None:
        missing = [c for c in select if c not in headers]
        if missing:
            raise ValueError(f"unknown column(s): {', '.join(missing)}")

    for lineno, row in enumerate(it, 2 if header else 1):
        if not row or row == [""]:
            continue  # skip blank lines
        cols = headers or [f"column_{i}" for i in range(1, len(row) + 1)]
        if len(row) > len(cols):
            if strict:
                raise ValueError(f"line {lineno}: expected {len(cols)} fields, got {len(row)}")
            row = row[: len(cols)]
        conv = infer if infer_types else (lambda v: v)
        rec = {c: conv(row[i]) if i < len(row) else None for i, c in enumerate(cols)}
        yield {c: rec.get(c) for c in select} if select else rec


def convert(
    src: TextIO,
    dst: TextIO,
    *,
    delimiter: str = ",",
    header: bool = True,
    infer_types: bool = False,
    select: list[str] | None = None,
    jsonl: bool = False,
    indent: int | None = 2,
    strict: bool = False,
) -> int:
    """Streams CSV from src to JSON on dst. Returns the number of records written."""
    reader = csv.reader(src, delimiter=delimiter)
    records = rows_to_records(reader, header=header, infer_types=infer_types, select=select, strict=strict)
    n = 0
    if jsonl:
        for rec in records:
            dst.write(json.dumps(rec, ensure_ascii=False) + "\n")
            n += 1
        return n
    # Stream a JSON array without holding every record in memory.
    dst.write("[")
    for rec in records:
        dst.write(("," if n else "") + ("\n" if indent is not None else ""))
        text = json.dumps(rec, ensure_ascii=False, indent=indent)
        dst.write(text if indent is None else "\n".join(" " * indent + ln for ln in text.splitlines()))
        n += 1
    dst.write(("\n" if n and indent is not None else "") + "]\n")
    return n


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="csv-to-json", description="Convert CSV to JSON or JSON Lines.")
    p.add_argument("input", help="CSV file path, or - for stdin")
    p.add_argument("-o", "--output", help="output file (default: stdout)")
    p.add_argument("-d", "--delimiter", default=",", help="field delimiter (use '\\t' for tabs)")
    p.add_argument("--no-header", action="store_true", help="first row is data; keys become column_1..N")
    p.add_argument("--infer-types", action="store_true", help="convert numbers, booleans, and empty cells")
    p.add_argument("--select", help="comma-separated columns to keep, in this order")
    p.add_argument("--jsonl", action="store_true", help="write JSON Lines instead of an array")
    p.add_argument("--compact", action="store_true", help="no indentation")
    p.add_argument("--strict", action="store_true", help="error on rows with too many fields")
    return p


def main(argv: list[str] | None = None, stdin: TextIO | None = None, stdout: TextIO | None = None) -> int:
    args = build_parser().parse_args(argv)
    stdin = stdin or sys.stdin
    stdout = stdout or sys.stdout
    delimiter = args.delimiter.encode().decode("unicode_escape")  # allow '\t'
    if len(delimiter) != 1:
        print("csv-to-json: delimiter must be a single character", file=sys.stderr)
        return 2
    select = [c.strip() for c in args.select.split(",")] if args.select else None
    try:
        src = stdin if args.input == "-" else open(args.input, newline="", encoding="utf-8-sig")
    except OSError as e:
        print(f"csv-to-json: {e.strerror}: {args.input}", file=sys.stderr)
        return 1
    try:
        dst = stdout if not args.output else open(args.output, "w", encoding="utf-8")
        try:
            convert(
                src, dst, delimiter=delimiter, header=not args.no_header, infer_types=args.infer_types,
                select=select, jsonl=args.jsonl, indent=None if args.compact else 2, strict=args.strict,
            )
        finally:
            if dst is not stdout:
                dst.close()
    except (ValueError, csv.Error) as e:
        print(f"csv-to-json: {e}", file=sys.stderr)
        return 1
    finally:
        if src is not stdin:
            src.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
