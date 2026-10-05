import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class SolutionTest {
    private static int passed = 0;

    interface TestBody {
        void run() throws Exception;
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void expectThrows(Class<? extends Throwable> type, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + t, t);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " to be thrown");
    }

    static void run(String name, TestBody body) {
        try {
            body.run();
        } catch (Throwable t) {
            throw new AssertionError(name + " failed: " + t.getMessage(), t);
        }
        passed++;
    }

    static void verifyCycle(List<List<Integer>> adj, List<Integer> c) {
        check(!c.isEmpty() && new HashSet<>(c).size() == c.size(), "simple cycle " + c);
        for (int i = 0; i < c.size(); i++) {
            check(adj.get(c.get(i)).contains(c.get((i + 1) % c.size())), "missing edge in " + c);
        }
    }

    static void verifyTopo(List<List<Integer>> adj, List<Integer> order) {
        int[] pos = new int[adj.size()];
        for (int i = 0; i < order.size(); i++) pos[order.get(i)] = i;
        for (int u = 0; u < adj.size(); u++) for (int v : adj.get(u)) check(pos[u] < pos[v], "edge " + u + "->" + v);
    }

    // Reference: v is on a cycle iff v is reachable from one of its successors.
    static boolean[] onCycle(List<List<Integer>> adj) {
        int n = adj.size();
        boolean[] res = new boolean[n];
        for (int s = 0; s < n; s++) {
            boolean[] seen = new boolean[n];
            Deque<Integer> q = new ArrayDeque<>(adj.get(s));
            while (!q.isEmpty()) {
                int u = q.poll();
                if (u == s) res[s] = true;
                if (seen[u]) continue;
                seen[u] = true;
                q.addAll(adj.get(u));
            }
        }
        return res;
    }

    // Reference: v is safe iff no vertex reachable from v (including v) is on a cycle.
    static List<Integer> bruteSafe(List<List<Integer>> adj) {
        boolean[] cyc = onCycle(adj);
        List<Integer> out = new ArrayList<>();
        for (int s = 0; s < adj.size(); s++) {
            boolean[] seen = new boolean[adj.size()];
            Deque<Integer> q = new ArrayDeque<>(List.of(s));
            boolean ok = true;
            while (!q.isEmpty()) {
                int u = q.poll();
                if (seen[u]) continue;
                seen[u] = true;
                if (cyc[u]) ok = false;
                q.addAll(adj.get(u));
            }
            if (ok) out.add(s);
        }
        return out;
    }

    public static void main(String[] args) {
        run("emptyGraph", () -> {
            var g = DirectedCycle.graph(0, new int[0][]);
            check(!DirectedCycle.hasCycle(g) && DirectedCycle.topologicalOrder(g).get().isEmpty(), "empty");
        });

        run("dagHasNoCycle", () -> {
            var g = DirectedCycle.graph(6, new int[][] {{5, 2}, {5, 0}, {4, 0}, {4, 1}, {2, 3}, {3, 1}});
            check(!DirectedCycle.hasCycle(g), "DAG");
            verifyTopo(g, DirectedCycle.topologicalOrder(g).orElseThrow());
        });

        run("diamondIsNotACycle", () -> {
            // Undirected-style "visited" check would wrongly report a cycle here.
            var g = DirectedCycle.graph(4, new int[][] {{0, 1}, {0, 2}, {1, 3}, {2, 3}});
            check(!DirectedCycle.hasCycle(g), "cross edge to a BLACK vertex is fine");
        });

        run("selfLoop", () -> {
            var g = DirectedCycle.graph(2, new int[][] {{0, 1}, {1, 1}});
            check(DirectedCycle.findCycle(g).orElseThrow().equals(List.of(1)), "self-loop");
            check(DirectedCycle.topologicalOrder(g).isEmpty(), "kahn agrees");
        });

        run("cycleCertificate", () -> {
            var g = DirectedCycle.graph(5, new int[][] {{0, 1}, {1, 2}, {2, 3}, {3, 1}, {3, 4}});
            List<Integer> c = DirectedCycle.findCycle(g).orElseThrow();
            check(new HashSet<>(c).equals(new HashSet<>(List.of(1, 2, 3))), c.toString());
            verifyCycle(g, c);
        });

        run("safeVertices", () -> {
            // 0->1, 1->2, 2->1 (cycle), 0->3, 3->4: 3 and 4 are safe, 0 can reach the cycle.
            var g = DirectedCycle.graph(5, new int[][] {{0, 1}, {1, 2}, {2, 1}, {0, 3}, {3, 4}});
            check(DirectedCycle.safeVertices(g).equals(List.of(3, 4)), DirectedCycle.safeVertices(g).toString());
        });

        run("deepPathNoStackOverflow", () -> {
            int n = 200_000;
            int[][] edges = new int[n][];
            for (int i = 0; i < n - 1; i++) edges[i] = new int[] {i, i + 1};
            edges[n - 1] = new int[] {n - 1, 0};
            var g = DirectedCycle.graph(n, edges);
            check(DirectedCycle.findCycle(g).orElseThrow().size() == n, "one giant cycle");
        });

        run("randomizedVsBruteForce", () -> {
            Random rnd = new Random(102);
            for (int t = 0; t < 500; t++) {
                int n = 1 + rnd.nextInt(9);
                int m = rnd.nextInt(2 * n);
                int[][] edges = new int[m][];
                for (int i = 0; i < m; i++) edges[i] = new int[] {rnd.nextInt(n), rnd.nextInt(n)};
                var g = DirectedCycle.graph(n, edges);
                boolean expected = false;
                for (boolean b : onCycle(g)) expected |= b;
                var cycle = DirectedCycle.findCycle(g);
                check(cycle.isPresent() == expected, "dfs answer");
                cycle.ifPresent(c -> verifyCycle(g, c));
                var topo = DirectedCycle.topologicalOrder(g);
                check(topo.isPresent() == !expected, "kahn answer");
                topo.ifPresent(o -> verifyTopo(g, o));
                check(DirectedCycle.safeVertices(g).equals(bruteSafe(g)), "safe vertices");
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
