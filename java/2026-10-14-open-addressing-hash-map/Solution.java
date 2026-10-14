import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Hash map with open addressing and linear probing. Entries live directly in parallel
 * arrays; collisions probe the next slot. Deletion uses backward-shift instead of
 * tombstones, so lookups never have to skip over dead slots.
 */
class OpenAddressingMap<K, V> {
    private static final double MAX_LOAD = 0.5;

    private Object[] keys;
    private Object[] values;
    private int size;

    OpenAddressingMap() {
        this(16);
    }

    OpenAddressingMap(int initialCapacity) {
        int cap = 2;
        while (cap < initialCapacity) cap <<= 1; // power of two: index = hash & (cap - 1)
        keys = new Object[cap];
        values = new Object[cap];
    }

    int size() {
        return size;
    }

    boolean isEmpty() {
        return size == 0;
    }

    int capacity() {
        return keys.length;
    }

    /** Returns the previous value, or null if the key was absent. */
    V put(K key, V value) {
        Objects.requireNonNull(key, "null keys are not supported");
        if (size + 1 > keys.length * MAX_LOAD) resize(keys.length << 1);
        int i = findSlot(key);
        if (keys[i] != null) {
            V old = valueAt(i);
            values[i] = value;
            return old;
        }
        keys[i] = key;
        values[i] = value;
        size++;
        return null;
    }

    V get(Object key) {
        if (key == null) return null;
        int i = findSlot(key);
        return keys[i] == null ? null : valueAt(i);
    }

    boolean containsKey(Object key) {
        return key != null && keys[findSlot(key)] != null;
    }

    V remove(Object key) {
        if (key == null) return null;
        int i = findSlot(key);
        if (keys[i] == null) return null;
        V old = valueAt(i);
        keys[i] = values[i] = null;
        size--;
        // Backward shift: pull later entries of the same probe run into the hole, so that
        // every remaining key stays reachable from its home slot without gaps.
        int mask = keys.length - 1;
        int hole = i;
        for (int j = (i + 1) & mask; keys[j] != null; j = (j + 1) & mask) {
            int home = indexFor(keys[j]);
            // Entry j may move to the hole only if its home is not cyclically in (hole, j].
            if (cyclicDistance(home, j, mask) >= cyclicDistance(hole, j, mask)) {
                keys[hole] = keys[j];
                values[hole] = values[j];
                keys[j] = values[j] = null;
                hole = j;
            }
        }
        return old;
    }

    @SuppressWarnings("unchecked")
    List<K> keys() {
        List<K> out = new ArrayList<>(size);
        for (Object k : keys) if (k != null) out.add((K) k);
        return out;
    }

    /** Longest probe sequence currently in the table (a measure of clustering). */
    int maxProbeLength() {
        int mask = keys.length - 1, worst = 0;
        for (int i = 0; i < keys.length; i++)
            if (keys[i] != null) worst = Math.max(worst, cyclicDistance(indexFor(keys[i]), i, mask) + 1);
        return worst;
    }

    /** Slot holding key, or the empty slot where it would be inserted. */
    private int findSlot(Object key) {
        int mask = keys.length - 1;
        int i = indexFor(key);
        while (keys[i] != null && !keys[i].equals(key)) i = (i + 1) & mask;
        return i;
    }

    private int indexFor(Object key) {
        int h = key.hashCode();
        h ^= (h >>> 16); // mix high bits into low bits, since the mask keeps only low bits
        return h & (keys.length - 1);
    }

    private static int cyclicDistance(int from, int to, int mask) {
        return (to - from) & mask;
    }

    private void resize(int newCap) {
        Object[] oldKeys = keys, oldValues = values;
        keys = new Object[newCap];
        values = new Object[newCap];
        size = 0;
        for (int i = 0; i < oldKeys.length; i++) {
            if (oldKeys[i] != null) {
                int slot = findSlot(oldKeys[i]);
                keys[slot] = oldKeys[i];
                values[slot] = oldValues[i];
                size++;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private V valueAt(int i) {
        return (V) values[i];
    }
}
