import java.util.Arrays;
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

    record Student(String name, int grade) {}

    public static void main(String[] args) {
        run("emptyAndSingle", () -> {
            check(LinearSorts.countingSort(new int[0]).length == 0, "counting empty");
            check(Arrays.equals(LinearSorts.countingSort(new int[] {5}), new int[] {5}), "counting single");
            int[] e = {};
            LinearSorts.radixSort(e);
            int[] one = {-3};
            LinearSorts.radixSort(one);
            check(one[0] == -3, "radix single");
        });

        run("countingSortKnown", () -> {
            int[] out = LinearSorts.countingSort(new int[] {4, -2, 2, 8, 3, 3, 1, -2});
            check(Arrays.equals(out, new int[] {-2, -2, 1, 2, 3, 3, 4, 8}), Arrays.toString(out));
        });

        run("countingSortRejectsHugeRange", () -> {
            expectThrows(IllegalArgumentException.class,
                    () -> LinearSorts.countingSort(new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE}));
        });

        run("countingSortByIsStable", () -> {
            Student[] s = {
                new Student("ana", 2), new Student("bo", 0), new Student("cy", 2),
                new Student("di", 1), new Student("ed", 0)
            };
            Student[] out = LinearSorts.countingSortBy(s, 3, Student::grade);
            String names = String.join(",", Arrays.stream(out).map(Student::name).toArray(String[]::new));
            check(names.equals("bo,ed,di,ana,cy"), names);
            expectThrows(IllegalArgumentException.class,
                    () -> LinearSorts.countingSortBy(new Student[] {new Student("x", 5)}, 3, Student::grade));
        });

        run("radixSortNegativesAndExtremes", () -> {
            int[] a = {Integer.MAX_VALUE, -1, 0, Integer.MIN_VALUE, 256, -256, 1, 255};
            LinearSorts.radixSort(a);
            check(Arrays.equals(a, new int[] {Integer.MIN_VALUE, -256, -1, 0, 1, 255, 256, Integer.MAX_VALUE}),
                    Arrays.toString(a));
        });

        run("radixSortStrings", () -> {
            String[] plates = {"4PGC938", "2IYE230", "3CIO720", "1ICK750", "1OHV845", "4JZY524", "1ICK750"};
            String[] expected = plates.clone();
            Arrays.sort(expected);
            LinearSorts.radixSortStrings(plates, 7);
            check(Arrays.equals(plates, expected), Arrays.toString(plates));
        });

        run("randomizedCrossCheck", () -> {
            Random rnd = new Random(31337);
            for (int t = 0; t < 200; t++) {
                int n = rnd.nextInt(500);
                int[] wide = rnd.ints(n).toArray();
                int[] expected = wide.clone();
                Arrays.sort(expected);
                LinearSorts.radixSort(wide);
                check(Arrays.equals(wide, expected), "radix");

                int[] narrow = rnd.ints(n, -50, 50).toArray();
                int[] exp2 = narrow.clone();
                Arrays.sort(exp2);
                check(Arrays.equals(LinearSorts.countingSort(narrow), exp2), "counting");
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
