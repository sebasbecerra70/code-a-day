# Access-log parser with top-k report (CLI)

**Problem:** Build a command-line tool that reads web server access logs (Apache/Nginx Common or Combined Log Format) from files or stdin and reports the top-k paths, IPs, status codes, methods, or user agents. It should support status filters (`404`, `5xx`, `4xx,503`), text or JSON output, and robust handling of malformed lines.

## Approach
- **Parsing:** one anchored regex with named groups handles both formats. The referer/agent pair is an optional suffix. A `-` size means 0, and query strings are stripped so `/search?q=a` and `/search?q=b` aggregate together. Malformed lines are counted, not fatal, which matters because real logs always contain junk.
- **Aggregation:** a `Counter` keyed by the chosen field, filtered by a predicate compiled from the `--status` spec (`5xx` gives `s // 100 == 5`).
- **Top-k:** `heapq.nsmallest(k, ..., key=(-count, key))` runs in O(n log k) and breaks ties deterministically by key. That determinism matters for stable output and tests.
- **Heavy hitters at scale:** `SpaceSaving` (Metwally et al.) tracks top items in a stream with a fixed number of counters. Any item with frequency > N/capacity is guaranteed to be kept, with per-item error bounds. The tests verify `count - error <= true <= count`.
- **CLI:** `argparse` with multiple files and `-` for stdin, `errors="replace"` for bad bytes, exit codes 1 (I/O) and 2 (usage), and injectable streams so tests can call `main()` directly. One test also runs it as a subprocess.

## Complexity
| Aspect | Cost |
|--------|------|
| Parse | O(total log size) |
| Exact top-k | O(n + u log k), u = unique keys |
| Space (exact) | O(u) |
| Space-Saving | O(capacity) memory, O(capacity) per eviction here (O(1) with a stream-summary list) |

## Interview talking points
- `sort | uniq -c | sort -rn | head` is the shell baseline. Know when that's good enough.
- Exact counting needs O(unique keys) memory. For huge cardinality, use Space-Saving or Misra-Gries (top-k), Count-Min Sketch (frequency estimates), or HyperLogLog (distinct counts).
- Distributed version: count per shard, then merge Counters. The top-k of the union is **not** the union of the shard top-ks unless each shard reports enough candidates (or exact counts).
- Regex pitfalls: quoted fields can contain spaces, and IPv6 addresses contain colons. Anchor the pattern and use `[^"]*` for quoted parts.
- Production extras: time-window filtering, gzip input, percentiles of response size or latency, and `--follow` for live tailing.

## Run
From this folder: `python -m pytest -q`
