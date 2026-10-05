# Rotate an array in place (reversal and cycle methods)

**Problem:** Rotate an array to the right by `k` steps (`[1,2,3,4,5,6,7]`, `k = 3` gives `[5,6,7,1,2,3,4]`) using O(1) extra space. Bonus: rotate an `n × n` matrix 90° clockwise in place.

## Approach
- Normalize `k %= n` first; `k` may exceed `n`.
- **Reversal trick:** reverse the whole array, then reverse the first `k` elements and the remaining `n − k`. Rotating right by `k` means the last `k` elements move to the front: reversing everything puts them in front (backwards), and the two partial reversals fix the order inside each block.
- **Cycle (juggling) method:** the element at `i` belongs at `(i + k) mod n`. Following that mapping from a start index forms a cycle of length `n / gcd(n, k)`, and there are `gcd(n, k)` such cycles, starting at `0 .. gcd − 1`. Each element is moved exactly once.
- **Matrix:** clockwise rotation = transpose, then reverse each row.

## Complexity
| Method | Time | Space |
|--------|------|-------|
| reversal | O(n), about 2n swaps | O(1) |
| cycles | O(n), exactly n moves | O(1) |
| copy to new array (baseline) | O(n) | O(n) |
| matrix rotate | O(n²) | O(1) |

## Interview talking points
- Forgetting `k %= n` is the usual bug (and with `n == 0`, `k % n` divides by zero).
- Why are there `gcd(n, k)` cycles? The positions reachable from `i` are `i + multiples of gcd(n, k)` modulo `n`.
- The cycle method makes the fewest writes, which matters for expensive-to-move elements; the reversal method is more cache friendly.
- `std::rotate` does this for you (it's a left rotation around a middle iterator).
- Counter-clockwise matrix rotation: transpose then reverse each column, or reverse rows first then transpose.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
