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

    public static void main(String[] args) {
        run("invalidInput", () -> {
            expectThrows(IllegalArgumentException.class, () -> new MatrixChain(new int[] {5}));
            expectThrows(IllegalArgumentException.class, () -> new MatrixChain(new int[] {5, 0, 3}));
        });

        run("singleMatrix", () -> {
            MatrixChain mc = new MatrixChain(new int[] {10, 20});
            check(mc.minCost() == 0 && mc.parenthesization().equals("A1"), "no multiplications");
        });

        run("twoMatrices", () -> {
            MatrixChain mc = new MatrixChain(new int[] {10, 20, 30});
            check(mc.minCost() == 6000 && mc.parenthesization().equals("(A1A2)"), "10*20*30");
        });

        run("threeMatricesOrderMatters", () -> {
            // (A1A2)A3 = 10*30*5 + 10*5*60 = 4500; A1(A2A3) = 30*5*60 + 10*30*60 = 27000
            MatrixChain mc = new MatrixChain(new int[] {10, 30, 5, 60});
            check(mc.minCost() == 4500, String.valueOf(mc.minCost()));
            check(mc.parenthesization().equals("((A1A2)A3)"), mc.parenthesization());
        });

        run("clrsExample", () -> {
            int[] dims = {30, 35, 15, 5, 10, 20, 25};
            MatrixChain mc = new MatrixChain(dims);
            check(mc.minCost() == 15125, String.valueOf(mc.minCost()));
            check(mc.parenthesization().equals("((A1(A2A3))((A4A5)A6))"), mc.parenthesization());
        });

        run("parenthesizationCostMatches", () -> {
            int[] dims = {40, 20, 30, 10, 30};
            MatrixChain mc = new MatrixChain(dims);
            check(mc.minCost() == 26000, String.valueOf(mc.minCost()));
            check(MatrixChain.costOf(mc.parenthesization(), dims) == mc.minCost(), "replayed cost");
        });

        run("largeDimensionsNoOverflow", () -> {
            int[] dims = {100_000, 100_000, 100_000};
            check(new MatrixChain(dims).minCost() == 1_000_000_000_000_000L, "long arithmetic");
        });

        run("randomizedVsBruteForce", () -> {
            Random rnd = new Random(82);
            for (int t = 0; t < 300; t++) {
                int n = 1 + rnd.nextInt(8);
                int[] dims = new int[n + 1];
                for (int i = 0; i <= n; i++) dims[i] = 1 + rnd.nextInt(50);
                MatrixChain mc = new MatrixChain(dims);
                check(mc.minCost() == MatrixChain.bruteForce(dims, 0, n - 1), "optimal");
                check(MatrixChain.costOf(mc.parenthesization(), dims) == mc.minCost(), "reconstruction");
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
