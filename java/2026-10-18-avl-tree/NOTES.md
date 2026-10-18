# AVL tree (self-balancing BST)

**Problem:** Implement a binary search tree that stays balanced under any sequence of inserts and deletes, guaranteeing O(log n) operations. Add order statistics: `select(k)` (k-th smallest) and `rank(key)`.

## Approach
- Each node caches its **height** and **subtree size**. The balance factor is `height(left) − height(right)` and must stay in {−1, 0, 1}.
- Insert and delete recursively like a normal BST; on the way back up, call `rebalance` on each node:
  - **Left-Left:** rotate right. **Right-Right:** rotate left.
  - **Left-Right:** rotate the left child left, then the node right. **Right-Left:** the mirror.
- A rotation changes only three pointers and keeps in-order order intact; heights/sizes are recomputed bottom-up (`update(child)` before `update(new parent)`).
- Delete with two children: copy the in-order successor's key, then delete the successor from the right subtree.
- Subtree sizes make `select` and `rank` single root-to-leaf walks.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| contains / insert / remove | O(log n) | O(log n) recursion |
| select / rank | O(log n) | O(1) |
| inOrder | O(n) | O(n) |
| Height bound | ≤ 1.44 · log₂(n + 2) | — |

## Interview talking points
- Why the height bound? The sparsest AVL tree of height h has `N(h) = N(h−1) + N(h−2) + 1` nodes, a Fibonacci recurrence, so `n` grows exponentially with `h`.
- AVL vs red-black: AVL is more strictly balanced (faster lookups); red-black does fewer rotations per update (at most 2 for insert, 3 for delete), which is why `TreeMap` and `std::map` use red-black trees.
- Insert needs at most one (single or double) rotation; delete may rotate at every level up to the root.
- Augmenting nodes with subtree size (an *order-statistic tree*) is a common follow-up; the same trick works for sums, min/max, and so on.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
