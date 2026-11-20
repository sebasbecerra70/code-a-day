# Fenwick tree (binary indexed tree)

**Problem:** Maintain an array under point updates (`values[i] += delta`) while answering prefix and range sums in O(log n), with less code and memory than a segment tree.

## Approach
- 1-based array `tree` where `tree[i]` holds the sum of the `lowbit(i) = i & -i` elements ending at `i`.
- **prefix_sum(k):** add `tree[k]`, then strip the lowest set bit (`k -= k & -k`) until 0. That's at most log n steps.
- **add(i, delta):** add to `tree[i]`, then move to the next covering node (`i += i & -i`).
- **range_sum(lo, hi) = prefix(hi) - prefix(lo).**
- **O(n) build:** copy values, then push each node's total into its parent `i + lowbit(i)` once.
- **lower_bound(target):** binary lifting over powers of two finds the first prefix ≥ target in O(log n) (needs non-negative values), e.g. "find the k-th element" in an order-statistics multiset.
- `count_inversions` is the standard application: coordinate-compress, then count earlier elements greater than each value.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| build | O(n) | O(n) |
| add / prefix_sum / range_sum | O(log n) | — |
| lower_bound | O(log n) | — |

## Interview talking points
- Why `i & -i`? In two's complement it isolates the lowest set bit; the tree's structure falls out of binary representations.
- Fenwick needs an invertible operation (sum, XOR) for range queries; for min/max use a segment tree.
- Range update + point query: store differences. Range update + range query: two BITs.
- 2-D BIT for grid prefix sums in O(log² n).
- Uses: inversion counting, rank queries, order statistics, frequency tables in arithmetic coding.

## Run
From this folder: `python -m pytest -q`
