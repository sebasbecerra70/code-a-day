# Lowest common ancestor (binary lifting)

**Problem:** Given a rooted tree with `n` vertices, answer many queries "what is the lowest common ancestor of `a` and `b`?" quickly. Also support the k-th ancestor of a vertex and the distance between two vertices.

## Approach
- Root the tree with an iterative DFS to get each vertex's parent and depth.
- Build a jump table: `up[0][v]` = parent, and `up[j][v] = up[j−1][ up[j−1][v] ]` (the 2ʲ-th ancestor). The root's ancestor is the root itself, so jumps past the top saturate.
- **k-th ancestor:** jump by `2ʲ` for every set bit `j` of `k`.
- **LCA(a, b):** lift the deeper vertex to the other's depth. If they're now equal, that's the answer. Otherwise, for `j` from high to low, jump both by `2ʲ` whenever their ancestors differ. They end as children of the LCA, so return `up[0][a]`.
- `distance(a, b) = depth[a] + depth[b] − 2·depth[lca]`.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| preprocessing | O(n log n) | O(n log n) |
| query / kthAncestor / distance | O(log n) | O(1) |
| naive (walk up) per query | O(n) | O(1) |

## Interview talking points
- Why jump only while the ancestors *differ*? Jumping to a common ancestor might overshoot the lowest one; staying just below it is the safe invariant.
- Iterative DFS matters: a path of 10⁵ nodes overflows the call stack with naive recursion.
- Alternatives: Euler tour + sparse table RMQ gives O(1) queries after O(n log n) preprocessing; Tarjan's offline LCA with union-find answers a batch in near-linear time.
- For a BST, LCA is simpler: walk down from the root until `a` and `b` fall on different sides.
- Binary lifting also answers path aggregates (max edge on the path) by storing values alongside `up`.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
