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
| 2026-10-26 | Merge sort and quicksort (stable vs in-place) | Java | [java/2026-10-26-merge-sort-and-quicksort](java/2026-10-26-merge-sort-and-quicksort) |
| 2026-10-27 | Rational number class (exact fractions) | C++ | [cpp/2026-10-27-rational-number-class](cpp/2026-10-27-rational-number-class) |
| 2026-10-28 | Edit distance (Levenshtein) with alignment | Python | [python/2026-10-28-edit-distance](python/2026-10-28-edit-distance) |
| 2026-10-29 | Deep equality (cycles, Maps, Sets, Dates) | TypeScript | [typescript/2026-10-29-deep-equal](typescript/2026-10-29-deep-equal) |
| 2026-10-30 | Heap sort (in-place, bottom-up heapify) | Java | [java/2026-10-30-heap-sort](java/2026-10-30-heap-sort) |
| 2026-10-31 | Big integer arithmetic (base 10^9 limbs) | C++ | [cpp/2026-10-31-big-integer-arithmetic](cpp/2026-10-31-big-integer-arithmetic) |
| 2026-11-01 | Longest increasing subsequence (patience sorting) | Python | [python/2026-11-01-longest-increasing-subsequence](python/2026-11-01-longest-increasing-subsequence) |
| 2026-11-02 | JSON parser (recursive descent) | TypeScript | [typescript/2026-11-02-json-parser](typescript/2026-11-02-json-parser) |
| 2026-11-03 | Counting sort and LSD radix sort | Java | [java/2026-11-03-counting-and-radix-sort](java/2026-11-03-counting-and-radix-sort) |
| 2026-11-04 | Sparse matrix (CSR format) | C++ | [cpp/2026-11-04-sparse-matrix](cpp/2026-11-04-sparse-matrix) |
| 2026-11-05 | Coin change (min coins and number of ways) | Python | [python/2026-11-05-coin-change](python/2026-11-05-coin-change) |
| 2026-11-06 | Binary search variants (bounds, rotated, answer search) | TypeScript | [typescript/2026-11-06-binary-search-variants](typescript/2026-11-06-binary-search-variants) |
| 2026-11-07 | Bounded blocking queue (lock + two conditions) | Java | [java/2026-11-07-bounded-blocking-queue](java/2026-11-07-bounded-blocking-queue) |
| 2026-11-08 | Bit manipulation tricks | C++ | [cpp/2026-11-08-bit-manipulation-tricks](cpp/2026-11-08-bit-manipulation-tricks) |
| 2026-11-09 | KMP string search (prefix function) | Python | [python/2026-11-09-kmp-string-search](python/2026-11-09-kmp-string-search) |
| 2026-11-10 | Union-find (path compression + union by size) | TypeScript | [typescript/2026-11-10-union-find](typescript/2026-11-10-union-find) |
| 2026-11-11 | Producer-consumer executor (fixed worker pool) | Java | [java/2026-11-11-producer-consumer-executor](java/2026-11-11-producer-consumer-executor) |
| 2026-11-12 | Sieve of Eratosthenes (plus linear and segmented sieves) | C++ | [cpp/2026-11-12-sieve-of-eratosthenes](cpp/2026-11-12-sieve-of-eratosthenes) |
| 2026-11-13 | Rabin-Karp rolling-hash search | Python | [python/2026-11-13-rabin-karp](python/2026-11-13-rabin-karp) |
| 2026-11-14 | Topological sort (Kahn's algorithm) | TypeScript | [typescript/2026-11-14-topological-sort-kahn](typescript/2026-11-14-topological-sort-kahn) |
<!-- INDEX:END -->
