import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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

    static boolean hasEdge(List<List<Integer>> adj, int a, int b) {
        return adj.get(a).contains(b);
    }

    /** Verifies whichever certificate the result carries. */
    static void verify(List<List<Integer>> adj, BipartiteResult r) {
        if (r.bipartite()) {
            for (int u = 0; u < adj.size(); u++) {
                for (int v : adj.get(u)) check(r.colors()[u] != r.colors()[v], "edge " + u + "-" + v + " same color");
            }
        } else {
            List<Integer> c = r.oddCycle();
            check(c.size() % 2 == 1, "cycle length must be odd: " + c);
            check(new HashSet<>(c).size() == c.size(), "simple cycle");
            for (int i = 0; i < c.size(); i++) {
                check(hasEdge(adj, c.get(i), c.get((i + 1) % c.size())), "cycle edge missing in " + c);
            }
        }
    }

    // Reference: try all 2^n colorings.
    static boolean bruteBipartite(int n, int[][] edges) {
        for (int mask = 0; mask < (1 << n); mask++) {
            boolean ok = true;
            for (int[] e : edges) if (((mask >> e[0]) & 1) == ((mask >> e[1]) & 1)) ok = false;
            if (ok) return true;
        }
        return false;
    }

    public static void main(String[] args) {
        run("emptyAndIsolated", () -> {
            check(Bipartite.check(Bipartite.graph(0, new int[0][])).bipartite(), "no vertices");
            check(Bipartite.check(Bipartite.graph(5, new int[0][])).bipartite(), "no edges");
        });

        run("evenCycle", () -> {
            var g = Bipartite.graph(4, new int[][] {{0, 1}, {1, 2}, {2, 3}, {3, 0}});
            BipartiteResult r = Bipartite.check(g);
            check(r.bipartite(), "C4");
            verify(g, r);
        });

        run("triangle", () -> {
            var g = Bipartite.graph(3, new int[][] {{0, 1}, {1, 2}, {2, 0}});
            BipartiteResult r = Bipartite.check(g);
            check(!r.bipartite() && r.oddCycle().size() == 3, "C3 " + r.oddCycle());
            verify(g, r);
        });

        run("selfLoopIsNotBipartite", () -> {
            var g = Bipartite.graph(2, new int[][] {{0, 1}, {1, 1}});
            BipartiteResult r = Bipartite.check(g);
            check(!r.bipartite() && r.oddCycle().equals(List.of(1)), String.valueOf(r.oddCycle()));
        });

        run("disconnectedWithOddComponent", () -> {
            var g = Bipartite.graph(8, new int[][] {{0, 1}, {1, 2}, {4, 5}, {5, 6}, {6, 7}, {7, 3}, {3, 4}});
            BipartiteResult r = Bipartite.check(g);
            check(!r.bipartite() && r.oddCycle().size() == 5, "C5 in the second component");
            verify(g, r);
        });

        run("treeAndCompleteBipartite", () -> {
            var tree = Bipartite.graph(7, new int[][] {{0, 1}, {0, 2}, {1, 3}, {1, 4}, {2, 5}, {2, 6}});
            verify(tree, Bipartite.check(tree));
            var k33 = Bipartite.graph(6, new int[][] {{0, 3}, {0, 4}, {0, 5}, {1, 3}, {1, 4}, {1, 5}, {2, 3}, {2, 4}, {2, 5}});
            BipartiteResult r = Bipartite.check(k33);
            check(r.bipartite(), "K3,3");
            Set<Integer> side = new HashSet<>();
            for (int i = 0; i < 6; i++) if (r.colors()[i] == r.colors()[0]) side.add(i);
            check(side.equals(Set.of(0, 1, 2)), "sides " + side);
        });

        run("longOddCycleCertificate", () -> {
            int n = 1001;
            int[][] edges = new int[n][];
            for (int i = 0; i < n; i++) edges[i] = new int[] {i, (i + 1) % n};
            var g = Bipartite.graph(n, edges);
            BipartiteResult r = Bipartite.check(g);
            check(!r.bipartite() && r.oddCycle().size() == n, "whole ring");
            verify(g, r);
        });

        run("randomizedVsBruteForce", () -> {
            Random rnd = new Random(98);
            for (int t = 0; t < 500; t++) {
                int n = 1 + rnd.nextInt(10);
                int m = rnd.nextInt(n * 2);
                int[][] edges = new int[m][];
                for (int i = 0; i < m; i++) {
                    int a = rnd.nextInt(n), b = rnd.nextInt(n);
                    if (a == b) b = (a + 1) % n;
                    edges[i] = new int[] {a, b};
                }
                if (n == 1) edges = new int[0][];
                var g = Bipartite.graph(n, edges);
                BipartiteResult r = Bipartite.check(g);
                check(r.bipartite() == bruteBipartite(n, edges), "answer");
                verify(g, r);
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
