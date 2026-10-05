# Bridges and articulation points (Tarjan low-link)

**Problem:** In an undirected graph (possibly disconnected, with parallel edges and self-loops), find every **bridge** (an edge whose removal increases the number of connected components) and every **articulation point** (a vertex whose removal does).

## Approach
- One DFS assigns each vertex a discovery time `tin[v]` and a **low-link** `low[v]`: the earliest discovery time reachable from v's subtree using tree edges down plus at most one back edge.
- For a tree edge p→u, after finishing u, set `low[p] = min(low[p], low[u])`:
  - **Bridge** if `low[u] > tin[p]`: nothing under u reaches p or above, so the edge is the only connection.
  - **Articulation point** if `p` isn't the root and `low[u] ≥ tin[p]`: u's subtree can't bypass p.
  - **Root** is an articulation point iff it has two or more DFS children.
- **Parallel edges:** skip the *edge id* we arrived by, not the parent vertex. A second edge to the parent then counts as a back edge, so a doubled edge is correctly not a bridge.
- **Iterative DFS** with per-vertex edge cursors handles a 200k-vertex path without a stack overflow.
- Tests compare against brute force: remove each edge or vertex and count components with union-find.

## Complexity
| | Time | Space |
|-|------|-------|
| Tarjan (both outputs) | O(V + E) | O(V + E) |
| Brute force | O(E · (V + E)) for bridges, O(V · (V + E)) for vertices | O(V) |

## Interview talking points
- The bridge condition uses strict `>` and the articulation condition uses `≥`. A back edge *to* p saves the edge p–u but not the vertex p.
- The root needs a special case: it has no ancestors, so the low-link condition always holds for it; only having two or more DFS children makes it a cut vertex.
- The classic bug is skipping the parent *vertex* instead of the parent *edge*, which breaks on multigraphs.
- Applications: single points of failure in networks (LeetCode "critical connections"), road planning, and as the basis for 2-edge-connected and biconnected components (block-cut trees).
- The same low-link idea underlies Tarjan's SCC algorithm for directed graphs.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
