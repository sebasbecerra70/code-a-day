class Lcs {
    /** Full DP table: dp[i][j] = LCS length of a[0..i) and b[0..j). */
    static int[][] table(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1] + 1
                        : Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        return dp;
    }

    /** Length only, using two rows: O(min(n, m)) memory. */
    static int length(String a, String b) {
        if (b.length() > a.length()) {
            String t = a;
            a = b;
            b = t;
        }
        int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                cur[j] = a.charAt(i - 1) == b.charAt(j - 1) ? prev[j - 1] + 1 : Math.max(prev[j], cur[j - 1]);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }

    /** One LCS string, rebuilt by walking the table back from the bottom-right corner. */
    static String sequence(String a, String b) {
        int[][] dp = table(a, b);
        StringBuilder sb = new StringBuilder();
        int i = a.length(), j = b.length();
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                sb.append(a.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        return sb.reverse().toString();
    }

    /** Shortest common supersequence length follows directly: n + m - LCS. */
    static int shortestCommonSupersequenceLength(String a, String b) {
        return a.length() + b.length() - length(a, b);
    }

    /** Minimum deletions + insertions to turn a into b (edit distance with no substitutions). */
    static int insertDeleteDistance(String a, String b) {
        return a.length() + b.length() - 2 * length(a, b);
    }

    /** True if s is a subsequence of t (two pointers). */
    static boolean isSubsequence(String s, String t) {
        int i = 0;
        for (int j = 0; j < t.length() && i < s.length(); j++) if (s.charAt(i) == t.charAt(j)) i++;
        return i == s.length();
    }
}
