import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A resizable double-ended queue backed by a circular array whose capacity is always a
 * power of two, so "mod capacity" is a cheap bit mask.
 */
class ArrayDeque<T> implements Iterable<T> {
    private Object[] items;
    private int head; // index of the first element
    private int size;
    private int modCount;

    ArrayDeque() {
        this(8);
    }

    ArrayDeque(int initialCapacity) {
        if (initialCapacity < 1) throw new IllegalArgumentException("capacity must be positive");
        int cap = Integer.highestOneBit(Math.max(2, initialCapacity - 1)) << 1; // next power of two
        items = new Object[cap];
    }

    int size() {
        return size;
    }

    boolean isEmpty() {
        return size == 0;
    }

    int capacity() {
        return items.length;
    }

    void addFirst(T value) {
        if (size == items.length) grow();
        head = (head - 1) & mask();
        items[head] = value;
        size++;
        modCount++;
    }

    void addLast(T value) {
        if (size == items.length) grow();
        items[(head + size) & mask()] = value;
        size++;
        modCount++;
    }

    T removeFirst() {
        if (isEmpty()) throw new NoSuchElementException("deque is empty");
        T v = elementAt(head);
        items[head] = null; // let the GC reclaim it
        head = (head + 1) & mask();
        size--;
        modCount++;
        shrinkIfSparse();
        return v;
    }

    T removeLast() {
        if (isEmpty()) throw new NoSuchElementException("deque is empty");
        int idx = (head + size - 1) & mask();
        T v = elementAt(idx);
        items[idx] = null;
        size--;
        modCount++;
        shrinkIfSparse();
        return v;
    }

    T peekFirst() {
        return isEmpty() ? null : elementAt(head);
    }

    T peekLast() {
        return isEmpty() ? null : elementAt((head + size - 1) & mask());
    }

    /** Random access by logical position, 0 = front. */
    T get(int i) {
        if (i < 0 || i >= size) throw new IndexOutOfBoundsException("index " + i);
        return elementAt((head + i) & mask());
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int i = 0;
            private final int expected = modCount;

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public T next() {
                if (modCount != expected) throw new ConcurrentModificationException();
                if (!hasNext()) throw new NoSuchElementException();
                return get(i++);
            }
        };
    }

    private int mask() {
        return items.length - 1;
    }

    @SuppressWarnings("unchecked")
    private T elementAt(int physical) {
        return (T) items[physical];
    }

    /** Doubles capacity and unrolls the ring so the front lands at index 0. */
    private void grow() {
        resize(items.length << 1);
    }

    /** Halve when only a quarter full (not half) to avoid thrashing at the boundary. */
    private void shrinkIfSparse() {
        if (items.length > 8 && size <= items.length / 4) resize(items.length >> 1);
    }

    private void resize(int newCap) {
        Object[] next = new Object[newCap];
        for (int i = 0; i < size; i++) next[i] = items[(head + i) & mask()];
        items = next;
        head = 0;
    }
}
