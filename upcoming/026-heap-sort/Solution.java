import java.util.Comparator;

/**
 * In-place heap sort. Phase 1 turns the array into a max-heap in O(n) with
 * bottom-up heapify; phase 2 repeatedly swaps the max to the end and sifts down.
 */
class HeapSort {
    static void sort(int[] a) {
        int n = a.length;
        // Every index >= n/2 is a leaf, so start from the last internal node.
        for (int i = n / 2 - 1; i >= 0; i--) siftDown(a, i, n);
        for (int end = n - 1; end > 0; end--) {
            swap(a, 0, end);
            siftDown(a, 0, end);
        }
    }

    /** Restores the max-heap property for the subtree at i, considering only a[0..size). */
    static void siftDown(int[] a, int i, int size) {
        int x = a[i];
        while (true) {
            int child = 2 * i + 1;
            if (child >= size) break;
            if (child + 1 < size && a[child + 1] > a[child]) child++;
            if (a[child] <= x) break;
            a[i] = a[child]; // move the hole down instead of swapping each level
            i = child;
        }
        a[i] = x;
    }

    /** Generic version with a comparator; the array is sorted ascending under {@code cmp}. */
    static <T> void sort(T[] a, Comparator<? super T> cmp) {
        int n = a.length;
        for (int i = n / 2 - 1; i >= 0; i--) siftDown(a, i, n, cmp);
        for (int end = n - 1; end > 0; end--) {
            T t = a[0];
            a[0] = a[end];
            a[end] = t;
            siftDown(a, 0, end, cmp);
        }
    }

    private static <T> void siftDown(T[] a, int i, int size, Comparator<? super T> cmp) {
        T x = a[i];
        while (true) {
            int child = 2 * i + 1;
            if (child >= size) break;
            if (child + 1 < size && cmp.compare(a[child + 1], a[child]) > 0) child++;
            if (cmp.compare(a[child], x) <= 0) break;
            a[i] = a[child];
            i = child;
        }
        a[i] = x;
    }

    /** True if a[0..n) satisfies the max-heap property. */
    static boolean isMaxHeap(int[] a, int n) {
        for (int i = 1; i < n; i++) if (a[(i - 1) / 2] < a[i]) return false;
        return true;
    }

    /** Partial heap sort: the k largest values in descending order, in O(n + k log n). */
    static int[] topK(int[] input, int k) {
        int[] a = input.clone();
        int n = a.length;
        k = Math.min(k, n);
        for (int i = n / 2 - 1; i >= 0; i--) siftDown(a, i, n);
        int[] out = new int[k];
        for (int j = 0; j < k; j++) {
            out[j] = a[0];
            a[0] = a[n - 1 - j];
            siftDown(a, 0, n - 1 - j);
        }
        return out;
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
