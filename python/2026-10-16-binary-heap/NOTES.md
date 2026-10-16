# Binary min-heap (with heapify and heapsort)

**Problem:** Implement a priority queue that returns the smallest element quickly, supports inserts, and can be built from an existing list in linear time.

## Approach
- Store a complete binary tree in a list: children of `i` are `2i+1` and `2i+2`, parent is `(i-1)//2`.
- **push:** append, then *sift up* while smaller than the parent.
- **pop:** swap root with the last element, remove it, then *sift down* the new root toward the smaller child.
- **heapify:** sift down every internal node from the last one back to the root. That's O(n), not O(n log n), because most nodes are near the bottom.
- An optional `key` function gives max-heaps or ordering by a field without wrapper objects.
- `pushpop` does push+pop in a single sift.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| push / pop | O(log n) | — |
| peek | O(1) | — |
| heapify | O(n) | O(n) |
| heapsort | O(n log n) | O(n) here (in-place is possible) |

## Interview talking points
- Why is heapify O(n)? Sum over levels of (nodes at level × height) = n·Σ h/2^h = O(n).
- Python's `heapq` is a min-heap on plain lists; negate values or use `(-priority, item)` for max-heap.
- Ties with unorderable payloads: add a counter `(priority, seq, item)` for stability.
- Decrease-key needs an index map from item to position (indexed heap).
- Uses: Dijkstra/Prim, top-k, merge k sorted lists, schedulers, median maintenance with two heaps.

## Run
From this folder: `python -m pytest -q`
