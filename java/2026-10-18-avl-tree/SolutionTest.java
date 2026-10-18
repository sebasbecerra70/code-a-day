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

    public static void main(String[] args) {
        run("emptyTree", () -> {
            AvlTree<Integer> t = new AvlTree<>();
            check(t.size() == 0 && t.height() == 0 && !t.contains(1), "empty");
            check(!t.remove(1) && t.inOrder().isEmpty(), "remove on empty");
            expectThrows(IndexOutOfBoundsException.class, () -> t.select(0));
        });

        run("insertAndContains", () -> {
            AvlTree<String> t = new AvlTree<>();
            check(t.insert("m") && t.insert("c") && t.insert("x"), "inserts");
            check(!t.insert("c"), "duplicate rejected");
            check(t.contains("x") && !t.contains("a") && t.size() == 3, "contains");
        });

        run("sortedInsertStaysBalanced", () -> {
            AvlTree<Integer> t = new AvlTree<>();
            for (int i = 0; i < 1023; i++) t.insert(i);
            check(t.height() == 10, "perfectly balanced: height " + t.height());
            check(t.isValid(), "invariants");
        });

        run("allFourRotationCases", () -> {
            int[][] orders = {{3, 2, 1}, {1, 2, 3}, {3, 1, 2}, {1, 3, 2}}; // LL, RR, LR, RL
            for (int[] order : orders) {
                AvlTree<Integer> t = new AvlTree<>();
                for (int k : order) t.insert(k);
                check(t.height() == 2 && t.isValid() && t.inOrder().equals(List.of(1, 2, 3)), "rotation case");
            }
        });

        run("removeLeafOneChildTwoChildren", () -> {
            AvlTree<Integer> t = new AvlTree<>();
            for (int k : new int[] {50, 30, 70, 20, 40, 60, 80, 65}) t.insert(k);
            check(t.remove(20) && t.isValid(), "leaf");
            check(t.remove(60) && t.isValid(), "one child");
            check(t.remove(50) && t.isValid(), "two children (root)");
            check(t.inOrder().equals(List.of(30, 40, 65, 70, 80)), "contents " + t.inOrder());
        });

        run("heightBoundUnderRandomInserts", () -> {
            Random rng = new Random(14);
            AvlTree<Integer> t = new AvlTree<>();
            for (int i = 0; i < 100000; i++) t.insert(rng.nextInt());
            int n = t.size();
            double bound = 1.4405 * Math.log(n + 2) / Math.log(2);
            check(t.height() <= bound, "height " + t.height() + " exceeds AVL bound " + bound);
        });

        run("selectAndRank", () -> {
            AvlTree<Integer> t = new AvlTree<>();
            for (int k : new int[] {10, 20, 30, 40, 50}) t.insert(k);
            check(t.select(0) == 10 && t.select(4) == 50 && t.select(2) == 30, "select");
            check(t.rank(10) == 0 && t.rank(35) == 3 && t.rank(100) == 5 && t.rank(5) == 0, "rank");
        });

        run("randomAgainstTreeSet", () -> {
            Random rng = new Random(15);
            AvlTree<Integer> t = new AvlTree<>();
            TreeSet<Integer> ref = new TreeSet<>();
            for (int step = 0; step < 20000; step++) {
                int k = rng.nextInt(1000);
                if (rng.nextInt(3) == 0) check(t.remove(k) == ref.remove(k), "remove " + k);
                else check(t.insert(k) == ref.add(k), "insert " + k);
                if (step % 500 == 0) check(t.isValid(), "invariants at step " + step);
            }
            check(t.inOrder().equals(new ArrayList<>(ref)), "contents");
            for (int i = 0; i < ref.size(); i += 7) check(t.select(i).equals(new ArrayList<>(ref).get(i)), "select");
            check(t.rank(500) == ref.headSet(500).size(), "rank");
        });

        System.out.println("All " + passed + " tests passed");
    }
}
