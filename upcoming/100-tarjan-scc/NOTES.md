# Tarjan's strongly connected components (iterative)

**Problem:** Partition a directed graph into strongly connected components (maximal sets where every node reaches every other) in linear time. Use them to build the condensation DAG and to solve 2-SAT.

## Approach
- One DFS. Each node gets a discovery `index` and a `low` value: the smallest index reachable from its DFS subtree using at most one edge back into nodes still on the stack.
- Nodes are pushed onto a separate SCC stack when discovered. When DFS finishes `v` and `low[v] == index[v]`, `v` is the root of an SCC: pop the stack down to `v`, and that's the component.
- Only edges to nodes **still on the stack** update `low`. Edges into already-finished SCCs are cross edges and must be ignored.
- **Iterative:** an explicit `(node, neighbor_position)` work stack replaces recursion, so 100k-node paths don't blow Python's recursion limit. On "return", propagate `low[child]` into the parent.
- SCCs are emitted in **reverse topological order** of the condensation, so reversing them gives a topological order for free.
- **2-SAT:** clause `(a ∨ b)` gives implications `¬a → b` and `¬b → a`. The formula is unsatisfiable iff `x` and `¬x` share an SCC. Otherwise set `x` true iff its SCC comes later in topological order.

## Complexity
| Aspect | Cost |
|--------|------|
| Time | O(V + E) |
| Space | O(V) (plus adjacency) |
| 2-SAT | O(vars + clauses) |

## Interview talking points
- Kosaraju's algorithm also runs in O(V + E) but needs two passes and the transposed graph. Tarjan does one pass.
- Why the `on_stack` check matters: without it, cross edges into completed SCCs would wrongly merge components.
- Uses: deadlock detection (cycles in wait-for graphs), finding dependency cycles in build systems or imports, compressing a graph to a DAG for DP, and 2-SAT.
- Tarjan's other famous DFS-low-link algorithms find bridges and articulation points in undirected graphs.
- Tests check against brute-force mutual reachability and brute-force 2-SAT enumeration.

## Run
From this folder: `python -m pytest -q`
