import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    /** A key whose hash is chosen by the test, to force collisions. */
    record BadKey(int id, int hash) {
        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof BadKey b && b.id == id;
        }
    }

    public static void main(String[] args) {
        run("putGetUpdate", () -> {
            OpenAddressingMap<String, Integer> m = new OpenAddressingMap<>();
            check(m.put("a", 1) == null && m.put("b", 2) == null, "fresh puts return null");
            check(m.put("a", 10) == 1, "update returns old value");
            check(m.get("a") == 10 && m.get("b") == 2 && m.get("zzz") == null, "gets");
            check(m.size() == 2, "size counts distinct keys");
        });

        run("removeBasics", () -> {
            OpenAddressingMap<String, Integer> m = new OpenAddressingMap<>();
            m.put("x", 1);
            check(m.remove("x") == 1 && m.remove("x") == null, "remove twice");
            check(m.isEmpty() && !m.containsKey("x"), "empty after remove");
        });

        run("nullHandling", () -> {
            OpenAddressingMap<String, String> m = new OpenAddressingMap<>();
            expectThrows(NullPointerException.class, () -> m.put(null, "v"));
            check(m.get(null) == null && m.remove(null) == null && !m.containsKey(null), "null lookups");
            m.put("k", null);
            check(m.containsKey("k") && m.get("k") == null, "null values allowed");
        });

        run("collisionsAndBackwardShift", () -> {
            OpenAddressingMap<BadKey, Integer> m = new OpenAddressingMap<>(64);
            // Five keys with the same hash form one probe run.
            for (int i = 0; i < 5; i++) m.put(new BadKey(i, 7), i);
            check(m.maxProbeLength() == 5, "one cluster of 5");
            m.remove(new BadKey(1, 7)); // hole in the middle of the run
            for (int i = 0; i < 5; i++) {
                Integer v = m.get(new BadKey(i, 7));
                check(i == 1 ? v == null : v == i, "lookup after delete, key " + i);
            }
            check(m.maxProbeLength() == 4, "run shrank, no tombstone");
        });

        run("wrapAroundRun", () -> {
            OpenAddressingMap<BadKey, Integer> m = new OpenAddressingMap<>(64);
            int cap = m.capacity();
            // Home slot is the last index, so the run wraps to slot 0, 1, ...
            for (int i = 0; i < 4; i++) m.put(new BadKey(i, cap - 1), i);
            m.put(new BadKey(99, 0), 99); // home 0, displaced by the wrapped run
            m.remove(new BadKey(0, cap - 1));
            for (int i = 1; i < 4; i++) check(m.get(new BadKey(i, cap - 1)) == i, "wrapped key " + i);
            check(m.get(new BadKey(99, 0)) == 99, "displaced key still found");
        });

        run("resizeKeepsEntries", () -> {
            OpenAddressingMap<Integer, Integer> m = new OpenAddressingMap<>(2);
            for (int i = 0; i < 1000; i++) m.put(i, i * i);
            check(m.size() == 1000 && m.capacity() >= 2000, "grew with load factor 0.5");
            for (int i = 0; i < 1000; i++) check(m.get(i) == i * i, "value " + i);
        });

        run("keysListing", () -> {
            OpenAddressingMap<String, Integer> m = new OpenAddressingMap<>();
            m.put("a", 1);
            m.put("b", 2);
            m.put("c", 3);
            m.remove("b");
            check(new HashSet<>(m.keys()).equals(java.util.Set.of("a", "c")), "keys");
        });

        run("randomAgainstHashMap", () -> {
            Random rng = new Random(10);
            OpenAddressingMap<Integer, Integer> m = new OpenAddressingMap<>(4);
            Map<Integer, Integer> ref = new HashMap<>();
            for (int step = 0; step < 50000; step++) {
                int k = rng.nextInt(500), op = rng.nextInt(3);
                if (op == 0) check(java.util.Objects.equals(m.put(k, step), ref.put(k, step)), "put " + k);
                else if (op == 1) check(java.util.Objects.equals(m.remove(k), ref.remove(k)), "remove " + k);
                else check(java.util.Objects.equals(m.get(k), ref.get(k)), "get " + k);
                check(m.size() == ref.size(), "size");
            }
        });

        run("randomCollidingKeys", () -> {
            Random rng = new Random(11);
            OpenAddressingMap<BadKey, Integer> m = new OpenAddressingMap<>(8);
            Map<BadKey, Integer> ref = new HashMap<>();
            for (int step = 0; step < 20000; step++) {
                int id = rng.nextInt(60);
                BadKey k = new BadKey(id, id % 4); // only 4 distinct hashes
                if (rng.nextBoolean()) check(java.util.Objects.equals(m.put(k, step), ref.put(k, step)), "put");
                else check(java.util.Objects.equals(m.remove(k), ref.remove(k)), "remove");
            }
            for (Map.Entry<BadKey, Integer> e : ref.entrySet()) check(m.get(e.getKey()).equals(e.getValue()), "final");
        });

        System.out.println("All " + passed + " tests passed");
    }
}
