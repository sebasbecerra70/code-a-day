import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
        if (threadFailure.get() != null) {
            throw new AssertionError(name + " failed in a worker thread: " + threadFailure.get(), threadFailure.get());
        }
        passed++;
    }

    interface Action {
        void run() throws Exception;
    }

    // Failures inside worker threads are recorded here so the test thread can assert on them.
    static final AtomicReference<Throwable> threadFailure = new AtomicReference<>();

    static Thread start(Action a) {
        Thread t = new Thread(() -> {
            try {
                a.run();
            } catch (Throwable e) {
                threadFailure.compareAndSet(null, e);
            }
        });
        t.start();
        return t;
    }

    public static void main(String[] args) {
        run("basicPutGetRemove", () -> {
            ReadWriteCache<String, Integer> c = new ReadWriteCache<>();
            check(c.get("a") == null, "miss");
            c.put("a", 1);
            c.put("a", 2);
            check(c.get("a") == 2 && c.size() == 1, "overwrite");
            check(c.remove("a") == 2 && c.get("a") == null, "remove");
        });

        run("unlockWithoutLockThrows", () -> {
            SimpleReadWriteLock l = new SimpleReadWriteLock();
            expectThrows(IllegalMonitorStateException.class, l::unlockRead);
            expectThrows(IllegalMonitorStateException.class, l::unlockWrite);
        });

        run("readersShareTheLock", () -> {
            SimpleReadWriteLock l = new SimpleReadWriteLock();
            int n = 4;
            CountDownLatch allHolding = new CountDownLatch(n);
            CountDownLatch release = new CountDownLatch(1);
            List<Thread> ts = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                ts.add(start(() -> {
                    l.lockRead();
                    allHolding.countDown();
                    release.await();
                    l.unlockRead();
                }));
            }
            check(allHolding.await(2, TimeUnit.SECONDS), "all readers inside at once");
            check(l.readers() == n, "reader count");
            release.countDown();
            for (Thread t : ts) t.join();
            check(l.readers() == 0, "released");
        });

        run("writerExcludesReaders", () -> {
            SimpleReadWriteLock l = new SimpleReadWriteLock();
            l.lockWrite();
            AtomicBoolean readerIn = new AtomicBoolean();
            Thread r = start(() -> {
                l.lockRead();
                readerIn.set(true);
                l.unlockRead();
            });
            Thread.sleep(50);
            check(!readerIn.get(), "reader must wait for writer");
            l.unlockWrite();
            r.join(2000);
            check(readerIn.get(), "reader proceeds after writer");
        });

        run("waitingWriterBlocksNewReaders", () -> {
            SimpleReadWriteLock l = new SimpleReadWriteLock();
            l.lockRead(); // main holds a read lock
            AtomicBoolean writerIn = new AtomicBoolean(), lateReaderIn = new AtomicBoolean();
            Thread w = start(() -> {
                l.lockWrite();
                writerIn.set(true);
                Thread.sleep(30);
                l.unlockWrite();
            });
            Thread.sleep(30); // writer is now waiting
            Thread r = start(() -> {
                l.lockRead();
                lateReaderIn.set(true);
                check(writerIn.get(), "late reader should get in only after the writer");
                l.unlockRead();
            });
            Thread.sleep(30);
            check(!lateReaderIn.get() && !writerIn.get(), "both waiting");
            l.unlockRead();
            w.join(2000);
            r.join(2000);
            check(writerIn.get() && lateReaderIn.get(), "both done");
        });

        run("getOrComputeLoadsOnceUnderContention", () -> {
            ReadWriteCache<Integer, String> c = new ReadWriteCache<>();
            AtomicInteger loads = new AtomicInteger();
            CountDownLatch go = new CountDownLatch(1);
            List<Thread> ts = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                ts.add(start(() -> {
                    go.await();
                    for (int k = 0; k < 50; k++) {
                        String v = c.getOrCompute(k, key -> {
                            loads.incrementAndGet();
                            return "v" + key;
                        });
                        check(v.equals("v" + k), "value");
                    }
                }));
            }
            go.countDown();
            for (Thread t : ts) t.join(5000);
            check(loads.get() == 50, "each key loaded once, got " + loads.get());
            check(c.misses() == 50 && c.hits() == 16 * 50 - 50, "stats " + c.hits() + "/" + c.misses());
        });

        run("randomConcurrentOpsStayConsistent", () -> {
            ReadWriteCache<Integer, Integer> c = new ReadWriteCache<>();
            AtomicBoolean bad = new AtomicBoolean();
            List<Thread> ts = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                int seed = i;
                ts.add(start(() -> {
                    Random rnd = new Random(seed);
                    for (int op = 0; op < 5000; op++) {
                        int k = rnd.nextInt(20);
                        switch (rnd.nextInt(3)) {
                            case 0 -> c.put(k, k * 10); // value is always a function of key
                            case 1 -> c.remove(k);
                            default -> {
                                Integer v = c.get(k);
                                if (v != null && v != k * 10) bad.set(true);
                            }
                        }
                    }
                }));
            }
            for (Thread t : ts) t.join(10_000);
            check(!bad.get(), "observed a torn value");
            check(c.size() <= 20, "size bound");
            check(c.lock().readers() == 0 && !c.lock().isWriteLocked(), "lock fully released");
        });

        System.out.println("All " + passed + " tests passed");
    }
}
