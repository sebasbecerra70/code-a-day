# Big integer arithmetic (base 10^9 limbs)

**Problem:** Implement a signed arbitrary-precision integer with parsing from and printing to decimal strings, comparison, `+`, `-`, `*`, and truncating `/` and `%`.

## Approach
- **Sign-magnitude** representation: a `neg` flag plus a little-endian vector of base-10⁹ "limbs". Base 10⁹ is the largest power of ten whose limb product (< 10¹⁸) fits in 64 bits, and parsing/printing is just splitting into 9-digit chunks.
- Invariant: no leading zero limbs, and zero is never negative. `trim()` restores it after every operation.
- **Add/subtract** reduce to magnitude helpers: same sign means add magnitudes; different signs means subtract the smaller magnitude from the larger and take the larger one's sign.
- **Multiply:** schoolbook O(n·m) with a 64-bit accumulator and carry propagation.
- **Divide:** long division, one limb at a time. Shift the remainder up by one limb, then **binary search** the next quotient limb in [0, 10⁹).

## Complexity
(n, m = number of limbs)

| Operation | Time | Space |
|-----------|------|-------|
| parse / print | O(n) | O(n) |
| compare | O(n) | O(1) |
| add / subtract | O(max(n, m)) | O(max(n, m)) |
| multiply | O(n·m) | O(n + m) |
| divide | O(n · m · log BASE) | O(n + m) |

## Interview talking points
- Base choice: base 2³² is faster for arithmetic (and what GMP uses), but base 10⁹ makes decimal I/O trivial.
- Faster multiplication: Karatsuba is O(n^1.585); FFT/NTT-based methods are O(n log n). They pay off only past a few hundred limbs.
- Knuth's Algorithm D estimates each quotient limb from the top two limbs, so it needs no binary search.
- Negating `LLONG_MIN` overflows; convert to unsigned before negating.
- Division semantics differ by language: C++ and Java truncate toward zero, Python floors.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
