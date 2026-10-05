import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

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

    // Brute-force helpers: count components, optionally ignoring a vertex or an edge.
    static int components(int n, int[][] edges, int skipVertex, int skipEdge) {
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        int count = skipVertex >= 0 ? n - 1 : n;
        for (int i = 0; i < edges.length; i++) {
            int a = edges[i][0], b = edges[i][1];
            if (i == skipEdge || a == skipVertex || b == skipVertex) continue;
            int ra = find(parent, a), rb = find(parent, b);
            if (ra != rb) {
                parent[ra] = rb;
                count--;
            }
        }
        return count;
    }

    static int find(int[] p, int x) {
        while (p[x] != x) x = p[x] = p[p[x]];
        return x;
    }

    public static void main(String[] args) {
        run("emptyAndSingleVertex", () -> {
            CutFinder f = new CutFinder(1, new int[0][]);
            check(f.bridges.isEmpty() && f.articulationPoints.isEmpty(), "nothing");
        });

        run("pathGraph", () -> {
            CutFinder f = new CutFinder(4, new int[][] {{0, 1}, {1, 2}, {2, 3}});
            check(f.bridges.equals(List.of(0, 1, 2)), "every edge is a bridge");
            check(f.articulationPoints.equals(new TreeSet<>(List.of(1, 2))), "interior vertices");
        });

        run("cycleHasNone", () -> {
            CutFinder f = new CutFinder(4, new int[][] {{0, 1}, {1, 2}, {2, 3}, {3, 0}});
            check(f.bridges.isEmpty() && f.articulationPoints.isEmpty(), "2-connected");
        });

        run("twoTrianglesJoinedByBridge", () -> {
            int[][] e = {{0, 1}, {1, 2}, {2, 0}, {2, 3}, {3, 4}, {4, 5}, {5, 3}};
            CutFinder f = new CutFinder(6, e);
            check(f.bridges.equals(List.of(3)), "edge 2-3");
            check(f.articulationPoints.equals(new TreeSet<>(List.of(2, 3))), f.articulationPoints.toString());
        });

        run("parallelEdgesAreNotBridges", () -> {
            CutFinder f = new CutFinder(3, new int[][] {{0, 1}, {0, 1}, {1, 2}});
            check(f.bridges.equals(List.of(2)), "double edge 0-1 survives one removal");
            check(f.articulationPoints.equals(new TreeSet<>(List.of(1))), "1 is still a cut vertex");
        });

        run("starRootIsArticulation", () -> {
            CutFinder f = new CutFinder(4, new int[][] {{0, 1}, {0, 2}, {0, 3}});
            check(f.articulationPoints.equals(new TreeSet<>(List.of(0))), "center");
            check(f.bridges.size() == 3, "all spokes");
        });

        run("selfLoopAndDisconnected", () -> {
            CutFinder f = new CutFinder(5, new int[][] {{0, 0}, {0, 1}, {3, 4}});
            check(f.bridges.equals(List.of(1, 2)), "self-loop ignored, both real edges are bridges");
            check(f.articulationPoints.isEmpty(), "no cut vertices");
        });

        run("deepGraphNoStackOverflow", () -> {
            int n = 200_000;
            int[][] e = new int[n - 1][];
            for (int i = 0; i < n - 1; i++) e[i] = new int[] {i, i + 1};
            CutFinder f = new CutFinder(n, e);
            check(f.bridges.size() == n - 1 && f.articulationPoints.size() == n - 2, "long path");
        });

        run("randomizedVsBruteForce", () -> {
            Random rnd = new Random(110);
            for (int t = 0; t < 500; t++) {
                int n = 1 + rnd.nextInt(9);
                int m = rnd.nextInt(2 * n + 1);
                int[][] e = new int[m][];
                for (int i = 0; i < m; i++) e[i] = new int[] {rnd.nextInt(n), rnd.nextInt(n)};
                CutFinder f = new CutFinder(n, e);
                int base = components(n, e, -1, -1);
                List<Integer> expectedBridges = new ArrayList<>();
                for (int i = 0; i < m; i++) if (components(n, e, -1, i) > base) expectedBridges.add(i);
                TreeSet<Integer> expectedAps = new TreeSet<>();
                for (int v = 0; v < n; v++) {
                    // Removing v also removes v itself from the count, hence the comparison with base.
                    boolean isolated = true;
                    for (int[] ed : e) if ((ed[0] == v) != (ed[1] == v)) isolated = false;
                    if (!isolated && components(n, e, v, -1) > base) expectedAps.add(v);
                }
                check(f.bridges.equals(expectedBridges), "bridges " + f.bridges + " vs " + expectedBridges);
                check(f.articulationPoints.equals(expectedAps), "aps " + f.articulationPoints + " vs " + expectedAps);
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
