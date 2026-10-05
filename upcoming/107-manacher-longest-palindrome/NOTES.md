# Manacher's algorithm (longest palindromic substring)

**Problem:** Find the longest palindromic substring of a string in O(n). The same computation also counts all palindromic substrings.

## Approach
- For each center compute a radius: `d1[i]` for odd-length palindromes centered at `i`, and `d2[i]` for even-length ones centered between `i−1` and `i`.
- Keep the **rightmost palindrome** found so far, `[l, r]`. For a center `i` inside it, the mirror center `j = l + r − i` already has a known radius, and by symmetry `i`'s radius is at least `min(radius[j], r − i + 1)`.
- Then extend by direct comparison and update `[l, r]` if the new palindrome reaches further right.
- `r` only moves right, so total extension work is O(n).
- Longest palindrome: the best `2·d1[i] − 1` or `2·d2[i]`. Count of palindromic substrings: `Σ d1[i] + d2[i]` (a radius of `k` means `k` nested palindromes at that center).

## Complexity
| Function | Time | Space |
|----------|------|-------|
| manacher | O(n) | O(n) |
| longestPalindrome | O(n) | O(n) |
| countPalindromicSubstrings | O(n) | O(n) |
| expand-around-center (comparison) | O(n²) | O(1) |

## Interview talking points
- Start with expand-around-center (O(n²), O(1) space): it's the expected answer in most interviews; Manacher is the follow-up.
- The common trick of inserting `#` between characters (`a#b#a`) merges odd and even cases into one loop; separate `d1`/`d2` arrays avoid the extra memory and index mapping.
- Correctness of the mirror step: inside `[l, r]`, the text around `i` is the reverse of the text around `j`, so palindromes at `j` that stay inside the window also exist at `i`.
- The DP solution (`isPal[i][j]`) is O(n²) time and space and is mostly useful when you need all palindrome ranges for a later DP (e.g. palindrome partitioning).

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
