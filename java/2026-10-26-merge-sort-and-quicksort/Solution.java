import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;

class Sorts {
    private static final int INSERTION_CUTOFF = 16;

    // ---------- Merge sort: stable, O(n log n) always, O(n) extra space ----------

    /** Stable top-down merge sort. A single scratch buffer is allocated once. */
    @SuppressWarnings("unchecked")
    static <T> void mergeSort(T[] a, Comparator<? super T> cmp) {
        T[] buf = (T[]) new Object[a.length];
        mergeSort(a, buf, 0, a.length, cmp);
    }

    private static <T> void mergeSort(T[] a, T[] buf, int lo, int hi, Comparator<? super T> cmp) {
        if (hi - lo <= INSERTION_CUTOFF) {
            insertionSort(a, lo, hi, cmp);
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSort(a, buf, lo, mid, cmp);
        mergeSort(a, buf, mid, hi, cmp);
        // Already in order: skip the merge (makes sorted input O(n)).
        if (cmp.compare(a[mid - 1], a[mid]) <= 0) return;
        merge(a, buf, lo, mid, hi, cmp);
    }

    private static <T> void merge(T[] a, T[] buf, int lo, int mid, int hi, Comparator<? super T> cmp) {
        System.arraycopy(a, lo, buf, lo, hi - lo);
        int i = lo, j = mid, k = lo;
        while (i < mid && j < hi) {
            // "<=" takes from the left run on ties, which is what makes it stable.
            a[k++] = cmp.compare(buf[i], buf[j]) <= 0 ? buf[i++] : buf[j++];
        }
        while (i < mid) a[k++] = buf[i++];
        while (j < hi) a[k++] = buf[j++];
    }

    private static <T> void insertionSort(T[] a, int lo, int hi, Comparator<? super T> cmp) {
        for (int i = lo + 1; i < hi; i++) {
            T x = a[i];
            int j = i - 1;
            while (j >= lo && cmp.compare(a[j], x) > 0) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = x;
        }
    }

    /** Bottom-up merge sort on ints: no recursion, merges runs of width 1, 2, 4, ... */
    static void mergeSortBottomUp(int[] a) {
        int n = a.length;
        int[] src = a, dst = new int[n];
        for (int width = 1; width < n; width *= 2) {
            for (int lo = 0; lo < n; lo += 2 * width) {
                int mid = Math.min(lo + width, n), hi = Math.min(lo + 2 * width, n);
                int i = lo, j = mid, k = lo;
                while (i < mid && j < hi) dst[k++] = src[i] <= src[j] ? src[i++] : src[j++];
                while (i < mid) dst[k++] = src[i++];
                while (j < hi) dst[k++] = src[j++];
            }
            int[] t = src;
            src = dst;
            dst = t;
        }
        if (src != a) System.arraycopy(src, 0, a, 0, n);
    }

    // ---------- Quicksort: in-place, O(n log n) expected, not stable ----------

    /**
     * Randomized quicksort with 3-way (Dutch flag) partitioning, so arrays with
     * many duplicates stay O(n log n). Recurses on the smaller side to bound the
     * stack at O(log n).
     */
    static void quickSort(int[] a) {
        quickSort(a, 0, a.length - 1);
    }

    private static void quickSort(int[] a, int lo, int hi) {
        while (lo < hi) {
            if (hi - lo < INSERTION_CUTOFF) {
                insertionSort(a, lo, hi);
                return;
            }
            int pivot = a[ThreadLocalRandom.current().nextInt(lo, hi + 1)];
            // Invariant: a[lo..lt-1] < pivot, a[lt..i-1] == pivot, a[gt+1..hi] > pivot.
            int lt = lo, i = lo, gt = hi;
            while (i <= gt) {
                if (a[i] < pivot) swap(a, lt++, i++);
                else if (a[i] > pivot) swap(a, i, gt--);
                else i++;
            }
            if (lt - lo < hi - gt) {
                quickSort(a, lo, lt - 1);
                lo = gt + 1;
            } else {
                quickSort(a, gt + 1, hi);
                hi = lt - 1;
            }
        }
    }

    /** Lomuto partition around a[hi]; returns the pivot's final index. Shown for comparison. */
    static int lomutoPartition(int[] a, int lo, int hi) {
        int pivot = a[hi], store = lo;
        for (int i = lo; i < hi; i++) {
            if (a[i] < pivot) swap(a, store++, i);
        }
        swap(a, store, hi);
        return store;
    }

    private static void insertionSort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int x = a[i], j = i - 1;
            while (j >= lo && a[j] > x) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = x;
        }
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
