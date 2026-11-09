# KMP string search (prefix function)

**Problem:** Find every occurrence of a pattern in a text in linear time, without the O(n·m) worst case of naive matching (e.g. `"aaaa...ab"` in `"aaaa...a"`).

## Approach
- **Prefix function** `pi[i]`: length of the longest proper prefix of `pattern[:i+1]` that is also a suffix of it (a "border").
- Build `pi` by extending the previous border `k`; on mismatch, fall back to `pi[k-1]`, the next shorter border, until one extends or `k` hits 0.
- **Search** runs the same automaton over the text: `k` is how many pattern characters currently match. On mismatch, fall back via `pi` instead of moving the text pointer backward. When `k == m`, record a match and fall back to `pi[m-1]` so overlapping matches are found.
- Bonus: the smallest period of `s` is `len(s) - pi[-1]`.

## Complexity
| Step | Time | Space |
|------|------|-------|
| prefix_function | O(m) | O(m) |
| find_all | O(n + m) | O(m) |

## Interview talking points
- Why linear? `k` increases by at most 1 per character and every fallback decreases it, so total fallbacks ≤ total increments (amortized argument).
- The text pointer never moves backward, so KMP works on streams.
- Alternatives: Z-algorithm (same power, different array), Rabin-Karp (hashing, easy multi-pattern), Boyer-Moore (sublinear on average, used by `grep`), Aho-Corasick (many patterns at once, KMP generalized to a trie).
- Other uses of `pi`: repeated-substring pattern, shortest palindrome (`s + "#" + reverse(s)`), counting prefix occurrences.

## Run
From this folder: `python -m pytest -q`
