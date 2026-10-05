#pragma once
// Rotate an array right by k positions using O(1) extra space, two ways:
//   reversal trick: reverse all, then reverse each part
//   cycle (juggling): follow gcd(n, k) independent cycles, moving each element once
// Plus rotating an n x n matrix 90 degrees clockwise in place.

#include <cstddef>
#include <numeric>
#include <stdexcept>
#include <utility>
#include <vector>

template <typename T>
void reverseRange(std::vector<T>& a, std::size_t lo, std::size_t hi) {  // [lo, hi)
    while (lo + 1 < hi) std::swap(a[lo++], a[--hi]);
}

template <typename T>
void rotateRightReversal(std::vector<T>& a, std::size_t k) {
    const std::size_t n = a.size();
    if (n == 0) return;
    k %= n;
    reverseRange(a, 0, n);  // [1 2 3 4 5], k=2 -> [5 4 3 2 1]
    reverseRange(a, 0, k);  // -> [4 5 3 2 1]
    reverseRange(a, k, n);  // -> [4 5 1 2 3]
}

template <typename T>
void rotateRightCycles(std::vector<T>& a, std::size_t k) {
    const std::size_t n = a.size();
    if (n == 0) return;
    k %= n;
    if (k == 0) return;
    // Index i's element belongs at (i + k) % n. There are gcd(n, k) disjoint cycles.
    const std::size_t cycles = std::gcd(n, k);
    for (std::size_t start = 0; start < cycles; ++start) {
        T carried = std::move(a[start]);
        std::size_t cur = start;
        do {
            std::size_t next = (cur + k) % n;
            std::swap(carried, a[next]);
            cur = next;
        } while (cur != start);
    }
}

// Clockwise 90 degrees = transpose, then reverse every row.
template <typename T>
void rotateMatrix90(std::vector<std::vector<T>>& m) {
    const std::size_t n = m.size();
    for (const auto& row : m)
        if (row.size() != n) throw std::invalid_argument("matrix must be square");
    for (std::size_t i = 0; i < n; ++i)
        for (std::size_t j = i + 1; j < n; ++j) std::swap(m[i][j], m[j][i]);
    for (auto& row : m) reverseRange(row, 0, n);
}
