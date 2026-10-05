import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
        run("rejectsBadCapacityAndNulls", () -> {
            expectThrows(IllegalArgumentException.class, () -> new BoundedBlockingQueue<Integer>(0));
            BoundedBlockingQueue<String> q = new BoundedBlockingQueue<>(1);
            expectThrows(NullPointerException.class, () -> q.offer(null));
        });

        run("fifoOrderWithWraparound", () -> {
            BoundedBlockingQueue<Integer> q = new BoundedBlockingQueue<>(3);
            for (int round = 0; round < 5; round++) {
                q.put(round * 10);
                q.put(round * 10 + 1);
                check(q.take() == round * 10 && q.take() == round * 10 + 1, "fifo round " + round);
            }
            check(q.size() == 0, "empty after");
        });

        run("nonBlockingOfferWhenFull", () -> {
            BoundedBlockingQueue<Integer> q = new BoundedBlockingQueue<>(2);
            check(q.offer(1) && q.offer(2), "fits");
            check(!q.offer(3), "full");
            check(q.size() == 2 && q.capacity() == 2, "size");
        });

        run("timedOfferAndPollTimeOut", () -> {
            BoundedBlockingQueue<Integer> q = new BoundedBlockingQueue<>(1);
            check(q.poll(20, TimeUnit.MILLISECONDS) == null, "poll empty times out");
            q.put(1);
            long start = System.nanoTime();
            check(!q.offer(2, 30, TimeUnit.MILLISECONDS), "offer full times out");
            check(System.nanoTime() - start >= TimeUnit.MILLISECONDS.toNanos(25), "actually waited");
        });

        run("putBlocksUntilTake", () -> {
            BoundedBlockingQueue<Integer> q = new BoundedBlockingQueue<>(1);
            q.put(1);
            AtomicBoolean done = new AtomicBoolean();
            Thread producer = new Thread(() -> {
                try {
                    q.put(2);
                    done.set(true);
                } catch (InterruptedException ignored) {
                }
            });
            producer.start();
            Thread.sleep(50);
            check(!done.get(), "producer should be blocked");
            check(q.take() == 1, "take first");
            producer.join(2000);
            check(done.get() && q.take() == 2, "producer unblocked");
        });

        run("takeBlocksUntilPut", () -> {
            BoundedBlockingQueue<String> q = new BoundedBlockingQueue<>(4);
            CountDownLatch started = new CountDownLatch(1);
            String[] got = new String[1];
            Thread consumer = new Thread(() -> {
                started.countDown();
                try {
                    got[0] = q.take();
                } catch (InterruptedException ignored) {
                }
            });
            consumer.start();
            started.await();
            Thread.sleep(30);
            q.put("hello");
            consumer.join(2000);
            check("hello".equals(got[0]), "consumer received");
        });

        run("interruptWhileBlocked", () -> {
            BoundedBlockingQueue<Integer> q = new BoundedBlockingQueue<>(1);
            AtomicBoolean interrupted = new AtomicBoolean();
            Thread t = new Thread(() -> {
                try {
                    q.take();
                } catch (InterruptedException e) {
                    interrupted.set(true);
                }
            });
            t.start();
            Thread.sleep(30);
            t.interrupt();
            t.join(2000);
            check(interrupted.get(), "take should throw InterruptedException");
        });

        run("manyProducersManyConsumers", () -> {
            BoundedBlockingQueue<Integer> q = new BoundedBlockingQueue<>(8);
            int producers = 4, consumers = 4, perProducer = 5000;
            AtomicLong sum = new AtomicLong();
            List<Thread> threads = new ArrayList<>();
            for (int p = 0; p < producers; p++) {
                threads.add(new Thread(() -> {
                    try {
                        for (int i = 1; i <= perProducer; i++) q.put(i);
                    } catch (InterruptedException ignored) {
                    }
                }));
            }
            int total = producers * perProducer;
            for (int c = 0; c < consumers; c++) {
                threads.add(new Thread(() -> {
                    try {
                        for (int i = 0; i < total / consumers; i++) sum.addAndGet(q.take());
                    } catch (InterruptedException ignored) {
                    }
                }));
            }
            threads.forEach(Thread::start);
            for (Thread t : threads) t.join(10_000);
            long expected = (long) producers * perProducer * (perProducer + 1) / 2;
            check(sum.get() == expected, "sum " + sum.get() + " != " + expected);
            check(q.size() == 0, "drained");
        });

        System.out.println("All " + passed + " tests passed");
    }
}
