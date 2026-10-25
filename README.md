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
| 2026-10-09 | Trie autocomplete (top-k by frequency) | TypeScript | [typescript/2026-10-09-trie-autocomplete](typescript/2026-10-09-trie-autocomplete) |
| 2026-10-10 | Array deque (circular buffer that grows) | Java | [java/2026-10-10-array-deque](java/2026-10-10-array-deque) |
| 2026-10-11 | unique_ptr from scratch (move-only ownership) | C++ | [cpp/2026-10-11-unique-ptr-from-scratch](cpp/2026-10-11-unique-ptr-from-scratch) |
| 2026-10-12 | Bloom filter (optimal sizing, double hashing) | Python | [python/2026-10-12-bloom-filter](python/2026-10-12-bloom-filter) |
| 2026-10-13 | Token bucket rate limiter (injectable clock) | TypeScript | [typescript/2026-10-13-token-bucket-rate-limiter](typescript/2026-10-13-token-bucket-rate-limiter) |
| 2026-10-14 | Open addressing hash map (linear probing, backward-shift delete) | Java | [java/2026-10-14-open-addressing-hash-map](java/2026-10-14-open-addressing-hash-map) |
| 2026-10-15 | Ring buffer (fixed-capacity circular queue template) | C++ | [cpp/2026-10-15-ring-buffer-template](cpp/2026-10-15-ring-buffer-template) |
| 2026-10-16 | Binary min-heap (with heapify and heapsort) | Python | [python/2026-10-16-binary-heap](python/2026-10-16-binary-heap) |
| 2026-10-17 | Debounce and throttle | TypeScript | [typescript/2026-10-17-debounce-and-throttle](typescript/2026-10-17-debounce-and-throttle) |
| 2026-10-18 | AVL tree (self-balancing BST) | Java | [java/2026-10-18-avl-tree](java/2026-10-18-avl-tree) |
| 2026-10-19 | String tokenizer (split, quoted fields, arithmetic lexer) | C++ | [cpp/2026-10-19-string-tokenizer](cpp/2026-10-19-string-tokenizer) |
| 2026-10-20 | Quickselect: k-th largest element | Python | [python/2026-10-20-quickselect-kth-largest](python/2026-10-20-quickselect-kth-largest) |
| 2026-10-21 | Type-safe event emitter | TypeScript | [typescript/2026-10-21-typed-event-emitter](typescript/2026-10-21-typed-event-emitter) |
| 2026-10-22 | BST iterators (in-order, reverse, pre/post-order) | Java | [java/2026-10-22-bst-iterators](java/2026-10-22-bst-iterators) |
| 2026-10-23 | Matrix class with operator overloading | C++ | [cpp/2026-10-23-matrix-class-operators](cpp/2026-10-23-matrix-class-operators) |
| 2026-10-24 | 0/1 knapsack (DP with item reconstruction) | Python | [python/2026-10-24-knapsack-01](python/2026-10-24-knapsack-01) |
| 2026-10-25 | Promise pool with a concurrency limit | TypeScript | [typescript/2026-10-25-promise-pool-concurrency-limit](typescript/2026-10-25-promise-pool-concurrency-limit) |
<!-- INDEX:END -->
