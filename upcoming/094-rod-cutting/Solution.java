import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Best revenue and the piece lengths that achieve it. */
record RodPlan(long revenue, List<Integer> pieces) {}

/** Throughout, price[i] is the value of a piece of length i + 1. */
class RodCutting {
    /**
     * Bottom-up: best[len] = max over first-piece length k of price(k) + best[len - k].
     * Recording the best first piece for each length lets us rebuild the cuts.
     */
    static RodPlan solve(int[] price, int n) {
        return solve(price, n, 0);
    }

    /** Variant where every cut costs {@code cutCost}; selling the rod whole costs nothing. */
    static RodPlan solve(int[] price, int n, int cutCost) {
        if (n < 0) throw new IllegalArgumentException("negative length");
        if (price.length == 0) throw new IllegalArgumentException("price list must be non-empty");
        long[] best = new long[n + 1];
        int[] firstPiece = new int[n + 1];
        for (int len = 1; len <= n; len++) {
            best[len] = Long.MIN_VALUE;
            for (int k = 1; k <= Math.min(len, price.length); k++) {
                // Cutting off k leaves len - k; that's one more cut unless nothing remains.
                long v = price[k - 1] + best[len - k] - (k < len ? cutCost : 0);
                if (v > best[len]) {
                    best[len] = v;
                    firstPiece[len] = k;
                }
            }
        }
        List<Integer> pieces = new ArrayList<>();
        for (int len = n; len > 0; len -= firstPiece[len]) pieces.add(firstPiece[len]);
        return new RodPlan(best[n], pieces);
    }

    /** Top-down memoized version of the same recurrence (no cut cost). */
    static long solveMemo(int[] price, int n) {
        long[] memo = new long[n + 1];
        Arrays.fill(memo, -1);
        return memo(price, n, memo);
    }

    private static long memo(int[] price, int len, long[] memo) {
        if (len == 0) return 0;
        if (memo[len] >= 0) return memo[len];
        long best = Long.MIN_VALUE;
        for (int k = 1; k <= Math.min(len, price.length); k++) best = Math.max(best, price[k - 1] + memo(price, len - k, memo));
        return memo[len] = best;
    }

    /** Brute force over all 2^(n-1) cut patterns, for testing. */
    static long bruteForce(int[] price, int n, int cutCost) {
        if (n == 0) return 0;
        long best = Long.MIN_VALUE;
        for (int mask = 0; mask < (1 << (n - 1)); mask++) {
            long total = 0;
            int start = 0;
            boolean valid = true;
            for (int pos = 1; pos <= n; pos++) {
                if (pos == n || (mask & (1 << (pos - 1))) != 0) {
                    int piece = pos - start;
                    if (piece > price.length) {
                        valid = false;
                        break;
                    }
                    total += price[piece - 1];
                    if (pos < n) total -= cutCost;
                    start = pos;
                }
            }
            if (valid) best = Math.max(best, total);
        }
        return best;
    }
}
