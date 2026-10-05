import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/** Either a single integer or a list of nested elements (like [1,[2,[3]],[]]). */
sealed interface Nested permits Nested.Int, Nested.Many {
    record Int(int value) implements Nested {}

    record Many(List<Nested> items) implements Nested {
        public Many {
            items = List.copyOf(items);
        }
    }

    static Nested of(int v) {
        return new Int(v);
    }

    static Nested of(Nested... items) {
        return new Many(List.of(items));
    }
}

/**
 * Lazily yields every integer in depth-first order. Keeps a stack of iterators,
 * one per open list, so memory is O(depth), not O(total size).
 */
class NestedIterator implements Iterator<Integer> {
    private final Deque<Iterator<Nested>> stack = new ArrayDeque<>();
    private Integer next; // look-ahead; null means "not computed yet"

    NestedIterator(List<Nested> list) {
        stack.push(list.iterator());
    }

    /**
     * Advances to the next integer, descending into lists and popping exhausted
     * iterators. Doing the work here (not in next()) makes hasNext() correct for
     * inputs like [[], [[]]] that contain no integers at all.
     */
    @Override
    public boolean hasNext() {
        while (next == null && !stack.isEmpty()) {
            Iterator<Nested> top = stack.peek();
            if (!top.hasNext()) {
                stack.pop();
                continue;
            }
            switch (top.next()) {
                case Nested.Int i -> next = i.value();
                case Nested.Many m -> stack.push(m.items().iterator());
            }
        }
        return next != null;
    }

    @Override
    public Integer next() {
        if (!hasNext()) throw new NoSuchElementException();
        Integer v = next;
        next = null;
        return v;
    }
}

/** Flattens an iterator of iterators (e.g. a 2D jagged array), skipping empty inner ones. */
class FlattenIterator<T> implements Iterator<T> {
    private final Iterator<? extends Iterator<? extends T>> outer;
    private Iterator<? extends T> inner = Collections.emptyIterator();
    private Iterator<? extends T> lastReturnedFrom;

    FlattenIterator(Iterator<? extends Iterator<? extends T>> outer) {
        this.outer = outer;
    }

    @Override
    public boolean hasNext() {
        while (!inner.hasNext() && outer.hasNext()) inner = outer.next();
        return inner.hasNext();
    }

    @Override
    public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        lastReturnedFrom = inner;
        return inner.next();
    }

    /** Delegates to the iterator that produced the last element (hasNext may have moved past it). */
    @Override
    public void remove() {
        if (lastReturnedFrom == null) throw new IllegalStateException();
        lastReturnedFrom.remove();
        lastReturnedFrom = null;
    }
}
