import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Writer-preferring read-write lock built on a monitor. Many readers may hold
 * it at once; a writer holds it alone. New readers queue behind a waiting
 * writer, so a steady stream of readers can't starve writers. Not reentrant.
 */
class SimpleReadWriteLock {
    private int activeReaders;
    private boolean writerActive;
    private int waitingWriters;

    synchronized void lockRead() throws InterruptedException {
        while (writerActive || waitingWriters > 0) wait();
        activeReaders++;
    }

    synchronized void unlockRead() {
        if (activeReaders == 0) throw new IllegalMonitorStateException("no read lock held");
        if (--activeReaders == 0) notifyAll(); // a writer may be waiting for zero readers
    }

    synchronized void lockWrite() throws InterruptedException {
        waitingWriters++;
        try {
            while (writerActive || activeReaders > 0) wait();
        } finally {
            waitingWriters--;
        }
        writerActive = true;
    }

    synchronized void unlockWrite() {
        if (!writerActive) throw new IllegalMonitorStateException("no write lock held");
        writerActive = false;
        notifyAll(); // wake both readers and writers; they re-check their conditions
    }

    synchronized int readers() {
        return activeReaders;
    }

    synchronized boolean isWriteLocked() {
        return writerActive;
    }
}

/**
 * A read-mostly cache: lookups take the shared read lock, so they run in
 * parallel; writes and misses take the exclusive write lock.
 */
class ReadWriteCache<K, V> {
    private final Map<K, V> map = new HashMap<>();
    private final SimpleReadWriteLock lock = new SimpleReadWriteLock();
    private final AtomicLong hits = new AtomicLong(), misses = new AtomicLong();

    V get(K key) throws InterruptedException {
        lock.lockRead();
        try {
            return map.get(key);
        } finally {
            lock.unlockRead();
        }
    }

    void put(K key, V value) throws InterruptedException {
        lock.lockWrite();
        try {
            map.put(key, value);
        } finally {
            lock.unlockWrite();
        }
    }

    V remove(K key) throws InterruptedException {
        lock.lockWrite();
        try {
            return map.remove(key);
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * Returns the cached value, computing it at most once per key even under
     * contention. The read lock can't be upgraded, so on a miss we release it,
     * take the write lock, and check again: another thread may have filled it.
     */
    V getOrCompute(K key, Function<? super K, ? extends V> loader) throws InterruptedException {
        lock.lockRead();
        try {
            V v = map.get(key);
            if (v != null) {
                hits.incrementAndGet();
                return v;
            }
        } finally {
            lock.unlockRead();
        }
        lock.lockWrite();
        try {
            V v = map.get(key);
            if (v == null) {
                misses.incrementAndGet();
                v = loader.apply(key);
                map.put(key, v);
            } else {
                hits.incrementAndGet();
            }
            return v;
        } finally {
            lock.unlockWrite();
        }
    }

    int size() throws InterruptedException {
        lock.lockRead();
        try {
            return map.size();
        } finally {
            lock.unlockRead();
        }
    }

    long hits() {
        return hits.get();
    }

    long misses() {
        return misses.get();
    }

    // Exposed for tests.
    SimpleReadWriteLock lock() {
        return lock;
    }
}
