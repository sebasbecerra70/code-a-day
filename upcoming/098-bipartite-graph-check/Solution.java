import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Result of a bipartiteness check: either a valid 2-coloring, or an odd
 * cycle proving none exists. Both are certificates you can verify independently.
 */
record BipartiteResult(boolean bipartite, int[] colors, List<Integer> oddCycle) {}

class Bipartite {
    /**
     * BFS 2-coloring of an undirected graph given as adjacency lists. Handles
     * disconnected graphs by starting a BFS from every uncolored vertex.
     */
    static BipartiteResult check(List<List<Integer>> adj) {
        int n = adj.size();
        int[] color = new int[n];
        Arrays.fill(color, -1);
        int[] parent = new int[n];
        int[] depth = new int[n];
        for (int s = 0; s < n; s++) {
            if (color[s] != -1) continue;
            color[s] = 0;
            parent[s] = -1;
            Deque<Integer> q = new ArrayDeque<>();
            q.add(s);
            while (!q.isEmpty()) {
                int u = q.poll();
                for (int v : adj.get(u)) {
                    if (color[v] == -1) {
                        color[v] = 1 - color[u];
                        parent[v] = u;
                        depth[v] = depth[u] + 1;
                        q.add(v);
                    } else if (color[v] == color[u]) {
                        return new BipartiteResult(false, null, oddCycle(u, v, parent, depth));
                    }
                }
            }
        }
        return new BipartiteResult(true, color, List.of());
    }

    /**
     * Edge (u, v) joins two same-colored vertices. Their BFS tree paths meet at
     * the lowest common ancestor; the two paths plus the edge form an odd cycle,
     * since same color means equal depth parity.
     */
    private static List<Integer> oddCycle(int u, int v, int[] parent, int[] depth) {
        List<Integer> fromU = new ArrayList<>(), fromV = new ArrayList<>();
        while (depth[u] > depth[v]) {
            fromU.add(u);
            u = parent[u];
        }
        while (depth[v] > depth[u]) {
            fromV.add(v);
            v = parent[v];
        }
        while (u != v) {
            fromU.add(u);
            fromV.add(v);
            u = parent[u];
            v = parent[v];
        }
        fromU.add(u); // the LCA
        Collections.reverse(fromV);
        fromU.addAll(fromV);
        return fromU; // u ... lca ... v, and the edge v-u closes it
    }

    /** Builds adjacency lists for an undirected graph with n vertices. */
    static List<List<Integer>> graph(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) {
            adj.get(e[0]).add(e[1]);
            if (e[0] != e[1]) adj.get(e[1]).add(e[0]);
        }
        return adj;
    }
}
