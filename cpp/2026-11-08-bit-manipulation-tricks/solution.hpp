#pragma once
// A toolbox of classic bit tricks on unsigned 32/64-bit integers, each written by hand
// (no compiler builtins) so the idea is visible.

#include <cstdint>
#include <utility>
#include <vector>

namespace bits {

inline bool isPowerOfTwo(std::uint64_t x) { return x && !(x & (x - 1)); }

// Brian Kernighan: x & (x - 1) clears the lowest set bit, so loop once per set bit.
inline int popcount(std::uint64_t x) {
    int n = 0;
    for (; x; x &= x - 1) ++n;
    return n;
}

inline std::uint64_t lowestSetBit(std::uint64_t x) { return x & (~x + 1); }  // x & -x

// Number of trailing zeros, by binary search on halves. Returns 64 for x == 0.
inline int countTrailingZeros(std::uint64_t x) {
    if (x == 0) return 64;
    int n = 0;
    for (int shift = 32; shift > 0; shift >>= 1) {
        std::uint64_t mask = (std::uint64_t{1} << shift) - 1;
        if ((x & mask) == 0) { n += shift; x >>= shift; }
    }
    return n;
}

// Smallest power of two >= x (x >= 1): smear the top bit right, then add one.
inline std::uint64_t nextPowerOfTwo(std::uint64_t x) {
    if (x <= 1) return 1;
    --x;
    for (int s = 1; s < 64; s <<= 1) x |= x >> s;
    return x + 1;
}

// Reverse 32 bits with divide-and-conquer swaps: halves, bytes, nibbles, pairs, bits.
inline std::uint32_t reverseBits(std::uint32_t x) {
    x = (x >> 16) | (x << 16);
    x = ((x & 0xFF00FF00u) >> 8) | ((x & 0x00FF00FFu) << 8);
    x = ((x & 0xF0F0F0F0u) >> 4) | ((x & 0x0F0F0F0Fu) << 4);
    x = ((x & 0xCCCCCCCCu) >> 2) | ((x & 0x33333333u) << 2);
    x = ((x & 0xAAAAAAAAu) >> 1) | ((x & 0x55555555u) << 1);
    return x;
}

// Every value appears twice except one: XOR cancels the pairs.
inline int singleNumber(const std::vector<int>& v) {
    int x = 0;
    for (int a : v) x ^= a;
    return x;
}

// Every value appears twice except a and b: split on any bit where a and b differ.
inline std::pair<int, int> twoSingleNumbers(const std::vector<int>& v) {
    unsigned all = 0;
    for (int a : v) all ^= static_cast<unsigned>(a);
    unsigned diff = all & (~all + 1);
    unsigned a = 0;
    for (int x : v)
        if (static_cast<unsigned>(x) & diff) a ^= static_cast<unsigned>(x);
    int first = static_cast<int>(a), second = static_cast<int>(a ^ all);
    return first < second ? std::pair{first, second} : std::pair{second, first};
}

// Gray code: consecutive values differ in exactly one bit.
inline std::uint32_t toGray(std::uint32_t x) { return x ^ (x >> 1); }
inline std::uint32_t fromGray(std::uint32_t g) {
    for (std::uint32_t s = 1; s < 32; s <<= 1) g ^= g >> s;
    return g;
}

// Next larger integer with the same number of set bits (Gosper's hack). x must be nonzero.
inline std::uint64_t nextSamePopcount(std::uint64_t x) {
    std::uint64_t low = x & (~x + 1);
    std::uint64_t ripple = x + low;
    return ripple | (((x ^ ripple) >> 2) / low);
}

// All subsets of `mask`, in decreasing order, including 0.
inline std::vector<std::uint32_t> submasks(std::uint32_t mask) {
    std::vector<std::uint32_t> out;
    for (std::uint32_t s = mask;; s = (s - 1) & mask) {
        out.push_back(s);
        if (s == 0) break;
    }
    return out;
}

// Add without '+': XOR is the sum without carries, AND<<1 is the carries.
inline std::uint32_t addWithoutPlus(std::uint32_t a, std::uint32_t b) {
    while (b) {
        std::uint32_t carry = (a & b) << 1;
        a ^= b;
        b = carry;
    }
    return a;
}

}  // namespace bits
