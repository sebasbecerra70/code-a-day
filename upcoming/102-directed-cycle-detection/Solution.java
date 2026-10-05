import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

class DirectedCycle {
    private static final int WHITE = 0, GRAY = 1, BLACK = 2;

    /**
     * Finds one directed cycle, returned as vertices in order (the last vertex
     * has an edge back to the first), or empty if the graph is a DAG.
     * The DFS is iterative, so long paths can't overflow the call stack.
     */
    static Optional<List<Integer>> findCycle(List<List<Integer>> adj) {
        int n = adj.size();
        int[] color = new int[n];
        int[] parent = new int[n];
        int[] nextEdge = new int[n]; // per-vertex cursor into its adjacency list
        for (int s = 0; s < n; s++) {
            if (color[s] != WHITE) continue;
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(s);
            color[s] = GRAY;
            parent[s] = -1;
            while (!stack.isEmpty()) {
                int u = stack.peek();
                if (nextEdge[u] < adj.get(u).size()) {
                    int v = adj.get(u).get(nextEdge[u]++);
                    if (color[v] == WHITE) {
                        color[v] = GRAY;
                        parent[v] = u;
                        stack.push(v);
                    } else if (color[v] == GRAY) {
                        // Back edge u -> v: v is an ancestor still on the stack. Walk parents from u to v.
                        List<Integer> cycle = new ArrayList<>();
                        for (int x = u; x != v; x = parent[x]) cycle.add(x);
                        cycle.add(v);
                        Collections.reverse(cycle);
                        return Optional.of(cycle);
                    }
                    // BLACK: fully explored and known acyclic beyond it; skip.
                } else {
                    color[u] = BLACK;
                    stack.pop();
                }
            }
        }
        return Optional.empty();
    }

    static boolean hasCycle(List<List<Integer>> adj) {
        return findCycle(adj).isPresent();
    }

    /**
     * Kahn's algorithm: repeatedly remove vertices with in-degree 0. If some
     * vertices are never removed, they're on or downstream of a cycle.
     * Returns a topological order, or empty if there's a cycle.
     */
    static Optional<List<Integer>> topologicalOrder(List<List<Integer>> adj) {
        int n = adj.size();
        int[] indeg = new int[n];
        for (List<Integer> out : adj) for (int v : out) indeg[v]++;
        Deque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < n; i++) if (indeg[i] == 0) q.add(i);
        List<Integer> order = new ArrayList<>();
        while (!q.isEmpty()) {
            int u = q.poll();
            order.add(u);
            for (int v : adj.get(u)) if (--indeg[v] == 0) q.add(v);
        }
        return order.size() == n ? Optional.of(order) : Optional.empty();
    }

    /**
     * "Eventually safe" vertices: every path from them ends at a sink, i.e. they
     * can't reach a cycle. Kahn's algorithm on the reversed graph, peeling off sinks.
     */
    static List<Integer> safeVertices(List<List<Integer>> adj) {
        int n = adj.size();
        List<List<Integer>> rev = new ArrayList<>();
        for (int i = 0; i < n; i++) rev.add(new ArrayList<>());
        int[] outdeg = new int[n];
        for (int u = 0; u < n; u++) {
            outdeg[u] = adj.get(u).size();
            for (int v : adj.get(u)) rev.get(v).add(u);
        }
        Deque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < n; i++) if (outdeg[i] == 0) q.add(i);
        boolean[] safe = new boolean[n];
        while (!q.isEmpty()) {
            int v = q.poll();
            safe[v] = true;
            for (int u : rev.get(v)) if (--outdeg[u] == 0) q.add(u);
        }
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < n; i++) if (safe[i]) out.add(i);
        return out;
    }

    static List<List<Integer>> graph(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) adj.get(e[0]).add(e[1]);
        return adj;
    }
}
