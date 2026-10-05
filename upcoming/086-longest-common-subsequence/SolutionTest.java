import java.util.Random;

public class SolutionTest {
    private static int passed = 0;

    interface TestBody {
        void run() throws Exception;
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void expectThrows(Class<? extends Throwable> type, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + t, t);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " to be thrown");
    }

    static void run(String name, TestBody body) {
        try {
            body.run();
        } catch (Throwable t) {
            throw new AssertionError(name + " failed: " + t.getMessage(), t);
        }
        passed++;
    }

    // Exponential reference: try every subsequence of the shorter string.
    static int bruteForce(String a, String b) {
        if (a.length() > b.length()) return bruteForce(b, a);
        int best = 0;
        for (int mask = 0; mask < (1 << a.length()); mask++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < a.length(); i++) if ((mask & (1 << i)) != 0) sb.append(a.charAt(i));
            if (sb.length() > best && Lcs.isSubsequence(sb.toString(), b)) best = sb.length();
        }
        return best;
    }

    static String randomString(Random rnd, int maxLen, int alphabet) {
        StringBuilder sb = new StringBuilder();
        for (int i = rnd.nextInt(maxLen + 1); i > 0; i--) sb.append((char) ('a' + rnd.nextInt(alphabet)));
        return sb.toString();
    }

    public static void main(String[] args) {
        run("emptyStrings", () -> {
            check(Lcs.length("", "abc") == 0 && Lcs.length("", "") == 0, "length");
            check(Lcs.sequence("abc", "").isEmpty(), "sequence");
        });

        run("classicExample", () -> {
            check(Lcs.length("ABCBDAB", "BDCABA") == 4, "length 4");
            String s = Lcs.sequence("ABCBDAB", "BDCABA");
            check(s.length() == 4 && Lcs.isSubsequence(s, "ABCBDAB") && Lcs.isSubsequence(s, "BDCABA"), s);
        });

        run("identicalAndDisjoint", () -> {
            check(Lcs.sequence("hello", "hello").equals("hello"), "identical");
            check(Lcs.length("abc", "xyz") == 0, "disjoint");
        });

        run("oneIsSubsequenceOfOther", () -> {
            check(Lcs.sequence("ace", "abcde").equals("ace"), "ace");
            check(Lcs.length("abcde", "ace") == 3, "symmetric");
        });

        run("derivedMetrics", () -> {
            check(Lcs.shortestCommonSupersequenceLength("abac", "cab") == 5, "SCS 'cabac'");
            check(Lcs.insertDeleteDistance("sea", "eat") == 2, "delete s, insert t");
        });

        run("isSubsequence", () -> {
            check(Lcs.isSubsequence("", "x") && Lcs.isSubsequence("abc", "aXbYc"), "yes");
            check(!Lcs.isSubsequence("acb", "abc") && !Lcs.isSubsequence("a", ""), "no");
        });

        run("longInputsLinearMemory", () -> {
            String a = "ab".repeat(2500), b = "ba".repeat(2500);
            check(Lcs.length(a, b) == 4999, "length on 5000-char strings");
        });

        run("randomizedVsBruteForce", () -> {
            Random rnd = new Random(86);
            for (int t = 0; t < 400; t++) {
                String a = randomString(rnd, 10, 3), b = randomString(rnd, 12, 3);
                int expected = bruteForce(a, b);
                check(Lcs.length(a, b) == expected && Lcs.table(a, b)[a.length()][b.length()] == expected, a + "/" + b);
                String s = Lcs.sequence(a, b);
                check(s.length() == expected && Lcs.isSubsequence(s, a) && Lcs.isSubsequence(s, b), "valid LCS " + s);
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
