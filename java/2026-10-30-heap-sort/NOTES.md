# Heap sort (in-place, bottom-up heapify)

**Problem:** Sort an array in place in guaranteed O(n log n) time with O(1) extra memory, using a binary max-heap laid out inside the array itself.

## Approach
- The array is an implicit complete binary tree: the children of `i` are `2i+1` and `2i+2`.
- **Build the heap bottom-up:** call `siftDown` on every internal node from `n/2 − 1` down to 0. This is O(n), not O(n log n).
- **Extract repeatedly:** swap the max (`a[0]`) with the last element of the heap, shrink the heap by one and sift the new root down. The sorted suffix grows from the right.
- `siftDown` moves a "hole" down instead of swapping at every level, which halves the writes.
- **Partial sort (`topK`):** stop after k extractions, which costs O(n + k log n).

## Complexity
| Phase | Time | Space |
|-------|------|-------|
| Build heap | O(n) | O(1) |
| n extractions | O(n log n) | O(1) |
| Total, all cases | O(n log n) | O(1) |
| topK | O(n + k log n) | O(n) for the copy |

## Interview talking points
- Why is heapify O(n)? Most nodes are near the bottom: about n/2 leaves do no work, n/4 nodes sift at most one level, and so on. The sum Σ h·n/2^(h+1) converges to O(n).
- Heap sort guarantees O(n log n) worst case with O(1) space, which neither quicksort (O(n²) worst case) nor merge sort (O(n) space) offers. That's why introsort uses it as a fallback.
- In practice it's slower than quicksort: sift-down jumps around the array, so cache locality is poor, and it isn't stable.
- Sorting ascending needs a **max**-heap, because the max is swapped to the end each round.
- Related: a min-heap of size k gives the top-k of a stream in O(n log k) with O(k) memory.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
