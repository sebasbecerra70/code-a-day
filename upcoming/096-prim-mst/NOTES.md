# Prim's minimum spanning tree (heap and dense variants)

**Problem:** Find a minimum-weight set of edges connecting all nodes of an undirected weighted graph. Provide a heap-based version for sparse graphs and an O(V²) version for dense ones.

## Approach
- Grow a single tree from a start node. At each step add the **cheapest edge leaving the tree**. By the cut property (tree vs rest), that edge is in some MST.
- **Heap version (lazy):** push `(weight, node, parent)` for every edge out of a newly added node. When popping, skip nodes already in the tree. This is simpler than decrease-key and has the same O(E log V) bound.
- **Dense version:** keep `best[v]`, the cheapest edge from the tree to `v`. Each round, linear-scan for the minimum, add it, and relax its row. O(V²) total with no heap overhead, which is optimal when E ≈ V².
- `prim_forest` restarts from each uncovered node to handle disconnected graphs. `prim_dense` returns ∞ if the graph is disconnected.
- Tests cross-check both versions against Kruskal on random multigraphs with self-loops and negative weights.

## Complexity
| Variant | Time | Space |
|--------|------|------|
| Lazy binary heap | O(E log E) = O(E log V) | O(E) heap |
| Array (dense) | O(V²) | O(V) |
| Fibonacci heap | O(E + V log V) | O(V) |

## Interview talking points
- Prim vs Dijkstra: identical structure, but Prim keys on the *edge weight* into the tree, while Dijkstra keys on the *total distance* from the source.
- Prim vs Kruskal: Prim needs adjacency lists and grows one component. Kruskal needs a sorted edge list and a DSU and grows a forest.
- For "connect all points" on a complete graph, the dense O(V²) Prim beats building O(V²) edges for Kruskal.
- Self-loops are ignored naturally (the node is already in the tree), and parallel edges are harmless.
- MST ≠ shortest-path tree: the MST minimizes total weight, not distance from a root.

## Run
From this folder: `python -m pytest -q`
