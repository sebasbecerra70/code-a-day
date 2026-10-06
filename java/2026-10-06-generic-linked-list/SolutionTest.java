import java.util.ArrayList;
import java.util.ConcurrentModificationException;
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

    public static void main(String[] args) {
        run("emptyList", () -> {
            DoublyLinkedList<Integer> l = new DoublyLinkedList<>();
            check(l.isEmpty() && l.size() == 0, "new list is empty");
            check(l.toString().equals("[]"), "empty toString");
            expectThrows(NoSuchElementException.class, l::removeFirst);
            expectThrows(NoSuchElementException.class, l::removeLast);
            expectThrows(IndexOutOfBoundsException.class, () -> l.get(0));
        });

        run("addAndGet", () -> {
            DoublyLinkedList<String> l = new DoublyLinkedList<>();
            l.addLast("b");
            l.addFirst("a");
            l.addLast("d");
            l.add(2, "c");
            check(l.toString().equals("[a, b, c, d]"), "order after adds: " + l);
            check(l.get(0).equals("a") && l.get(3).equals("d"), "get by index");
            l.add(4, "e");
            l.add(0, "start");
            check(l.toString().equals("[start, a, b, c, d, e]"), "add at both ends: " + l);
        });

        run("removeOperations", () -> {
            DoublyLinkedList<Integer> l = DoublyLinkedList.of(1, 2, 3, 4, 5);
            check(l.removeFirst() == 1 && l.removeLast() == 5, "remove ends");
            check(l.remove(1) == 3, "remove middle");
            check(l.toString().equals("[2, 4]") && l.size() == 2, "after removes: " + l);
            expectThrows(IndexOutOfBoundsException.class, () -> l.remove(2));
        });

        run("setAndIndexOfWithNulls", () -> {
            DoublyLinkedList<String> l = DoublyLinkedList.of("x", null, "y");
            check(l.indexOf(null) == 1 && l.indexOf("y") == 2 && l.indexOf("z") == -1, "indexOf");
            check(l.set(1, "mid") == null && l.get(1).equals("mid"), "set returns old value");
        });

        run("reverse", () -> {
            DoublyLinkedList<Integer> l = DoublyLinkedList.of(1, 2, 3, 4);
            l.reverse();
            check(l.toString().equals("[4, 3, 2, 1]"), "reversed: " + l);
            l.addLast(0);
            l.addFirst(5);
            check(l.toString().equals("[5, 4, 3, 2, 1, 0]"), "adds still work after reverse: " + l);
            check(l.removeLast() == 0 && l.get(0) == 5, "ends correct after reverse");
            DoublyLinkedList<Integer> empty = new DoublyLinkedList<>();
            empty.reverse();
            check(empty.isEmpty(), "reverse empty");
        });

        run("iteratorAndRemove", () -> {
            DoublyLinkedList<Integer> l = DoublyLinkedList.of(1, 2, 3, 4, 5, 6);
            Iterator<Integer> it = l.iterator();
            while (it.hasNext()) if (it.next() % 2 == 0) it.remove();
            check(l.toString().equals("[1, 3, 5]"), "iterator remove: " + l);
            Iterator<Integer> it2 = l.iterator();
            expectThrows(IllegalStateException.class, it2::remove);
            int sum = 0;
            for (int x : l) sum += x;
            check(sum == 9, "for-each sum");
        });

        run("failFastIterator", () -> {
            DoublyLinkedList<Integer> l = DoublyLinkedList.of(1, 2, 3);
            Iterator<Integer> it = l.iterator();
            it.next();
            l.addLast(4);
            expectThrows(ConcurrentModificationException.class, it::next);
            Iterator<Integer> done = DoublyLinkedList.of(1).iterator();
            done.next();
            expectThrows(NoSuchElementException.class, done::next);
        });

        run("removeIf", () -> {
            DoublyLinkedList<Integer> l = DoublyLinkedList.of(5, 1, 5, 2, 5);
            check(l.removeIf(x -> x == 5) == 3, "removed count");
            check(l.toString().equals("[1, 2]"), "after removeIf: " + l);
        });

        run("randomAgainstArrayList", () -> {
            Random rng = new Random(2);
            DoublyLinkedList<Integer> l = new DoublyLinkedList<>();
            List<Integer> ref = new ArrayList<>();
            for (int step = 0; step < 5000; step++) {
                int op = rng.nextInt(6);
                int v = rng.nextInt(100);
                if (op == 0) { l.addFirst(v); ref.add(0, v); }
                else if (op == 1) { l.addLast(v); ref.add(v); }
                else if (op == 2) { int i = rng.nextInt(ref.size() + 1); l.add(i, v); ref.add(i, v); }
                else if (op == 3 && !ref.isEmpty()) { int i = rng.nextInt(ref.size()); check(l.remove(i).equals(ref.remove(i)), "remove(i)"); }
                else if (op == 4 && !ref.isEmpty()) { int i = rng.nextInt(ref.size()); check(l.get(i).equals(ref.get(i)), "get(i)"); }
                else if (op == 5 && rng.nextInt(50) == 0) { l.reverse(); java.util.Collections.reverse(ref); }
                check(l.size() == ref.size(), "size");
            }
            check(l.toString().equals(ref.toString()), "final contents");
        });

        System.out.println("All " + passed + " tests passed");
    }
}
