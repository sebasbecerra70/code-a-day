# Quickselect: k-th largest element

**Problem:** Given an unsorted array and `k`, return the k-th largest element (LeetCode 215) without fully sorting.

## Approach
- The k-th largest is the element at index `n - k` in ascending order.
- **Quickselect:** pick a random pivot and partition. The pivot's final position tells us which side holds the target, so we recurse (loop) into only that side.
- A **three-way partition** (`< pivot | == pivot | > pivot`, Dutch national flag) means arrays full of duplicates still shrink every round.
- Works on a copy so the caller's list isn't reordered.
- **Alternative:** keep a min-heap of the `k` largest seen so far; its root is the answer. O(n log k) and works on streams.

## Complexity
| Method | Time | Space |
|--------|------|-------|
| Quickselect | O(n) average, O(n²) worst (vanishingly unlikely with random pivots) | O(n) copy (O(1) in place) |
| Heap of size k | O(n log k) | O(k) |
| Sort | O(n log n) | O(n) |

## Interview talking points
- Why average O(n)? Each round discards a constant fraction in expectation: n + n/2 + n/4 + ... = 2n.
- Median of medians gives worst-case O(n) but with large constants; introselect (C++ `std::nth_element`) falls back to it.
- Lomuto vs. Hoare vs. three-way partitioning; Lomuto degrades to O(n²) on all-equal input.
- When k is tiny or data streams in, prefer the heap. When many queries hit the same array, sort once.

## Run
From this folder: `python -m pytest -q`
