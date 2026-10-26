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

    record Item(int key, int order) {}

    static void checkAllSorts(int[] input) {
        int[] expected = input.clone();
        Arrays.sort(expected);
        int[] q = input.clone();
        Sorts.quickSort(q);
        check(Arrays.equals(q, expected), "quickSort " + Arrays.toString(input));
        int[] b = input.clone();
        Sorts.mergeSortBottomUp(b);
        check(Arrays.equals(b, expected), "bottom-up " + Arrays.toString(input));
        Integer[] boxed = Arrays.stream(input).boxed().toArray(Integer[]::new);
        Sorts.mergeSort(boxed, Comparator.naturalOrder());
        check(Arrays.equals(Arrays.stream(boxed).mapToInt(Integer::intValue).toArray(), expected), "mergeSort");
    }

    public static void main(String[] args) {
        run("emptyAndSingle", () -> {
            checkAllSorts(new int[0]);
            checkAllSorts(new int[] {42});
        });

        run("smallKnown", () -> {
            checkAllSorts(new int[] {5, 2, 9, 1, 5, 6});
            checkAllSorts(new int[] {2, 1});
        });

        run("alreadySortedAndReversed", () -> {
            int[] asc = new int[1000], desc = new int[1000];
            for (int i = 0; i < 1000; i++) {
                asc[i] = i;
                desc[i] = 1000 - i;
            }
            checkAllSorts(asc);
            checkAllSorts(desc);
        });

        run("allDuplicates", () -> {
            int[] a = new int[100_000];
            Arrays.fill(a, 7);
            checkAllSorts(a); // 3-way partition keeps this linear-ish instead of quadratic
        });

        run("extremeValues", () -> {
            checkAllSorts(new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE, 0, -1, 1, Integer.MIN_VALUE});
        });

        run("mergeSortIsStable", () -> {
            Random rnd = new Random(7);
            Item[] items = new Item[500];
            for (int i = 0; i < items.length; i++) items[i] = new Item(rnd.nextInt(10), i);
            Sorts.mergeSort(items, Comparator.comparingInt(Item::key));
            for (int i = 1; i < items.length; i++) {
                Item p = items[i - 1], c = items[i];
                check(p.key() < c.key() || (p.key() == c.key() && p.order() < c.order()), "stable at " + i);
            }
        });

        run("customComparator", () -> {
            String[] words = {"pear", "fig", "banana", "kiwi", "apple"};
            Sorts.mergeSort(words, Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
            check(Arrays.equals(words, new String[] {"fig", "kiwi", "pear", "apple", "banana"}), Arrays.toString(words));
        });

        run("lomutoPartition", () -> {
            int[] a = {3, 8, 1, 9, 4, 5};
            int p = Sorts.lomutoPartition(a, 0, a.length - 1);
            check(a[p] == 5, "pivot placed");
            for (int i = 0; i < p; i++) check(a[i] < 5, "left side");
            for (int i = p + 1; i < a.length; i++) check(a[i] >= 5, "right side");
        });

        run("randomizedCrossCheck", () -> {
            Random rnd = new Random(2024);
            for (int trial = 0; trial < 300; trial++) {
                int n = rnd.nextInt(200);
                int range = 1 + rnd.nextInt(trial % 3 == 0 ? 5 : 1000);
                int[] a = new int[n];
                for (int i = 0; i < n; i++) a[i] = rnd.nextInt(range) - range / 2;
                checkAllSorts(a);
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
