import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * A fixed-size worker pool fed by a bounded queue: callers are producers,
 * workers are consumers. A full queue blocks submitters (backpressure).
 * Shutdown enqueues one "poison pill" per worker after all real tasks.
 */
class SimpleExecutor {
    private static final Runnable POISON = () -> {};

    private final BlockingQueue<Runnable> queue;
    private final List<Thread> workers = new ArrayList<>();
    private final Object stateLock = new Object();
    private final AtomicInteger failedTasks = new AtomicInteger();
    private boolean shutdown;

    SimpleExecutor(int threads, int queueCapacity) {
        if (threads <= 0 || queueCapacity <= 0) throw new IllegalArgumentException();
        queue = new ArrayBlockingQueue<>(queueCapacity);
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(this::workLoop, "worker-" + i);
            workers.add(t);
            t.start();
        }
    }

    private void workLoop() {
        while (true) {
            Runnable task;
            try {
                task = queue.take();
            } catch (InterruptedException e) {
                continue; // shutdownNow interrupts; the pill that follows ends the loop
            }
            if (task == POISON) return;
            try {
                task.run();
            } catch (Throwable t) {
                failedTasks.incrementAndGet(); // a bad task must not kill the worker
            }
            Thread.interrupted(); // clear any interrupt aimed at the finished task
        }
    }

    /** Enqueues a task, blocking while the queue is full. */
    void execute(Runnable task) throws InterruptedException {
        // Checking the flag and enqueuing under one lock means no task can land behind the pills.
        synchronized (stateLock) {
            if (shutdown) throw new RejectedExecutionException("executor is shut down");
            queue.put(task);
        }
    }

    /** Runs a callable and exposes its result (or exception) as a future. */
    <T> CompletableFuture<T> submit(Callable<T> callable) throws InterruptedException {
        CompletableFuture<T> future = new CompletableFuture<>();
        execute(() -> {
            try {
                future.complete(callable.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /** Stops accepting tasks; queued tasks still run. */
    void shutdown() throws InterruptedException {
        synchronized (stateLock) {
            if (shutdown) return;
            shutdown = true;
        }
        for (int i = 0; i < workers.size(); i++) queue.put(POISON);
    }

    /** Stops accepting tasks, discards queued ones, interrupts running ones. Returns the discarded tasks. */
    List<Runnable> shutdownNow() throws InterruptedException {
        List<Runnable> pending = new ArrayList<>();
        synchronized (stateLock) {
            shutdown = true;
            queue.drainTo(pending);
        }
        pending.removeIf(r -> r == POISON); // in case shutdown() ran first
        for (Thread w : workers) w.interrupt();
        for (int i = 0; i < workers.size(); i++) queue.put(POISON);
        return pending;
    }

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        for (Thread w : workers) {
            long remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
            if (remainingMs <= 0) return !anyAlive();
            w.join(remainingMs);
        }
        return !anyAlive();
    }

    private boolean anyAlive() {
        for (Thread w : workers) if (w.isAlive()) return true;
        return false;
    }

    int failedTaskCount() {
        return failedTasks.get();
    }

    /** Convenience: applies {@code fn} to each input in parallel and returns results in input order. */
    static <A, B> List<B> parallelMap(List<A> inputs, Function<A, B> fn, int threads) throws Exception {
        SimpleExecutor ex = new SimpleExecutor(threads, Math.max(1, threads * 2));
        try {
            List<CompletableFuture<B>> futures = new ArrayList<>();
            for (A a : inputs) futures.add(ex.submit(() -> fn.apply(a)));
            List<B> out = new ArrayList<>();
            for (CompletableFuture<B> f : futures) out.add(f.get());
            return out;
        } finally {
            ex.shutdown();
        }
    }
}
