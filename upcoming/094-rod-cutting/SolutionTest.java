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

    static final int[] CLRS = {1, 5, 8, 9, 10, 17, 17, 20, 24, 30};

    static long revenueOf(int[] price, List<Integer> pieces, int cutCost) {
        long r = 0;
        for (int p : pieces) r += price[p - 1];
        return r - (long) cutCost * Math.max(0, pieces.size() - 1);
    }

    public static void main(String[] args) {
        run("zeroLength", () -> {
            RodPlan p = RodCutting.solve(CLRS, 0);
            check(p.revenue() == 0 && p.pieces().isEmpty(), "nothing to sell");
        });

        run("invalidInput", () -> {
            expectThrows(IllegalArgumentException.class, () -> RodCutting.solve(CLRS, -1));
            expectThrows(IllegalArgumentException.class, () -> RodCutting.solve(new int[0], 3));
        });

        run("clrsTable", () -> {
            long[] expected = {0, 1, 5, 8, 10, 13, 17, 18, 22, 25, 30};
            for (int n = 0; n <= 10; n++) {
                RodPlan p = RodCutting.solve(CLRS, n);
                check(p.revenue() == expected[n], "n=" + n + " got " + p.revenue());
                check(p.pieces().stream().mapToInt(Integer::intValue).sum() == n, "pieces sum to n");
                check(revenueOf(CLRS, p.pieces(), 0) == p.revenue(), "plan matches revenue");
            }
        });

        run("knownCuts", () -> {
            check(RodCutting.solve(CLRS, 4).pieces().equals(List.of(2, 2)), "4 = 2 + 2");
            check(RodCutting.solve(CLRS, 10).pieces().equals(List.of(10)), "sell 10 whole");
        });

        run("rodLongerThanPriceList", () -> {
            int[] price = {2, 5}; // only lengths 1 and 2 are sellable
            RodPlan p = RodCutting.solve(price, 7);
            check(p.revenue() == 17, "3*5 + 2");
            check(p.pieces().stream().allMatch(x -> x <= 2), "no unsellable pieces");
        });

        run("cutCostDiscouragesCutting", () -> {
            RodPlan free = RodCutting.solve(CLRS, 4, 0);
            RodPlan costly = RodCutting.solve(CLRS, 4, 2);
            check(free.revenue() == 10 && free.pieces().size() == 2, "cut when free");
            check(costly.revenue() == 9 && costly.pieces().equals(List.of(4)), "sell whole when cuts cost 2");
        });

        run("memoMatchesBottomUp", () -> {
            for (int n = 0; n <= 40; n++) {
                check(RodCutting.solveMemo(CLRS, n) == RodCutting.solve(CLRS, n).revenue(), "n=" + n);
            }
        });

        run("randomizedVsBruteForce", () -> {
            Random rnd = new Random(94);
            for (int t = 0; t < 300; t++) {
                int[] price = new int[1 + rnd.nextInt(8)];
                for (int i = 0; i < price.length; i++) price[i] = rnd.nextInt(30);
                int n = rnd.nextInt(14), cut = rnd.nextInt(4);
                RodPlan p = RodCutting.solve(price, n, cut);
                check(p.revenue() == RodCutting.bruteForce(price, n, cut), "optimal");
                check(revenueOf(price, p.pieces(), cut) == p.revenue(), "plan consistent");
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
