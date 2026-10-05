import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * Finds bridges (edges whose removal disconnects the graph) and articulation
 * points (vertices whose removal does) in one DFS, using discovery times and
 * low-links. Undirected multigraph; edges are identified by index so parallel
 * edges are handled correctly.
 */
class CutFinder {
    final List<Integer> bridges = new ArrayList<>();          // edge indices, ascending
    final TreeSet<Integer> articulationPoints = new TreeSet<>();

    private final int n;
    private final int[][] edges;
    private final List<List<Integer>> incident = new ArrayList<>(); // vertex -> edge ids
    private final int[] tin, low;
    private int timer;

    CutFinder(int n, int[][] edges) {
        this.n = n;
        this.edges = edges;
        for (int i = 0; i < n; i++) incident.add(new ArrayList<>());
        for (int i = 0; i < edges.length; i++) {
            incident.get(edges[i][0]).add(i);
            if (edges[i][0] != edges[i][1]) incident.get(edges[i][1]).add(i); // self-loops never matter
        }
        tin = new int[n];
        low = new int[n];
        Arrays.fill(tin, -1);
        for (int s = 0; s < n; s++) if (tin[s] == -1) dfs(s);
        bridges.sort(null);
    }

    private int other(int edge, int v) {
        return edges[edge][0] == v ? edges[edge][1] : edges[edge][0];
    }

    /**
     * Iterative DFS (deep graphs would overflow recursion). For each vertex we
     * remember the edge we arrived by, so we skip that exact edge, not every
     * edge to the parent. A parallel edge back to the parent is a real back edge.
     */
    private void dfs(int root) {
        int[] parentEdge = new int[n];
        int[] cursor = new int[n];
        int[] stack = new int[n];
        int top = 0;
        stack[top++] = root;
        parentEdge[root] = -1;
        tin[root] = low[root] = timer++;
        int rootChildren = 0;
        while (top > 0) {
            int u = stack[top - 1];
            if (cursor[u] < incident.get(u).size()) {
                int e = incident.get(u).get(cursor[u]++);
                if (e == parentEdge[u]) continue;
                int v = other(e, u);
                if (tin[v] == -1) {
                    parentEdge[v] = e;
                    tin[v] = low[v] = timer++;
                    stack[top++] = v;
                    if (u == root) rootChildren++;
                } else {
                    low[u] = Math.min(low[u], tin[v]); // back edge
                }
            } else {
                top--;
                if (u == root) continue;
                int p = other(parentEdge[u], u);
                low[p] = Math.min(low[p], low[u]);
                // Nothing in u's subtree reaches above u: the tree edge p-u is the only link.
                if (low[u] > tin[p]) bridges.add(parentEdge[u]);
                // Nothing in u's subtree reaches above p: removing p cuts u's subtree off.
                if (p != root && low[u] >= tin[p]) articulationPoints.add(p);
            }
        }
        // The root is a cut vertex iff it has two or more DFS children.
        if (rootChildren >= 2) articulationPoints.add(root);
    }

    /** Edges as {u, v} pairs for the bridges found. */
    List<int[]> bridgeEdges() {
        List<int[]> out = new ArrayList<>();
        for (int e : bridges) out.add(edges[e]);
        return out;
    }
}
