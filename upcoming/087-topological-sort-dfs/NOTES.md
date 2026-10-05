# Topological sort (DFS with cycle detection)

**Problem:** Order the vertices of a directed graph so that every edge `u → v` has `u` before `v` (for example, course prerequisites or build dependencies). If no order exists because of a cycle, report one concrete cycle.

## Approach
- **Three colors:** White = unvisited, Gray = on the current DFS path, Black = finished.
- Run DFS from every White vertex. When a vertex finishes (all its descendants are Black), append it to a post-order list. **Reverse post-order** is a topological order: a vertex finishes only after everything reachable from it.
- An edge into a **Gray** vertex is a back edge, which means a cycle. Walk `parent` pointers from the current vertex back to the Gray one to print the cycle.
- Edges into Black vertices (forward/cross edges) are fine, which is why two colors aren't enough.
- The DFS is iterative (a stack of `(vertex, next neighbor index)` frames) so a 200k-long chain doesn't overflow the call stack.
- **Kahn's algorithm** for comparison: repeatedly remove a vertex with in-degree 0. With a min-heap it yields the lexicographically smallest order; if some vertices are never removed, there's a cycle.

## Complexity
| Algorithm | Time | Space |
|-----------|------|-------|
| DFS topo sort + cycle | O(V + E) | O(V + E) |
| Kahn (queue) | O(V + E) | O(V + E) |
| Kahn (min-heap, lexicographic) | O(V log V + E) | O(V + E) |

## Interview talking points
- A topological order exists iff the graph is a DAG.
- Visited/unvisited alone can't detect cycles in directed graphs: a diamond revisits a finished vertex without any cycle.
- Kahn's is easier to explain for "can all courses be finished?" and naturally gives layers (parallel build steps); DFS gives the cycle for free.
- Uses: build systems (make, Bazel), package managers, spreadsheet recalculation, scheduling, and DP over DAGs (longest path).

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
