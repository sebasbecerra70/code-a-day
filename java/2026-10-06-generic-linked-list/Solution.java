import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/** A generic doubly linked list with sentinel nodes, a fail-fast iterator, and reverse(). */
class DoublyLinkedList<T> implements Iterable<T> {
    private static final class Node<T> {
        T value;
        Node<T> prev, next;

        Node(T value) {
            this.value = value;
        }
    }

    // Sentinels: head.next is the first element, tail.prev the last. No null checks needed.
    private final Node<T> head = new Node<>(null);
    private final Node<T> tail = new Node<>(null);
    private int size;
    private int modCount; // bumped on structural changes so iterators can detect them

    DoublyLinkedList() {
        head.next = tail;
        tail.prev = head;
    }

    @SafeVarargs
    static <T> DoublyLinkedList<T> of(T... values) {
        DoublyLinkedList<T> list = new DoublyLinkedList<>();
        for (T v : values) list.addLast(v);
        return list;
    }

    int size() {
        return size;
    }

    boolean isEmpty() {
        return size == 0;
    }

    void addFirst(T value) {
        linkAfter(head, value);
    }

    void addLast(T value) {
        linkAfter(tail.prev, value);
    }

    /** Inserts at position index (0..size). Walks from whichever end is closer. */
    void add(int index, T value) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException("index " + index);
        linkAfter(index == size ? tail.prev : nodeAt(index).prev, value);
    }

    T get(int index) {
        checkElementIndex(index);
        return nodeAt(index).value;
    }

    T set(int index, T value) {
        checkElementIndex(index);
        Node<T> n = nodeAt(index);
        T old = n.value;
        n.value = value;
        return old;
    }

    T removeFirst() {
        if (isEmpty()) throw new NoSuchElementException();
        return unlink(head.next);
    }

    T removeLast() {
        if (isEmpty()) throw new NoSuchElementException();
        return unlink(tail.prev);
    }

    T remove(int index) {
        checkElementIndex(index);
        return unlink(nodeAt(index));
    }

    /** Removes every element matching the predicate; returns how many were removed. */
    int removeIf(Predicate<? super T> pred) {
        int removed = 0;
        for (Node<T> n = head.next; n != tail; ) {
            Node<T> next = n.next;
            if (pred.test(n.value)) {
                unlink(n);
                removed++;
            }
            n = next;
        }
        return removed;
    }

    int indexOf(Object o) {
        int i = 0;
        for (Node<T> n = head.next; n != tail; n = n.next, i++) {
            if (o == null ? n.value == null : o.equals(n.value)) return i;
        }
        return -1;
    }

    /** Reverses in place by swapping each node's prev/next pointers. */
    void reverse() {
        Node<T> n = head;
        while (n != null) {
            Node<T> next = n.next;
            n.next = n.prev;
            n.prev = next;
            n = next;
        }
        // The old tail is now the front, so swap the sentinels' roles back.
        Node<T> first = tail.next, last = head.prev;
        head.next = first;
        head.prev = null;
        tail.prev = last;
        tail.next = null;
        first.prev = head;
        last.next = tail;
        modCount++;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Node<T> cursor = head.next;
            private Node<T> lastReturned;
            private int expectedModCount = modCount;

            @Override
            public boolean hasNext() {
                return cursor != tail;
            }

            @Override
            public T next() {
                checkForComodification();
                if (!hasNext()) throw new NoSuchElementException();
                lastReturned = cursor;
                cursor = cursor.next;
                return lastReturned.value;
            }

            @Override
            public void remove() {
                checkForComodification();
                if (lastReturned == null) throw new IllegalStateException();
                unlink(lastReturned);
                lastReturned = null;
                expectedModCount = modCount;
            }

            private void checkForComodification() {
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Node<T> n = head.next; n != tail; n = n.next) {
            sb.append(n.value);
            if (n.next != tail) sb.append(", ");
        }
        return sb.append(']').toString();
    }

    private void linkAfter(Node<T> pred, T value) {
        Node<T> node = new Node<>(value);
        node.prev = pred;
        node.next = pred.next;
        pred.next.prev = node;
        pred.next = node;
        size++;
        modCount++;
    }

    private T unlink(Node<T> n) {
        n.prev.next = n.next;
        n.next.prev = n.prev;
        n.prev = n.next = null; // help GC and catch stale use
        size--;
        modCount++;
        return n.value;
    }

    private Node<T> nodeAt(int index) {
        if (index < size / 2) {
            Node<T> n = head.next;
            for (int i = 0; i < index; i++) n = n.next;
            return n;
        }
        Node<T> n = tail.prev;
        for (int i = size - 1; i > index; i--) n = n.prev;
        return n;
    }

    private void checkElementIndex(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("index " + index);
    }
}
