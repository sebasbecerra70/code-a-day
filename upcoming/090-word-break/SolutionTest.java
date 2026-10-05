import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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

    // Plain backtracking with no memo, as a reference.
    static void brute(String s, int start, Set<String> dict, String acc, List<String> out) {
        if (start == s.length()) {
            out.add(acc);
            return;
        }
        for (int end = start + 1; end <= s.length(); end++) {
            String w = s.substring(start, end);
            if (dict.contains(w)) brute(s, end, dict, acc.isEmpty() ? w : acc + " " + w, out);
        }
    }

    public static void main(String[] args) {
        run("emptyString", () -> {
            WordBreak wb = new WordBreak(List.of("a"));
            check(wb.canSegment("") && wb.minWords("") == 0 && wb.countSegmentations("") == 1, "empty is trivially ok");
            check(wb.allSegmentations("").equals(List.of("")), "one empty sentence");
        });

        run("classicYes", () -> {
            WordBreak wb = new WordBreak(List.of("leet", "code"));
            check(wb.canSegment("leetcode") && wb.minWords("leetcode") == 2, "leet code");
            check(new WordBreak(List.of("apple", "pen")).canSegment("applepenapple"), "reuse words");
        });

        run("classicNo", () -> {
            WordBreak wb = new WordBreak(List.of("cats", "dog", "sand", "and", "cat"));
            check(!wb.canSegment("catsandog") && wb.minWords("catsandog") == -1, "no segmentation");
            check(wb.allSegmentations("catsandog").isEmpty() && wb.countSegmentations("catsandog") == 0, "none");
        });

        run("allSegmentations", () -> {
            WordBreak wb = new WordBreak(List.of("cat", "cats", "and", "sand", "dog"));
            List<String> got = wb.allSegmentations("catsanddog");
            check(new HashSet<>(got).equals(Set.of("cats and dog", "cat sand dog")), got.toString());
            check(wb.countSegmentations("catsanddog") == 2, "count");
        });

        run("minWordsPrefersLongWords", () -> {
            WordBreak wb = new WordBreak(List.of("a", "aa", "aaa", "aaaa"));
            check(wb.minWords("aaaaaaaaaa") == 3, "4+4+2 or 4+3+3");
        });

        run("ignoresEmptyWord", () -> {
            WordBreak wb = new WordBreak(List.of("", "x"));
            check(!wb.canSegment("y") && wb.canSegment("xx"), "empty word ignored");
        });

        run("pathologicalInputStaysFast", () -> {
            // Exponential for naive backtracking: many ways to split the a's, then a dead end.
            WordBreak wb = new WordBreak(List.of("a", "aa", "aaa", "aaaa", "aaaaa"));
            String s = "a".repeat(200) + "b";
            check(!wb.canSegment(s), "dead end");
            check(wb.allSegmentations(s).isEmpty(), "pruned instantly");
            check(wb.countSegmentations("a".repeat(30)) == 345_052_351L, "compositions of 30 into parts 1..5");
        });

        run("randomizedVsBacktracking", () -> {
            Random rnd = new Random(90);
            for (int t = 0; t < 300; t++) {
                Set<String> dict = new HashSet<>();
                for (int i = rnd.nextInt(6); i >= 0; i--) {
                    StringBuilder w = new StringBuilder();
                    for (int k = 1 + rnd.nextInt(3); k > 0; k--) w.append((char) ('a' + rnd.nextInt(2)));
                    dict.add(w.toString());
                }
                StringBuilder s = new StringBuilder();
                for (int k = rnd.nextInt(12); k > 0; k--) s.append((char) ('a' + rnd.nextInt(2)));
                WordBreak wb = new WordBreak(dict);
                List<String> expected = new ArrayList<>();
                brute(s.toString(), 0, dict, "", expected);
                if (s.length() == 0) expected = List.of("");
                List<String> got = wb.allSegmentations(s.toString());
                check(new HashSet<>(got).equals(new HashSet<>(expected)) && got.size() == expected.size(), "all");
                check(wb.canSegment(s.toString()) == !expected.isEmpty(), "canSegment");
                check(wb.countSegmentations(s.toString()) == expected.size(), "count");
                int minW = expected.stream().mapToInt(x -> x.isEmpty() ? 0 : x.split(" ").length).min().orElse(-1);
                check(wb.minWords(s.toString()) == minW, "minWords");
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
