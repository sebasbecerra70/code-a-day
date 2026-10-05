import java.util.Arrays;
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

    static char[][] grid(String... rows) {
        char[][] g = new char[rows.length][];
        for (int i = 0; i < rows.length; i++) g[i] = rows[i].toCharArray();
        return g;
    }

    // Independent reference: recursive DFS that sinks land in a copy.
    static int dfsCount(char[][] g) {
        char[][] copy = new char[g.length][];
        for (int i = 0; i < g.length; i++) copy[i] = g[i].clone();
        int count = 0;
        for (int r = 0; r < copy.length; r++) {
            for (int c = 0; c < copy[r].length; c++) {
                if (copy[r][c] == '1') {
                    count++;
                    sink(copy, r, c);
                }
            }
        }
        return count;
    }

    static void sink(char[][] g, int r, int c) {
        if (r < 0 || r >= g.length || c < 0 || c >= g[r].length || g[r][c] != '1') return;
        g[r][c] = '0';
        sink(g, r + 1, c);
        sink(g, r - 1, c);
        sink(g, r, c + 1);
        sink(g, r, c - 1);
    }

    public static void main(String[] args) {
        run("emptyAndAllWater", () -> {
            check(Islands.count(new char[0][]) == 0, "no rows");
            check(Islands.count(grid("000", "000")) == 0 && Islands.maxArea(grid("0")) == 0, "water");
        });

        run("classicExamples", () -> {
            check(Islands.count(grid("11110", "11010", "11000", "00000")) == 1, "one");
            check(Islands.count(grid("11000", "11000", "00100", "00011")) == 3, "three");
        });

        run("diagonalsDoNotConnect", () -> {
            check(Islands.count(grid("101", "010", "101")) == 5, "checkerboard");
        });

        run("sizesAndMaxArea", () -> {
            char[][] g = grid("1100", "1001", "0011", "1000");
            check(Islands.islandSizes(g).equals(List.of(3, 3, 1)), Islands.islandSizes(g).toString());
            check(Islands.maxArea(g) == 3, "max");
        });

        run("inputNotMutated", () -> {
            char[][] g = grid("11", "01");
            Islands.count(g);
            check(new String(g[0]).equals("11") && new String(g[1]).equals("01"), "unchanged");
        });

        run("hugeAllLandNoStackOverflow", () -> {
            char[][] g = new char[1000][1000];
            for (char[] row : g) Arrays.fill(row, '1');
            check(Islands.count(g) == 1 && Islands.maxArea(g) == 1_000_000, "one big island");
        });

        run("dynamicIslands", () -> {
            DynamicIslands d = new DynamicIslands(3, 3);
            check(d.addLand(0, 0) == 1, "first");
            check(d.addLand(0, 1) == 1, "joins");
            check(d.addLand(1, 2) == 2, "separate");
            check(d.addLand(2, 1) == 3, "another");
            check(d.addLand(1, 1) == 1, "bridges all three");
            check(d.addLand(1, 1) == 1, "repeat is no-op");
            expectThrows(IndexOutOfBoundsException.class, () -> d.addLand(3, 0));
        });

        run("randomizedCrossCheck", () -> {
            Random rnd = new Random(106);
            for (int t = 0; t < 200; t++) {
                int rows = 1 + rnd.nextInt(12), cols = 1 + rnd.nextInt(12);
                char[][] g = new char[rows][cols];
                for (char[] row : g) Arrays.fill(row, '0');
                DynamicIslands d = new DynamicIslands(rows, cols);
                for (int step = 0; step < rows * cols; step++) {
                    int r = rnd.nextInt(rows), c = rnd.nextInt(cols);
                    g[r][c] = '1';
                    int got = d.addLand(r, c);
                    int expected = dfsCount(g);
                    check(got == expected, "dynamic step");
                    if (step % 7 == 0) check(Islands.count(g) == expected, "bfs count");
                }
                int area = Islands.islandSizes(g).stream().mapToInt(Integer::intValue).sum();
                int land = 0;
                for (char[] row : g) for (char ch : row) if (ch == '1') land++;
                check(area == land, "sizes sum to land cells");
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
