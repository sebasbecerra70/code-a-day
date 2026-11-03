# Counting sort and LSD radix sort

**Problem:** Sort integers (and records keyed by small integers or fixed-width strings) in linear time by using the structure of the keys instead of comparisons.

## Approach
- **Counting sort (values):** shift by `min`, count occurrences, then write each value `count` times. O(n + k) for a key range k. It refuses ranges above 10⁸ to avoid a huge allocation.
- **Counting sort (records):** count keys, take a prefix sum to get each key's first output slot, then place items in input order. Placing in input order is what makes it **stable**.
- **LSD radix sort (ints):** four stable counting-sort passes on bytes, least significant first. Stability means that after pass d, the numbers are sorted by their low d bytes. XOR with `Integer.MIN_VALUE` flips the sign bit so negatives sort before positives. Ping-ponging between two buffers for an even number of passes leaves the result in the original array.
- **LSD radix sort (strings):** the same idea, one character position at a time from right to left, for equal-length keys.

## Complexity
| Algorithm | Time | Space | Stable |
|-----------|------|-------|--------|
| Counting sort | O(n + k) | O(n + k) | Yes (record version) |
| LSD radix, d digits, base b | O(d · (n + b)) | O(n + b) | Yes |
| 32-bit ints, base 256 | 4 passes ≈ O(n) | O(n) | Yes |

## Interview talking points
- The Ω(n log n) lower bound only applies to *comparison* sorts. These algorithms read the key digits directly, so the bound doesn't apply to them.
- Radix sort needs a **stable** inner sort; otherwise later passes scramble the order earlier passes established.
- Choosing the base trades the number of passes against the count-array size: base 256 fits in L1 cache and takes 4 passes for 32-bit ints, while base 65536 takes 2 passes but uses a 256 KB count array.
- MSD radix sort processes the most significant digit first and recurses into buckets. It handles variable-length strings and can stop early, but it needs care with small buckets.
- Real uses: suffix array construction, sorting fixed-width IDs, GPU sorts and database bucketing.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
