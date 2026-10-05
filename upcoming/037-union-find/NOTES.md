# Union-find (path compression + union by size)

**Problem:** Maintain a partition of `n` elements under two operations: merge the sets containing `a` and `b`, and ask whether `a` and `b` are in the same set. Also track the number of sets and each set's size.

## Approach
- Each set is a tree; `parent[x]` points toward the root, and the root identifies the set.
- **find** walks to the root, then a second pass rewrites every node on the path to point directly at it (*path compression*). Iterative, so a 200k-long chain can't overflow the stack.
- **union** links the smaller tree's root under the larger's (*union by size*) and decrements the component count.
- `Int32Array` storage keeps it compact and fast.
- `countIslands` shows a typical use: union adjacent land cells, then `components - water`.

## Complexity
| Operation | Time (amortized) | Space |
|-----------|------------------|-------|
| find / union / connected | O(α(n)) ≈ O(1) | — |
| construction | O(n) | O(n) |

## Interview talking points
- α(n), the inverse Ackermann function, is ≤ 4 for any realistic `n`. With only one of the two optimizations you get O(log n).
- Union by rank vs. by size: equivalent bounds; size is also useful information.
- Uses: Kruskal's MST, connected components, cycle detection in undirected graphs, accounts merge, percolation, image segmentation.
- No efficient "split": deletions need offline tricks (reverse time, or DSU with rollback).
- For string or sparse keys, map them to indices first (or use a `Map`-based parent).

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
