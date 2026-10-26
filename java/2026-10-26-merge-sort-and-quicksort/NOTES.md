# Merge sort and quicksort (stable vs in-place)

**Problem:** Implement the two classic O(n log n) divide-and-conquer sorts: a stable merge sort (top-down generic and bottom-up iterative) and an in-place randomized quicksort that copes with duplicates.

## Approach
- **Merge sort (top-down):** split in half, sort each half, merge with one scratch buffer allocated up front. Taking from the left run on ties (`<=`) keeps it stable. Two cheap tweaks: insertion sort below 16 elements, and skip the merge when `a[mid-1] <= a[mid]` (sorted input becomes O(n)).
- **Merge sort (bottom-up):** merge runs of width 1, 2, 4, ..., ping-ponging between two arrays. No recursion.
- **Quicksort:** random pivot plus **3-way partitioning** (`< pivot | == pivot | > pivot`), so an all-equal array finishes in one pass instead of degrading to O(n²). Recurse on the smaller side and loop on the larger, which caps stack depth at O(log n).
- Lomuto partition is included for comparison: simpler, but it degrades badly on duplicates.

## Complexity
| Algorithm | Best | Average | Worst | Extra space | Stable |
|-----------|------|---------|-------|-------------|--------|
| Merge sort | O(n) with the sorted check | O(n log n) | O(n log n) | O(n) | Yes |
| Quicksort (random, 3-way) | O(n) all-equal | O(n log n) | O(n²), vanishingly unlikely | O(log n) stack | No |

## Interview talking points
- Java's `Arrays.sort` uses dual-pivot quicksort for primitives (stability is meaningless for them) and TimSort, a merge sort that exploits existing runs, for objects (stability matters).
- Quicksort usually wins in practice: it works in place, scans memory sequentially and has a tight inner loop. Merge sort wins on linked lists (no extra space needed), for external sorting and when you need stability or a worst-case guarantee.
- Introsort (C++ `std::sort`) falls back to heapsort when recursion gets too deep, which bounds the worst case at O(n log n).
- A fixed first or last element as pivot gives O(n²) on sorted input, and adversarial inputs can force it. Randomizing the pivot (or using median-of-three) fixes this.
- Recursing on the smaller partition is the standard trick to guarantee O(log n) stack.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
