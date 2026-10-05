# CSV to JSON command-line tool

**Problem:** Build a production-quality CLI that converts CSV (from a file or stdin) to a JSON array or JSON Lines. It should handle quoting, embedded newlines, BOMs, ragged rows, duplicate headers, custom delimiters, optional type inference, and column selection, and it should stream instead of loading everything into memory.

## Approach
- **Parsing:** use the stdlib `csv` module. Hand-rolling CSV is a trap because of quoted commas, `""` escapes, and newlines inside quotes. Open files with `newline=""` (required for embedded newlines) and `utf-8-sig` (strips the Excel BOM).
- **Pipeline:** `csv.reader` feeds `rows_to_records` (a generator), which feeds the writer. The array writer emits `[`, then records separated by commas, then `]`, so memory stays O(row) even for huge files. JSON Lines is one `json.dumps` per line.
- **Headers:** blank names become `column_N`, and duplicates get `_2`, `_3` so no data is silently overwritten. `--no-header` generates `column_1..N`.
- **Ragged rows:** short rows are padded with `None`. Long rows are truncated by default or rejected with a line number under `--strict`. Blank lines are skipped.
- **Type inference (opt-in):** empty becomes `null`, `true`/`false` become booleans, and ints/floats are parsed. Leading-zero values like `02134` stay strings (zip codes, IDs), and `nan`/`inf` stay strings, which keeps the output valid JSON.
- **CLI hygiene:** `argparse` with `-` for stdin, `\t` escapes for the delimiter, errors go to stderr with exit codes 1/2, and `main()` takes injectable streams so it's testable in-process. One test also runs it as a real subprocess.

## Complexity
| Aspect | Cost |
|--------|------|
| Time | O(input size) |
| Space | O(longest row) when streaming |

## Interview talking points
- Why not `csv.DictReader`? It silently drops duplicate headers and stores extra fields under a `None` key. Explicit handling is clearer.
- Type inference is lossy and ambiguous (`"1.0"` vs `1`, phone numbers, IDs), so it's opt-in. A real tool would accept a schema per column.
- Streaming JSON arrays by hand avoids O(n) memory. JSON Lines is friendlier for big data (`jq`, BigQuery, Spark).
- Exit codes and stderr matter for composing tools in shell pipelines.
- Edge cases: BOM, CRLF, quoted newlines, Unicode (`ensure_ascii=False`), empty file, header-only file.

## Run
From this folder: `python -m pytest -q`
