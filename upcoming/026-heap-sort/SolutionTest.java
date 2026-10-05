import java.util.Arrays;
import java.util.Comparator;
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

    static void checkSorts(int[] input) {
        int[] expected = input.clone();
        Arrays.sort(expected);
        int[] a = input.clone();
        HeapSort.sort(a);
        check(Arrays.equals(a, expected), "sort " + Arrays.toString(input));
    }

    public static void main(String[] args) {
        run("emptyAndSingle", () -> {
            checkSorts(new int[0]);
            checkSorts(new int[] {3});
        });

        run("smallKnown", () -> {
            int[] a = {4, 10, 3, 5, 1};
            HeapSort.sort(a);
            check(Arrays.equals(a, new int[] {1, 3, 4, 5, 10}), Arrays.toString(a));
        });

        run("sortedReversedDuplicates", () -> {
            checkSorts(new int[] {1, 2, 3, 4, 5, 6, 7});
            checkSorts(new int[] {7, 6, 5, 4, 3, 2, 1});
            checkSorts(new int[] {2, 2, 2, 1, 1, 3, 3});
        });

        run("extremeValues", () -> {
            checkSorts(new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -5, Integer.MAX_VALUE});
        });

        run("heapifyBuildsValidHeap", () -> {
            Random rnd = new Random(1);
            for (int t = 0; t < 50; t++) {
                int[] a = rnd.ints(rnd.nextInt(100), -50, 50).toArray();
                for (int i = a.length / 2 - 1; i >= 0; i--) HeapSort.siftDown(a, i, a.length);
                check(HeapSort.isMaxHeap(a, a.length), "heap property");
            }
        });

        run("genericWithComparator", () -> {
            String[] s = {"delta", "alpha", "charlie", "bravo"};
            HeapSort.sort(s, Comparator.naturalOrder());
            check(Arrays.equals(s, new String[] {"alpha", "bravo", "charlie", "delta"}), Arrays.toString(s));
            Integer[] d = {3, 1, 2};
            HeapSort.sort(d, Comparator.reverseOrder());
            check(Arrays.equals(d, new Integer[] {3, 2, 1}), "descending");
        });

        run("topK", () -> {
            int[] input = {5, 1, 9, 3, 7, 9};
            check(Arrays.equals(HeapSort.topK(input, 3), new int[] {9, 9, 7}), "top 3");
            check(Arrays.equals(HeapSort.topK(input, 10), new int[] {9, 9, 7, 5, 3, 1}), "k > n");
            check(HeapSort.topK(input, 0).length == 0, "k = 0");
            check(Arrays.equals(input, new int[] {5, 1, 9, 3, 7, 9}), "input untouched");
        });

        run("randomizedCrossCheck", () -> {
            Random rnd = new Random(99);
            for (int t = 0; t < 300; t++) {
                int n = rnd.nextInt(300);
                checkSorts(rnd.ints(n, -1000, 1000).toArray());
                int[] a = rnd.ints(n, 0, 20).toArray();
                int k = rnd.nextInt(n + 1);
                int[] sorted = a.clone();
                Arrays.sort(sorted);
                int[] expected = new int[k];
                for (int i = 0; i < k; i++) expected[i] = sorted[n - 1 - i];
                check(Arrays.equals(HeapSort.topK(a, k), expected), "topK");
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
