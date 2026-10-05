import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
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

    static List<Integer> drain(Iterator<Integer> it) {
        List<Integer> out = new ArrayList<>();
        while (it.hasNext()) out.add(it.next());
        return out;
    }

    // Recursive reference flatten.
    static void flatten(Nested n, List<Integer> out) {
        switch (n) {
            case Nested.Int i -> out.add(i.value());
            case Nested.Many m -> m.items().forEach(x -> flatten(x, out));
        }
    }

    static Nested random(Random rnd, int depth) {
        if (depth == 0 || rnd.nextInt(3) == 0) return Nested.of(rnd.nextInt(100));
        List<Nested> items = new ArrayList<>();
        for (int i = rnd.nextInt(4); i > 0; i--) items.add(random(rnd, depth - 1));
        return new Nested.Many(items);
    }

    public static void main(String[] args) {
        run("emptyInputs", () -> {
            check(!new NestedIterator(List.of()).hasNext(), "empty");
            NestedIterator it = new NestedIterator(List.of(Nested.of(), Nested.of(Nested.of(), Nested.of(Nested.of()))));
            check(!it.hasNext(), "only empty lists");
            expectThrows(NoSuchElementException.class, it::next);
        });

        run("classicExamples", () -> {
            // [[1,1],2,[1,1]] and [1,[4,[6]]]
            var a = List.of(Nested.of(Nested.of(1), Nested.of(1)), Nested.of(2), Nested.of(Nested.of(1), Nested.of(1)));
            check(drain(new NestedIterator(a)).equals(List.of(1, 1, 2, 1, 1)), "first");
            var b = List.of(Nested.of(1), Nested.of(Nested.of(4), Nested.of(Nested.of(6))));
            check(drain(new NestedIterator(b)).equals(List.of(1, 4, 6)), "second");
        });

        run("hasNextIsIdempotent", () -> {
            NestedIterator it = new NestedIterator(List.of(Nested.of(), Nested.of(7)));
            check(it.hasNext() && it.hasNext() && it.hasNext(), "repeated");
            check(it.next() == 7 && !it.hasNext(), "single value");
        });

        run("nextWithoutHasNext", () -> {
            NestedIterator it = new NestedIterator(List.of(Nested.of(Nested.of(Nested.of(3))), Nested.of(4)));
            check(it.next() == 3 && it.next() == 4, "works without calling hasNext");
        });

        run("deepNesting", () -> {
            Nested n = Nested.of(42);
            for (int i = 0; i < 10_000; i++) n = Nested.of(n);
            check(drain(new NestedIterator(List.of(n))).equals(List.of(42)), "depth 10k");
        });

        run("flatten2D", () -> {
            List<List<Integer>> grid = List.of(List.of(1, 2), List.of(), List.of(3), List.of(), List.of());
            Iterator<Integer> it = new FlattenIterator<>(grid.stream().map(List::iterator).iterator());
            check(drain(it).equals(List.of(1, 2, 3)), "skips empty rows");
            check(!new FlattenIterator<Integer>(List.<Iterator<Integer>>of().iterator()).hasNext(), "empty outer");
        });

        run("flattenRemoveAfterHasNextMovedOn", () -> {
            List<List<Integer>> rows = List.of(new ArrayList<>(List.of(1, 2)), new ArrayList<>(List.of(3)));
            FlattenIterator<Integer> it = new FlattenIterator<>(rows.stream().map(List::iterator).iterator());
            expectThrows(IllegalStateException.class, it::remove);
            it.next();
            it.next(); // 2, the last of row 0
            check(it.hasNext(), "hasNext advances to row 1");
            it.remove(); // must still remove 2 from row 0
            check(rows.get(0).equals(List.of(1)) && rows.get(1).equals(List.of(3)), rows.toString());
            expectThrows(IllegalStateException.class, it::remove);
        });

        run("randomizedVsRecursive", () -> {
            Random rnd = new Random(114);
            for (int t = 0; t < 500; t++) {
                List<Nested> list = new ArrayList<>();
                for (int i = rnd.nextInt(5); i > 0; i--) list.add(random(rnd, 6));
                List<Integer> expected = new ArrayList<>();
                list.forEach(n -> flatten(n, expected));
                check(drain(new NestedIterator(list)).equals(expected), "matches recursive flatten");
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
