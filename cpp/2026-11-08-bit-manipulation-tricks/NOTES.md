# Bit manipulation tricks

**Problem:** Implement a set of classic bit tricks by hand: power-of-two test and round-up, popcount, trailing zeros, lowest set bit, bit reversal, finding unpaired numbers with XOR, Gray code, Gosper's hack, submask enumeration, and addition without `+`.

## Approach
| Trick | Key identity |
|-------|--------------|
| power of two | `x && !(x & (x-1))`: a power of two has exactly one set bit |
| popcount (Kernighan) | `x &= x-1` clears the lowest set bit, so the loop runs once per set bit |
| lowest set bit | `x & -x`, written `x & (~x + 1)` for unsigned |
| trailing zeros | binary search: test the low 32, 16, 8, 4, 2, 1 bits |
| next power of two | `x-1`, smear the top bit right with `x |= x >> s`, add 1 |
| reverse bits | swap halves, then bytes, nibbles, pairs, single bits with masks |
| single number | XOR of everything; pairs cancel because `a ^ a = 0` |
| two single numbers | XOR gives `a ^ b`; any set bit of it splits the input into two groups |
| Gray code | `g = x ^ (x >> 1)`; invert with a prefix XOR |
| Gosper's hack | next larger number with the same popcount |
| submasks | `s = (s - 1) & mask` walks every subset of `mask` |
| add without `+` | XOR is the carry-less sum, `(a & b) << 1` the carries; repeat |

## Complexity
| Function | Time | Space |
|----------|------|-------|
| isPowerOfTwo, lowestSetBit, toGray, nextSamePopcount | O(1) | O(1) |
| popcount | O(number of set bits) | O(1) |
| countTrailingZeros, nextPowerOfTwo, reverseBits, fromGray | O(log w) | O(1) |
| singleNumber / twoSingleNumbers | O(n) | O(1) |
| submasks | O(2^popcount(mask)) | output |
| addWithoutPlus | O(w) worst | O(1) |

## Interview talking points
- Use unsigned types: right-shifting negative signed values is implementation-defined before C++20, and overflow is undefined.
- In production, prefer `std::popcount`, `std::countr_zero`, `std::bit_ceil` (C++20 `<bit>`), which compile to single instructions (`POPCNT`, `TZCNT`).
- Submask enumeration over all masks costs O(3ⁿ) total, the basis of many bitmask DP solutions.
- Gray codes are used in rotary encoders and Karnaugh maps, because only one bit changes per step.
- `x & -x` is the core of Fenwick (binary indexed) trees.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
