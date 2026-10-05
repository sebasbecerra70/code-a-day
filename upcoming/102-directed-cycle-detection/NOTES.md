# Directed cycle detection (DFS colors + Kahn)

**Problem:** Detect whether a directed graph has a cycle and return one as proof; otherwise return a topological order. Also find the "eventually safe" vertices, those from which every path ends at a sink.

## Approach
- **Three-color DFS:** WHITE (unvisited), GRAY (on the current DFS path) and BLACK (finished). An edge to a **GRAY** vertex is a back edge, which means a cycle. An edge to a BLACK vertex is a cross or forward edge and is harmless; the diamond `0→1→3, 0→2→3` must *not* count as a cycle.
- **Certificate:** on back edge u→v, follow `parent` from u up to v and reverse.
- **Iterative DFS** with a per-vertex edge cursor, so a 200k-vertex path doesn't overflow the JVM stack.
- **Kahn's algorithm:** repeatedly remove in-degree-0 vertices. If not all vertices are removed, there's a cycle; otherwise the removal order is a topological order.
- **Safe vertices:** run Kahn on the *reversed* graph starting from sinks (out-degree 0). A vertex becomes safe once all its successors are safe.
- Tests compare all three against brute-force reachability on random graphs, and verify each cycle and topological order directly.

## Complexity
| Algorithm | Time | Space |
|-----------|------|-------|
| DFS find cycle | O(V + E) | O(V) |
| Kahn topological order | O(V + E) | O(V) |
| Safe vertices | O(V + E) | O(V + E) for the reverse graph |

## Interview talking points
- Undirected vs directed: in an undirected graph any visited non-parent neighbor means a cycle. In a directed graph you need the GRAY/BLACK distinction, and two colors aren't enough.
- DFS or Kahn? Both are O(V + E). Kahn gives the order directly and is easy to parallelize by levels. DFS gives the actual cycle more naturally.
- Real uses: build systems and package managers (dependency cycles), deadlock detection in wait-for graphs, spreadsheet formula cycles, and course schedule problems.
- Recursive DFS is fine for small graphs, but production graphs can be deep, so mention the iterative version or raising the stack size.
- Tarjan's or Kosaraju's SCC algorithm generalizes this: every SCC of size > 1 (or with a self-loop) is a cycle, and condensing SCCs yields a DAG.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
