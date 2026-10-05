#pragma once
// Kadane's algorithm and variants: with indices, circular arrays, and maximum product.

#include <algorithm>
#include <cstddef>
#include <stdexcept>
#include <vector>

struct SubarrayResult {
    long long sum;
    std::size_t begin, end;  // half-open [begin, end), non-empty
};

// Best non-empty subarray. Ties keep the earliest-ending, then shortest, subarray.
inline SubarrayResult maxSubarray(const std::vector<int>& a) {
    if (a.empty()) throw std::invalid_argument("empty array");
    SubarrayResult best{a[0], 0, 1};
    long long cur = 0;
    std::size_t curBegin = 0;
    for (std::size_t i = 0; i < a.size(); ++i) {
        // A negative running sum can only hurt what follows, so restart here.
        if (cur <= 0) {
            cur = 0;
            curBegin = i;
        }
        cur += a[i];
        if (cur > best.sum) best = {cur, curBegin, i + 1};
    }
    return best;
}

// The wrap-around answer is total - (minimum subarray). If every element is negative,
// that would pick the empty array, so fall back to the normal maximum.
inline long long maxSubarrayCircular(const std::vector<int>& a) {
    if (a.empty()) throw std::invalid_argument("empty array");
    long long total = 0, curMax = 0, curMin = 0;
    long long bestMax = a[0], bestMin = a[0];
    for (int x : a) {
        total += x;
        curMax = std::max<long long>(curMax + x, x);
        bestMax = std::max(bestMax, curMax);
        curMin = std::min<long long>(curMin + x, x);
        bestMin = std::min(bestMin, curMin);
    }
    return bestMax < 0 ? bestMax : std::max(bestMax, total - bestMin);
}

// Track both the max and min product ending here: a negative number swaps them.
inline long long maxProductSubarray(const std::vector<int>& a) {
    if (a.empty()) throw std::invalid_argument("empty array");
    long long hi = a[0], lo = a[0], best = a[0];
    for (std::size_t i = 1; i < a.size(); ++i) {
        long long x = a[i];
        if (x < 0) std::swap(hi, lo);
        hi = std::max(x, hi * x);
        lo = std::min(x, lo * x);
        best = std::max(best, hi);
    }
    return best;
}
