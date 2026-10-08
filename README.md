# code-a-day

![tests](https://github.com/sebasbecerra70/code-a-day/actions/workflows/test.yml/badge.svg)

One small, tested piece of code a day: data structures, algorithms, design patterns, and small utilities, rotating across **Python, TypeScript, Java and C++**. Each entry has a solution, tests, and `NOTES.md` explaining the approach, complexity, and trade-offs.

## Layout
```
<language>/<YYYY-MM-DD>-<slug>/
  solution + tests + NOTES.md
```
Conventions for every language are in [CONVENTIONS.md](CONVENTIONS.md).

## How entries are published
Entries are written and tested in batches, then queued in [`upcoming/`](upcoming). A scheduled GitHub Action ([`publish.yml`](.github/workflows/publish.yml)) releases one per day. It re-runs the entry's tests, moves it into its language folder and adds it to the index below. Entries are written with AI assistance, and I study each one as it's released.

## Index
<!-- INDEX:START -->
| Date | Topic | Language | Entry |
|------|-------|----------|-------|
| 2026-10-05 | LRU cache (hash map + doubly linked list) | Python | [python/2026-10-05-lru-cache](python/2026-10-05-lru-cache) |
| 2026-10-05 | Dijkstra shortest path (binary min-heap) | TypeScript | [typescript/2026-10-05-dijkstra-shortest-path](typescript/2026-10-05-dijkstra-shortest-path) |
| 2026-10-06 | Generic doubly linked list with iterator | Java | [java/2026-10-06-generic-linked-list](java/2026-10-06-generic-linked-list) |
| 2026-10-07 | Dynamic array (growable vector from scratch) | C++ | [cpp/2026-10-07-dynamic-array](cpp/2026-10-07-dynamic-array) |
| 2026-10-08 | Consistent hashing ring (virtual nodes) | Python | [python/2026-10-08-consistent-hashing-ring](python/2026-10-08-consistent-hashing-ring) |
<!-- INDEX:END -->
