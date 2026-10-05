# Suffix array (prefix doubling) with LCP array

**Problem:** Sort all suffixes of a string, returning their start indices (the suffix array), and compute the LCP array: the longest common prefix of each pair of adjacent suffixes in sorted order. Use them to count distinct substrings and find the longest repeated substring.

## Approach
- Append a sentinel `'\0'` smaller than every character and sort **cyclic shifts**; with the sentinel, shift order equals suffix order.
- **Prefix doubling:** after round `k` every shift is ranked by its first `2ᵏ` characters. The first `2ᵏ⁺¹` characters of shift `i` are the pair `(rank[i], rank[i + 2ᵏ])`, so one stable sort of pairs doubles the length.
- Two tricks make each round O(n): the order by the *second* key is just the previous order shifted left by `2ᵏ`, and the stable sort by the *first* key is a counting sort over rank values.
- Stop early once all ranks are distinct.
- **Kasai's LCP:** process suffixes in text order. If suffix `i` shares `h` characters with its sorted neighbor, suffix `i+1` shares at least `h − 1` with its own neighbor, so `h` drops by at most 1 per step: O(n) total.
- Distinct substrings = `n(n+1)/2 − Σ lcp`. Longest repeated substring = the maximum LCP entry.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| suffixArray | O(n log n) | O(n + alphabet) |
| lcpArray (Kasai) | O(n) | O(n) |
| countDistinctSubstrings / longestRepeatedSubstring | O(n log n) | O(n) |
| naive sort of suffixes (comparison) | O(n² log n) | O(n) |

## Interview talking points
- A suffix array is a space-efficient alternative to a suffix tree; with LCP + RMQ it answers most of the same queries.
- Substring search: binary search the pattern over the suffix array in O(m log n).
- SA-IS and DC3 build it in O(n), but prefix doubling is far easier to write correctly.
- Cast `char` to `unsigned char` before using it as a counting-sort index, or bytes ≥ 0x80 go negative.
- Applications: bioinformatics (genome indexing), compression (Burrows-Wheeler transform is read straight off the suffix array), plagiarism detection, longest common substring of two strings (concatenate with a separator).

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
