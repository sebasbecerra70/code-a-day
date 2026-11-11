import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
        run("rejectsBadConfig", () -> {
            expectThrows(IllegalArgumentException.class, () -> new SimpleExecutor(0, 1));
            expectThrows(IllegalArgumentException.class, () -> new SimpleExecutor(1, 0));
        });

        run("runsAllTasksThenShutsDown", () -> {
            SimpleExecutor ex = new SimpleExecutor(4, 16);
            AtomicInteger counter = new AtomicInteger();
            for (int i = 0; i < 1000; i++) ex.execute(counter::incrementAndGet);
            ex.shutdown();
            check(ex.awaitTermination(5, TimeUnit.SECONDS), "terminated");
            check(counter.get() == 1000, "all tasks ran: " + counter.get());
        });

        run("submitReturnsResults", () -> {
            SimpleExecutor ex = new SimpleExecutor(2, 4);
            CompletableFuture<Integer> f = ex.submit(() -> 6 * 7);
            check(f.get(2, TimeUnit.SECONDS) == 42, "result");
            ex.shutdown();
            ex.awaitTermination(2, TimeUnit.SECONDS);
        });

        run("exceptionsPropagateAndWorkerSurvives", () -> {
            SimpleExecutor ex = new SimpleExecutor(1, 4);
            CompletableFuture<Integer> bad = ex.submit(() -> {
                throw new IllegalStateException("boom");
            });
            try {
                bad.get(2, TimeUnit.SECONDS);
                check(false, "should have thrown");
            } catch (ExecutionException e) {
                check(e.getCause() instanceof IllegalStateException, "cause");
            }
            ex.execute(() -> {
                throw new RuntimeException("raw");
            });
            check(ex.submit(() -> "still alive").get(2, TimeUnit.SECONDS).equals("still alive"), "worker survived");
            ex.shutdown();
            ex.awaitTermination(2, TimeUnit.SECONDS);
            check(ex.failedTaskCount() == 1, "raw failure counted");
        });

        run("rejectsAfterShutdown", () -> {
            SimpleExecutor ex = new SimpleExecutor(1, 1);
            ex.shutdown();
            ex.shutdown(); // idempotent
            expectThrows(RejectedExecutionException.class, () -> {
                try {
                    ex.execute(() -> {});
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            check(ex.awaitTermination(2, TimeUnit.SECONDS), "terminated");
        });

        run("usesMultipleThreads", () -> {
            SimpleExecutor ex = new SimpleExecutor(4, 8);
            Set<String> names = ConcurrentHashMap.newKeySet();
            CountDownLatch allIn = new CountDownLatch(4);
            for (int i = 0; i < 4; i++) {
                ex.execute(() -> {
                    names.add(Thread.currentThread().getName());
                    allIn.countDown();
                    try {
                        allIn.await(2, TimeUnit.SECONDS); // forces 4 concurrent workers
                    } catch (InterruptedException ignored) {
                    }
                });
            }
            ex.shutdown();
            check(ex.awaitTermination(5, TimeUnit.SECONDS), "terminated");
            check(names.size() == 4, "distinct workers: " + names);
        });

        run("shutdownNowDiscardsAndInterrupts", () -> {
            SimpleExecutor ex = new SimpleExecutor(1, 10);
            CountDownLatch running = new CountDownLatch(1);
            AtomicInteger interrupted = new AtomicInteger();
            ex.execute(() -> {
                running.countDown();
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    interrupted.incrementAndGet();
                }
            });
            running.await();
            for (int i = 0; i < 5; i++) ex.execute(() -> {});
            List<Runnable> pending = ex.shutdownNow();
            check(pending.size() == 5, "pending " + pending.size());
            check(ex.awaitTermination(2, TimeUnit.SECONDS), "terminated quickly");
            check(interrupted.get() == 1, "running task interrupted");
        });

        run("backpressureBlocksProducer", () -> {
            SimpleExecutor ex = new SimpleExecutor(1, 1);
            CountDownLatch release = new CountDownLatch(1);
            ex.execute(() -> {
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                }
            });
            Thread.sleep(30); // let the worker take the blocking task
            ex.execute(() -> {}); // fills the queue
            AtomicInteger submitted = new AtomicInteger();
            Thread producer = new Thread(() -> {
                try {
                    ex.execute(() -> {});
                    submitted.set(1);
                } catch (InterruptedException ignored) {
                }
            });
            producer.start();
            Thread.sleep(50);
            check(submitted.get() == 0, "producer should block on full queue");
            release.countDown();
            producer.join(2000);
            check(submitted.get() == 1, "producer unblocked");
            ex.shutdown();
            ex.awaitTermination(2, TimeUnit.SECONDS);
        });

        run("parallelMapPreservesOrder", () -> {
            List<Integer> in = new ArrayList<>();
            for (int i = 0; i < 200; i++) in.add(i);
            List<Integer> out = SimpleExecutor.parallelMap(in, x -> x * x, 4);
            for (int i = 0; i < 200; i++) check(out.get(i) == i * i, "index " + i);
            check(SimpleExecutor.parallelMap(List.<Integer>of(), x -> x, 2).isEmpty(), "empty input");
        });

        System.out.println("All " + passed + " tests passed");
    }
}
