import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Fixed-capacity FIFO queue that blocks producers when full and consumers when
 * empty. A ring buffer holds the items; one lock guards it, with separate
 * conditions so a put wakes only consumers and a take wakes only producers.
 */
class BoundedBlockingQueue<E> {
    private final Object[] items;
    private int head, tail, count;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    BoundedBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        items = new Object[capacity];
    }

    /** Blocks until space is available, then inserts. */
    void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        lock.lockInterruptibly();
        try {
            // Always wait in a loop: guards against spurious wakeups and lost races.
            while (count == items.length) notFull.await();
            enqueue(e);
        } finally {
            lock.unlock();
        }
    }

    /** Blocks until an element is available, then removes and returns it. */
    E take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (count == 0) notEmpty.await();
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    /** Inserts if space frees up within the timeout; returns false otherwise. */
    boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (count == items.length) {
                if (nanos <= 0) return false;
                nanos = notFull.awaitNanos(nanos); // returns the time remaining
            }
            enqueue(e);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /** Removes the head if one arrives within the timeout; returns null otherwise. */
    E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (count == 0) {
                if (nanos <= 0) return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    /** Non-blocking insert. */
    boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        lock.lock();
        try {
            if (count == items.length) return false;
            enqueue(e);
            return true;
        } finally {
            lock.unlock();
        }
    }

    int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    int capacity() {
        return items.length;
    }

    // Callers hold the lock.
    private void enqueue(E e) {
        items[tail] = e;
        tail = (tail + 1) % items.length;
        count++;
        notEmpty.signal();
    }

    @SuppressWarnings("unchecked")
    private E dequeue() {
        E e = (E) items[head];
        items[head] = null; // let GC reclaim it
        head = (head + 1) % items.length;
        count--;
        notFull.signal();
        return e;
    }
}
