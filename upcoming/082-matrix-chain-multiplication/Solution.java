/**
 * Matrix i has shape dims[i] x dims[i+1]. Find the parenthesization that
 * minimizes scalar multiplications. Interval DP over chain length.
 */
class MatrixChain {
    final long[][] cost;  // cost[i][j] = min multiplications for matrices i..j
    final int[][] split;  // split[i][j] = k where the optimal top-level split is (i..k)(k+1..j)
    private final int n;

    MatrixChain(int[] dims) {
        if (dims.length < 2) throw new IllegalArgumentException("need at least one matrix");
        for (int d : dims) if (d <= 0) throw new IllegalArgumentException("dimensions must be positive");
        n = dims.length - 1;
        cost = new long[n][n];
        split = new int[n][n];
        // Solve all intervals of length 2, then 3, ...: each depends only on shorter ones.
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                cost[i][j] = Long.MAX_VALUE;
                for (int k = i; k < j; k++) {
                    long c = cost[i][k] + cost[k + 1][j] + (long) dims[i] * dims[k + 1] * dims[j + 1];
                    if (c < cost[i][j]) {
                        cost[i][j] = c;
                        split[i][j] = k;
                    }
                }
            }
        }
    }

    long minCost() {
        return cost[0][n - 1];
    }

    /** Optimal parenthesization, e.g. "((A1A2)A3)". Matrices are named A1..An. */
    String parenthesization() {
        StringBuilder sb = new StringBuilder();
        build(0, n - 1, sb);
        return sb.toString();
    }

    private void build(int i, int j, StringBuilder sb) {
        if (i == j) {
            sb.append('A').append(i + 1);
            return;
        }
        sb.append('(');
        build(i, split[i][j], sb);
        build(split[i][j] + 1, j, sb);
        sb.append(')');
    }

    /** Cost of evaluating a given parenthesization string (for verification). */
    static long costOf(String expr, int[] dims) {
        int[] pos = {0};
        long[] total = {0};
        evalShape(expr, pos, dims, total);
        return total[0];
    }

    // Returns {rows, cols} of the sub-expression starting at pos[0].
    private static int[] evalShape(String s, int[] pos, int[] dims, long[] total) {
        if (s.charAt(pos[0]) == 'A') {
            pos[0]++;
            int start = pos[0];
            while (pos[0] < s.length() && Character.isDigit(s.charAt(pos[0]))) pos[0]++;
            int idx = Integer.parseInt(s.substring(start, pos[0])) - 1;
            return new int[] {dims[idx], dims[idx + 1]};
        }
        pos[0]++; // '('
        int[] left = evalShape(s, pos, dims, total);
        int[] right = evalShape(s, pos, dims, total);
        pos[0]++; // ')'
        if (left[1] != right[0]) throw new IllegalArgumentException("shape mismatch");
        total[0] += (long) left[0] * left[1] * right[1];
        return new int[] {left[0], right[1]};
    }

    /** Exponential brute force over all parenthesizations (Catalan many), for testing. */
    static long bruteForce(int[] dims, int i, int j) {
        if (i == j) return 0;
        long best = Long.MAX_VALUE;
        for (int k = i; k < j; k++) {
            best = Math.min(best, bruteForce(dims, i, k) + bruteForce(dims, k + 1, j)
                    + (long) dims[i] * dims[k + 1] * dims[j + 1]);
        }
        return best;
    }
}
