# Z-algorithm (linear-time pattern matching)

**Problem:** For a string `s`, compute `z[i]` = the length of the longest substring starting at `i` that is also a prefix of `s`, for every `i`, in O(n). Use it to find all occurrences of a pattern in a text and the smallest period of a string.

## Approach
- Maintain the **Z-box** `[l, r)`: the match window (a substring equal to a prefix of `s`) that reaches furthest right.
- For position `i` inside the box, `s[i..r)` equals `s[i−l..r−l)`, so `z[i]` is at least `min(r − i, z[i − l])`.
- Then extend `z[i]` by direct character comparison, and update the box if it now reaches past `r`.
- Every successful comparison moves `r` right, and `r` never moves left, so the total work is O(n).
- **Pattern search:** compute the Z-array of `pattern + text`. Position `i` in the text part is a match when `z[i] ≥ |pattern|`. The usual version puts a separator `$` between them; checking `z[i] ≥ |pattern|` makes it unnecessary, so we never need a character that appears in neither string.
- **Period:** `p` is a period that tiles `s` exactly when `p` divides `n` and `p + z[p] = n`.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| zFunction | O(n) | O(n) |
| findAll | O(n + m) | O(n + m) |
| smallestPeriod | O(n) | O(n) |

## Interview talking points
- Z and KMP's prefix function carry the same information and convert to each other in O(n); many people find Z easier to derive and explain.
- Why linear: the inner `while` loop only runs while extending past `r`, and `r` only grows.
- `z[0]` is a convention: either `n` or 0, so state which one you use.
- Other uses: count distinct substrings (O(n²) overall), string compression, and checking whether a string is a rotation of another (search in `a + a`).
- Rabin-Karp is an alternative with hashing: simpler for multiple patterns, but probabilistic.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
