import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class WordBreak {
    private final Set<String> dict;
    private final int maxWordLen;

    WordBreak(Collection<String> words) {
        dict = new HashSet<>();
        int max = 0;
        for (String w : words) {
            if (w.isEmpty()) continue; // the empty word would make every string trivially segmentable
            dict.add(w);
            max = Math.max(max, w.length());
        }
        maxWordLen = max;
    }

    /**
     * ok[i] = true if s[0..i) can be segmented. Only the last maxWordLen
     * positions need checking, which bounds the inner loop by the longest word.
     */
    boolean canSegment(String s) {
        int n = s.length();
        boolean[] ok = new boolean[n + 1];
        ok[0] = true;
        for (int i = 1; i <= n; i++) {
            for (int j = Math.max(0, i - maxWordLen); j < i && !ok[i]; j++) {
                ok[i] = ok[j] && dict.contains(s.substring(j, i));
            }
        }
        return ok[n];
    }

    /** Fewest words in a segmentation, or -1 if none exists. */
    int minWords(String s) {
        int n = s.length();
        int[] best = new int[n + 1];
        java.util.Arrays.fill(best, Integer.MAX_VALUE);
        best[0] = 0;
        for (int i = 1; i <= n; i++) {
            for (int j = Math.max(0, i - maxWordLen); j < i; j++) {
                if (best[j] != Integer.MAX_VALUE && best[j] + 1 < best[i] && dict.contains(s.substring(j, i))) {
                    best[i] = best[j] + 1;
                }
            }
        }
        return best[n] == Integer.MAX_VALUE ? -1 : best[n];
    }

    /** Number of distinct segmentations (can be exponential, so it's a long). */
    long countSegmentations(String s) {
        int n = s.length();
        long[] ways = new long[n + 1];
        ways[0] = 1;
        for (int i = 1; i <= n; i++) {
            for (int j = Math.max(0, i - maxWordLen); j < i; j++) {
                if (ways[j] != 0 && dict.contains(s.substring(j, i))) ways[i] += ways[j];
            }
        }
        return ways[n];
    }

    /**
     * Every segmentation as a space-joined sentence. Memoized by start index;
     * output size can be exponential, so use this only when that's acceptable.
     */
    List<String> allSegmentations(String s) {
        if (!canSegment(s)) return List.of(); // prune: avoids exploring hopeless suffixes
        return suffixes(s, 0, new HashMap<>());
    }

    private List<String> suffixes(String s, int start, Map<Integer, List<String>> memo) {
        if (start == s.length()) return List.of("");
        List<String> cached = memo.get(start);
        if (cached != null) return cached;
        List<String> out = new ArrayList<>();
        for (int end = start + 1; end <= Math.min(s.length(), start + maxWordLen); end++) {
            String word = s.substring(start, end);
            if (!dict.contains(word)) continue;
            for (String rest : suffixes(s, end, memo)) out.add(rest.isEmpty() ? word : word + " " + rest);
        }
        memo.put(start, out);
        return out;
    }
}
