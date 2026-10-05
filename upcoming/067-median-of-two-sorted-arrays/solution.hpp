#pragma once
// Median of two sorted arrays in O(log(min(m, n))) by binary searching a partition,
// plus the more general k-th smallest element.

#include <algorithm>
#include <climits>
#include <stdexcept>
#include <vector>

// Choose i elements from a and j = half - i from b so that every element on the left
// is <= every element on the right. Then the median is at the partition boundary.
inline double findMedianSortedArrays(const std::vector<int>& a, const std::vector<int>& b) {
    if (a.size() > b.size()) return findMedianSortedArrays(b, a);  // search the shorter one
    const int m = static_cast<int>(a.size()), n = static_cast<int>(b.size());
    if (m + n == 0) throw std::invalid_argument("both arrays empty");
    const int half = (m + n + 1) / 2;  // left side gets the extra element when total is odd
    int lo = 0, hi = m;
    while (lo <= hi) {
        int i = lo + (hi - lo) / 2;
        int j = half - i;
        // Sentinels for empty sides; long long so they don't collide with INT_MIN/INT_MAX data.
        long long aLeft = i > 0 ? a[i - 1] : LLONG_MIN, aRight = i < m ? a[i] : LLONG_MAX;
        long long bLeft = j > 0 ? b[j - 1] : LLONG_MIN, bRight = j < n ? b[j] : LLONG_MAX;
        if (aLeft > bRight) {
            hi = i - 1;  // took too many from a
        } else if (bLeft > aRight) {
            lo = i + 1;  // took too few from a
        } else {
            long long leftMax = std::max(aLeft, bLeft);
            if ((m + n) % 2 == 1) return static_cast<double>(leftMax);
            long long rightMin = std::min(aRight, bRight);
            return (static_cast<double>(leftMax) + static_cast<double>(rightMin)) / 2.0;
        }
    }
    throw std::logic_error("inputs are not sorted");
}

// k-th smallest (1-based) of the union: discard k/2 elements from one array each step.
inline int kthSmallest(const std::vector<int>& a, const std::vector<int>& b, int k) {
    if (k < 1 || k > static_cast<int>(a.size() + b.size())) throw std::out_of_range("k out of range");
    std::size_t ia = 0, ib = 0;
    while (true) {
        if (ia == a.size()) return b[ib + k - 1];
        if (ib == b.size()) return a[ia + k - 1];
        if (k == 1) return std::min(a[ia], b[ib]);
        std::size_t step = static_cast<std::size_t>(k / 2);
        std::size_t na = std::min(ia + step, a.size()) - 1;
        std::size_t nb = std::min(ib + step, b.size()) - 1;
        // The smaller probe and everything before it can't be the k-th.
        if (a[na] <= b[nb]) {
            k -= static_cast<int>(na - ia + 1);
            ia = na + 1;
        } else {
            k -= static_cast<int>(nb - ib + 1);
            ib = nb + 1;
        }
    }
}
