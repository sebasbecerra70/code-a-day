# Topological sort (Kahn's algorithm)

**Problem:** Given tasks and their prerequisites, produce an order where every task comes after its prerequisites, or report that a cycle makes it impossible (LeetCode 207/210, build systems, package managers).

## Approach
- Build edges `dependency -> dependent` and count each node's in-degree (number of unmet prerequisites). Nodes mentioned only as dependencies are added automatically.
- **Kahn's algorithm:** start a queue with all in-degree-0 nodes. Pop a node, append it to the order, decrement each dependent's in-degree, and enqueue any that hit 0.
- If the order is shorter than the node count, the leftover nodes (in-degree still > 0) are on or behind a cycle; throw `CycleError` listing them.
- **Lexicographic variant:** use an ordered "ready" set instead of a FIFO queue so the result is deterministic.
- **Parallel levels:** process the frontier one whole wave at a time; each wave can run concurrently.

## Complexity
| Variant | Time | Space |
|---------|------|-------|
| topoSort / parallelLevels | O(V + E) | O(V + E) |
| lexicographic (sorted array) | O(V² + E); O((V + E) log V) with a heap | O(V + E) |

## Interview talking points
- DFS alternative: post-order DFS, reversed, with white/gray/black coloring for cycle detection. Kahn's is iterative (no recursion depth issue) and gives the cycle members for free.
- A DAG can have many valid orders; the order is unique iff every step has exactly one ready node (a Hamiltonian path).
- Number of parallel levels = length of the longest path = minimum number of rounds with unlimited workers.
- Uses: build tools (make, Bazel), package install order, spreadsheet recalculation, course schedule, task schedulers.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
