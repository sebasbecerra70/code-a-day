# Segment tree (range sum, lazy range add)

**Problem:** Support range-sum queries on an array that keeps changing: (1) point updates, and (2) adding a value to a whole range, both in O(log n).

## Approach
- **Iterative tree (point update):** store leaves at `t[n..2n)` and each internal node `i` as `t[2i] + t[2i+1]`. Update a leaf and recompute ancestors. To query `[lo, hi)`, move both ends up the tree; whenever an end is a "boundary" child (`lo` odd / `hi` odd), add that node and step inward. Works for any `n`, not just powers of two.
- **Lazy propagation (range add):** recursive tree over `[l, r]` with `4n` slots. If an update fully covers a node, bump its sum by `delta·len` and record `delta` in `lazy[node]` instead of touching children. Before descending into a node later, *push* its pending delta down to both children.
- Half-open `[lo, hi)` ranges throughout, matching Python slicing.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| build | O(n) | O(n) (2n iterative, 4n recursive) |
| point update / query | O(log n) | — |
| range add (lazy) | O(log n) | — |

## Interview talking points
- Any associative operation works: min, max, gcd, XOR, matrix product. Lazy needs the update to compose and to apply to a whole segment's aggregate in O(1).
- Fenwick tree is simpler and lighter for prefix sums, but can't do range-min easily.
- If the array never changes, a prefix-sum array gives O(1) queries; sparse tables give O(1) range-min.
- Extensions: persistent segment trees (versions), merge-sort tree, segment tree beats, dynamic/sparse trees for huge coordinate ranges.

## Run
From this folder: `python -m pytest -q`
