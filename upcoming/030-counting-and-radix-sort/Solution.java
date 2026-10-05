import java.util.function.ToIntFunction;

/** Non-comparison sorts: they beat the O(n log n) bound by exploiting the key structure. */
class LinearSorts {
    /**
     * Stable counting sort for ints in any range [min, max]. Runs in O(n + k)
     * where k = max - min + 1, so it only pays off when k is O(n).
     */
    static int[] countingSort(int[] a) {
        if (a.length == 0) return new int[0];
        int min = a[0], max = a[0];
        for (int x : a) {
            min = Math.min(min, x);
            max = Math.max(max, x);
        }
        long range = (long) max - min + 1;
        if (range > 100_000_000L) throw new IllegalArgumentException("key range too large: " + range);
        int[] count = new int[(int) range];
        for (int x : a) count[x - min]++;
        int[] out = new int[a.length];
        int k = 0;
        for (int v = 0; v < count.length; v++) {
            for (int c = count[v]; c > 0; c--) out[k++] = v + min;
        }
        return out;
    }

    /**
     * Stable counting sort of objects by an integer key in [0, k). Uses prefix
     * sums to compute each key's starting slot, then places items left to right.
     */
    @SuppressWarnings("unchecked")
    static <T> T[] countingSortBy(T[] a, int k, ToIntFunction<? super T> key) {
        int[] start = new int[k + 1];
        for (T x : a) {
            int kk = key.applyAsInt(x);
            if (kk < 0 || kk >= k) throw new IllegalArgumentException("key out of range: " + kk);
            start[kk + 1]++;
        }
        for (int i = 0; i < k; i++) start[i + 1] += start[i];
        T[] out = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), a.length);
        for (T x : a) out[start[key.applyAsInt(x)]++] = x;
        return out;
    }

    /**
     * LSD radix sort on 32-bit signed ints, one byte (base 256) per pass, so 4
     * stable counting-sort passes. Flipping the sign bit maps signed order onto
     * unsigned order, which makes negative numbers sort correctly.
     */
    static void radixSort(int[] a) {
        int n = a.length;
        int[] buf = new int[n];
        int[] src = a, dst = buf;
        for (int shift = 0; shift < 32; shift += 8) {
            int[] count = new int[257];
            for (int x : src) count[digit(x, shift) + 1]++;
            for (int i = 0; i < 256; i++) count[i + 1] += count[i];
            for (int x : src) dst[count[digit(x, shift)]++] = x;
            int[] t = src;
            src = dst;
            dst = t;
        }
        // After an even number of passes, the result is back in `a`.
    }

    private static int digit(int x, int shift) {
        return ((x ^ Integer.MIN_VALUE) >>> shift) & 0xFF;
    }

    /** LSD radix sort for equal-length strings (e.g. license plates, fixed-width IDs). */
    static void radixSortStrings(String[] a, int width) {
        String[] buf = new String[a.length];
        for (int pos = width - 1; pos >= 0; pos--) {
            int[] count = new int[257];
            for (String s : a) count[s.charAt(pos) + 1]++;
            for (int i = 0; i < 256; i++) count[i + 1] += count[i];
            for (String s : a) buf[count[s.charAt(pos)]++] = s;
            System.arraycopy(buf, 0, a, 0, a.length);
        }
    }
}
