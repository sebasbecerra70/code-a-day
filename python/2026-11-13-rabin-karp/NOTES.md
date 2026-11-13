# Rabin-Karp rolling-hash search

**Problem:** Find all occurrences of a pattern in a text by comparing hashes of sliding windows, and extend the idea to many patterns at once and to "longest repeated substring".

## Approach
- Treat a string as a base-256 number mod a large prime: `h(s) = Σ s[i]·B^(m-1-i) mod P`.
- **Rolling update** when the window slides one step: remove the leading char's contribution (`- s[i]·B^(m-1)`), shift (`·B`), add the new char. O(1) per step.
- Only when hashes match do we compare the actual substrings, so collisions cost time but never correctness.
- `P = 2^61 - 1` (Mersenne prime) makes collisions extremely unlikely; Python ints don't overflow.
- **Multi-pattern:** group patterns by length; for each length roll once over the text and look the window hash up in a dict.
- **Longest repeated substring:** "a repeat of length L exists" is monotone in L, so binary search L and test each with a rolling-hash set.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| find_all | O(n + m) expected, O(n·m) worst (all collisions) | O(1) |
| find_any | O(n · #distinct lengths + total pattern length) expected | O(#patterns) |
| longest_repeated_substring | O(n log n) expected | O(n) |

## Interview talking points
- Choosing modulus/base: large prime modulus, base ≥ alphabet size; randomize the base to defeat adversarial inputs (anti-hash tests).
- Double hashing (two moduli) as an alternative to verification.
- Rabin-Karp shines for multiple patterns of the same length and 2-D pattern matching; KMP/Z are better single-pattern worst cases.
- Same rolling-hash idea: rsync/content-defined chunking, plagiarism detection (winnowing), duplicate-substring problems.

## Run
From this folder: `python -m pytest -q`
