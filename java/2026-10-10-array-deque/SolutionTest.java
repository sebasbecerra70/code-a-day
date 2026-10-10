import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
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

    public static void main(String[] args) {
        run("emptyDeque", () -> {
            ArrayDeque<Integer> d = new ArrayDeque<>();
            check(d.isEmpty() && d.peekFirst() == null && d.peekLast() == null, "empty");
            expectThrows(NoSuchElementException.class, d::removeFirst);
            expectThrows(NoSuchElementException.class, d::removeLast);
            expectThrows(IndexOutOfBoundsException.class, () -> d.get(0));
        });

        run("queueBehavior", () -> {
            ArrayDeque<Integer> d = new ArrayDeque<>();
            for (int i = 0; i < 5; i++) d.addLast(i);
            for (int i = 0; i < 5; i++) check(d.removeFirst() == i, "FIFO order");
        });

        run("stackBehavior", () -> {
            ArrayDeque<String> d = new ArrayDeque<>();
            d.addFirst("a");
            d.addFirst("b");
            d.addFirst("c");
            check(d.removeFirst().equals("c") && d.removeFirst().equals("b"), "LIFO via front");
            check(d.peekLast().equals("a"), "peekLast");
        });

        run("wrapAroundAndGet", () -> {
            ArrayDeque<Integer> d = new ArrayDeque<>(4);
            d.addLast(1);
            d.addLast(2);
            d.addFirst(0);   // head wraps to the end of the array
            d.addFirst(-1);
            check(d.capacity() == 4 && d.size() == 4, "full without growing");
            for (int i = 0; i < 4; i++) check(d.get(i) == i - 1, "get(" + i + ")");
        });

        run("growsWhenFull", () -> {
            ArrayDeque<Integer> d = new ArrayDeque<>(2);
            for (int i = 0; i < 100; i++) {
                if (i % 2 == 0) d.addLast(i); else d.addFirst(i);
            }
            check(d.size() == 100 && d.capacity() >= 100, "grew");
            check(Integer.bitCount(d.capacity()) == 1, "capacity stays a power of two");
            check(d.peekFirst() == 99 && d.peekLast() == 98, "ends after growth");
        });

        run("shrinksWhenSparse", () -> {
            ArrayDeque<Integer> d = new ArrayDeque<>();
            for (int i = 0; i < 1000; i++) d.addLast(i);
            int big = d.capacity();
            for (int i = 0; i < 990; i++) d.removeFirst();
            check(d.capacity() < big && d.capacity() >= d.size(), "shrank");
            for (int i = 0; i < 10; i++) check(d.get(i) == 990 + i, "contents kept after shrink");
        });

        run("iteratorFailFast", () -> {
            ArrayDeque<Integer> d = new ArrayDeque<>();
            d.addLast(1);
            d.addLast(2);
            int sum = 0;
            for (int x : d) sum += x;
            check(sum == 3, "iteration");
            Iterator<Integer> it = d.iterator();
            it.next();
            d.addLast(3);
            expectThrows(ConcurrentModificationException.class, it::next);
        });

        run("invalidCapacity", () -> expectThrows(IllegalArgumentException.class, () -> new ArrayDeque<Integer>(0)));

        run("randomAgainstJdk", () -> {
            Random rng = new Random(6);
            ArrayDeque<Integer> d = new ArrayDeque<>(1);
            java.util.ArrayDeque<Integer> ref = new java.util.ArrayDeque<>();
            for (int step = 0; step < 20000; step++) {
                int op = rng.nextInt(4), v = rng.nextInt();
                if (op == 0) { d.addFirst(v); ref.addFirst(v); }
                else if (op == 1) { d.addLast(v); ref.addLast(v); }
                else if (op == 2 && !ref.isEmpty()) check(d.removeFirst().equals(ref.removeFirst()), "removeFirst");
                else if (op == 3 && !ref.isEmpty()) check(d.removeLast().equals(ref.removeLast()), "removeLast");
                check(d.size() == ref.size(), "size");
            }
            check(new ArrayList<>(ref).equals(toList(d)), "final contents");
        });

        System.out.println("All " + passed + " tests passed");
    }

    private static <T> ArrayList<T> toList(ArrayDeque<T> d) {
        ArrayList<T> out = new ArrayList<>();
        for (T x : d) out.add(x);
        return out;
    }
}
