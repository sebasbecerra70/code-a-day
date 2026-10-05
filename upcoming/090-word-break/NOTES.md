# Word break (DP + all segmentations)

**Problem:** Given a string and a dictionary, decide whether the string can be split into a sequence of dictionary words. Variants: the fewest words, the number of segmentations, and the list of all segmentations.

## Approach
- **Feasibility:** `ok[i]` is true if `s[0..i)` can be segmented. `ok[i] = OR over j of (ok[j] && dict ∋ s[j..i))`. Only `j ≥ i − maxWordLen` can match, which bounds the inner loop by the longest dictionary word.
- **Fewest words / count:** the same recurrence with `min(best[j] + 1)` or `ways[i] += ways[j]`.
- **All segmentations:** top-down recursion over the start index, memoizing the list of sentences for each suffix. First run the boolean DP and return early if it fails. Without that check, inputs like `"aaaa…ab"` explore an exponential number of dead ends.
- Empty dictionary words are ignored (otherwise the recursion never advances).
- Tests compare all four answers against plain backtracking on random small inputs.

## Complexity
| Variant | Time | Space |
|---------|------|-------|
| canSegment / minWords / count | O(n · L · L) for max word length L (substring + hash) | O(n) |
| allSegmentations | O(n · L² + output size) | O(output) |
| Naive backtracking | O(2ⁿ) | O(n) |

## Interview talking points
- The classic follow-up trap: "return all sentences" can produce exponential output, so say so up front and prune with the boolean DP.
- Bounding by `maxWordLen` turns O(n²) substring checks into O(n·L). A **trie** walk from each start index avoids building substrings at all, and can stop as soon as no dictionary word has that prefix.
- BFS over indices is an equivalent view: nodes are positions, edges are dictionary words, and the question is whether n is reachable from 0.
- Real uses: tokenizing languages without spaces (Chinese, Thai), hashtag and domain-name splitting, and spell checking. Production segmenters add word frequencies and pick the most probable split with a Viterbi-style max-product DP.
- Counts overflow quickly (all-`a` strings follow a Fibonacci-like recurrence), so use `long` or count modulo a prime.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
