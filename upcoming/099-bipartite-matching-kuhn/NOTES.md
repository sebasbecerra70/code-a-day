# Maximum bipartite matching (Kuhn's algorithm)

**Problem:** Given a bipartite graph (for example, workers and the jobs each can do), find the largest set of edges such that no vertex is used twice.

## Approach
- An **augmenting path** starts at a free left vertex, alternates between unmatched and matched edges, and ends at a free right vertex. Flipping every edge on it grows the matching by one.
- **Berge's theorem:** a matching is maximum iff no augmenting path exists.
- **Kuhn's algorithm:** for each free left vertex `l`, DFS over its neighbors `r`. If `r` is free, match it. Otherwise, recursively try to move `r`'s current partner to some other right vertex; if that works, take `r`. A `visited` array over right vertices (reset per start vertex) prevents revisiting.
- A **greedy warm start** matches easy pairs first, which in practice removes most of the DFS work.

## Complexity
| Step | Time | Space |
|------|------|-------|
| one augmentation attempt | O(V + E) | O(V) |
| Kuhn total | O(V · E) | O(V + E) |
| Hopcroft-Karp (comparison) | O(E √V) | O(V + E) |

## Interview talking points
- It's a special case of max flow: source → every left vertex, left → right edges, every right vertex → sink, all capacity 1. Kuhn's DFS is Ford-Fulkerson on that network.
- **König's theorem:** in bipartite graphs, maximum matching size equals minimum vertex cover size.
- Hopcroft-Karp finds many shortest augmenting paths per phase with BFS + DFS; use it for large graphs.
- Weighted version (min-cost assignment) needs the Hungarian algorithm, O(n³).
- Applications: job assignment, stable pairings without preferences, domino tilings, minimum path cover in a DAG (n − max matching).

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
