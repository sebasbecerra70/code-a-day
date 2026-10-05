# Bipartite graph check (BFS 2-coloring + odd cycle)

**Problem:** Decide whether an undirected graph's vertices can be split into two sets with every edge crossing between them. Return a proof either way: a valid 2-coloring, or an odd cycle.

## Approach
- **BFS 2-coloring:** color the start vertex 0, and give every newly discovered neighbor the opposite color. If an edge connects two vertices of the **same** color, the graph isn't bipartite.
- Start a BFS from every uncolored vertex, so disconnected graphs are handled.
- **Odd-cycle certificate:** when edge (u, v) has equal colors, u and v have the same depth parity in the BFS tree. Walk both up to their lowest common ancestor. Path u→LCA, path LCA→v and the edge v–u form a cycle of length `d(u) + d(v) − 2·d(lca) + 1`, which is odd. A self-loop is a cycle of length 1.
- Tests verify both kinds of certificate directly, and compare the yes/no answer against brute force over all 2ⁿ colorings.

## Complexity
| | Time | Space |
|-|------|-------|
| Check + certificate | O(V + E) | O(V) |
| Brute force | O(2^V · E) | O(1) |

## Interview talking points
- **König's theorem:** a graph is bipartite if and only if it has no odd cycle. Returning the cycle proves the "no" answer, which is good practice in interviews and in production (debuggable failures).
- DFS works just as well. Union-find also works: for each vertex, union all its neighbors together and check that the vertex never ends up in its neighbors' set.
- The trap is checking only from vertex 0 and missing other components.
- Applications: two-team scheduling, "possible bipartition" (people who dislike each other), conflict-free scheduling, and as a precondition for bipartite matching (Hopcroft–Karp, Kuhn).
- 2-coloring is linear, but k-coloring for k ≥ 3 is NP-complete.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
