import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
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

    static TreeNode build(int... vals) {
        TreeNode root = null;
        for (int v : vals) root = TreeNode.insert(root, v);
        return root;
    }

    static List<Integer> drain(Iterator<Integer> it) {
        List<Integer> out = new ArrayList<>();
        while (it.hasNext()) out.add(it.next());
        return out;
    }

    // Recursive reference traversals.
    static void pre(TreeNode n, List<Integer> out) {
        if (n == null) return;
        out.add(n.val);
        pre(n.left, out);
        pre(n.right, out);
    }

    static void post(TreeNode n, List<Integer> out) {
        if (n == null) return;
        post(n.left, out);
        post(n.right, out);
        out.add(n.val);
    }

    public static void main(String[] args) {
        run("emptyTree", () -> {
            check(!new InorderIterator(null).hasNext(), "inorder");
            check(!new PreorderIterator(null).hasNext(), "preorder");
            check(!new PostorderIterator(null).hasNext(), "postorder");
            expectThrows(NoSuchElementException.class, () -> new InorderIterator(null).next());
            check(!BstAlgorithms.hasPairWithSum(null, 0), "two-sum on empty");
        });

        run("singleNode", () -> {
            TreeNode r = build(7);
            check(drain(new InorderIterator(r)).equals(List.of(7)), "in");
            check(drain(new InorderIterator(r, true)).equals(List.of(7)), "rev");
            check(drain(new PreorderIterator(r)).equals(List.of(7)), "pre");
            check(drain(new PostorderIterator(r)).equals(List.of(7)), "post");
        });

        run("knownTree", () -> {
            //        5
            //      3   8
            //     1 4 7 9
            TreeNode r = build(5, 3, 8, 1, 4, 7, 9);
            check(drain(new InorderIterator(r)).equals(List.of(1, 3, 4, 5, 7, 8, 9)), "in");
            check(drain(new InorderIterator(r, true)).equals(List.of(9, 8, 7, 5, 4, 3, 1)), "rev");
            check(drain(new PreorderIterator(r)).equals(List.of(5, 3, 1, 4, 8, 7, 9)), "pre");
            check(drain(new PostorderIterator(r)).equals(List.of(1, 4, 3, 7, 9, 8, 5)), "post");
        });

        run("skewedTrees", () -> {
            TreeNode right = build(1, 2, 3, 4, 5);
            TreeNode left = build(5, 4, 3, 2, 1);
            check(drain(new InorderIterator(right)).equals(List.of(1, 2, 3, 4, 5)), "right in");
            check(drain(new PostorderIterator(right)).equals(List.of(5, 4, 3, 2, 1)), "right post");
            check(drain(new PostorderIterator(left)).equals(List.of(1, 2, 3, 4, 5)), "left post");
            check(drain(new PreorderIterator(left)).equals(List.of(5, 4, 3, 2, 1)), "left pre");
        });

        run("peekDoesNotAdvance", () -> {
            InorderIterator it = new InorderIterator(build(2, 1, 3));
            check(it.peek() == 1 && it.peek() == 1, "peek");
            check(it.next() == 1 && it.peek() == 2, "advance");
        });

        run("exhaustedThrows", () -> {
            Iterator<Integer> it = new PostorderIterator(build(1));
            it.next();
            expectThrows(NoSuchElementException.class, it::next);
        });

        run("twoSum", () -> {
            TreeNode r = build(5, 3, 8, 1, 4, 7, 9);
            check(BstAlgorithms.hasPairWithSum(r, 17), "8+9");
            check(BstAlgorithms.hasPairWithSum(r, 4), "1+3");
            check(!BstAlgorithms.hasPairWithSum(r, 2), "no 1+1 reuse");
            check(!BstAlgorithms.hasPairWithSum(r, 100), "too big");
        });

        run("kthSmallest", () -> {
            TreeNode r = build(5, 3, 8, 1, 4, 7, 9);
            check(BstAlgorithms.kthSmallest(r, 1) == 1, "k=1");
            check(BstAlgorithms.kthSmallest(r, 4) == 5, "k=4");
            check(BstAlgorithms.kthSmallest(r, 7) == 9, "k=7");
        });

        run("randomizedCrossCheck", () -> {
            Random rnd = new Random(42);
            for (int trial = 0; trial < 200; trial++) {
                TreeNode r = null;
                TreeSet<Integer> ref = new TreeSet<>();
                int n = rnd.nextInt(40);
                for (int i = 0; i < n; i++) {
                    int v = rnd.nextInt(100);
                    r = TreeNode.insert(r, v);
                    ref.add(v);
                }
                List<Integer> sorted = new ArrayList<>(ref);
                List<Integer> desc = new ArrayList<>(sorted);
                Collections.reverse(desc);
                List<Integer> p = new ArrayList<>(), q = new ArrayList<>();
                pre(r, p);
                post(r, q);
                check(drain(new InorderIterator(r)).equals(sorted), "in");
                check(drain(new InorderIterator(r, true)).equals(desc), "rev");
                check(drain(new PreorderIterator(r)).equals(p), "pre");
                check(drain(new PostorderIterator(r)).equals(q), "post");
                int target = rnd.nextInt(200);
                boolean expected = false;
                for (int x : sorted) if (x * 2 != target && ref.contains(target - x)) expected = true;
                check(BstAlgorithms.hasPairWithSum(r, target) == expected, "two-sum " + target);
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
