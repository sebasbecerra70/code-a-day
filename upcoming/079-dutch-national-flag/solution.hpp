#pragma once
// Dijkstra's three-way partition, and two applications: sort colors (0/1/2)
// and a quicksort that handles many duplicate keys efficiently.

#include <cstddef>
#include <functional>
#include <random>
#include <utility>
#include <vector>

// Rearranges a[lo, hi) into  [< pivot | == pivot | > pivot].
// Returns {lt, gt} so the equal block is [lt, gt).
template <typename T, typename Less = std::less<T>>
std::pair<std::size_t, std::size_t> threeWayPartition(std::vector<T>& a, std::size_t lo, std::size_t hi,
                                                      const T& pivot, Less less = Less()) {
    // Invariant: [lo, lt) < pivot, [lt, i) == pivot, [i, gt) unknown, [gt, hi) > pivot.
    std::size_t lt = lo, i = lo, gt = hi;
    while (i < gt) {
        if (less(a[i], pivot)) {
            std::swap(a[lt++], a[i++]);
        } else if (less(pivot, a[i])) {
            std::swap(a[i], a[--gt]);  // don't advance i: the swapped-in element is unexamined
        } else {
            ++i;
        }
    }
    return {lt, gt};
}

// LeetCode "Sort Colors": one pass, constant space.
inline void sortColors(std::vector<int>& colors) {
    threeWayPartition(colors, 0, colors.size(), 1);
}

namespace detail {
template <typename T, typename Less>
void quicksort3(std::vector<T>& a, std::size_t lo, std::size_t hi, std::mt19937& rng, Less less) {
    while (hi - lo > 1) {
        std::uniform_int_distribution<std::size_t> pick(lo, hi - 1);
        T pivot = a[pick(rng)];
        auto [lt, gt] = threeWayPartition(a, lo, hi, pivot, less);
        // Recurse into the smaller side and loop on the larger: O(log n) stack depth.
        if (lt - lo < hi - gt) {
            quicksort3(a, lo, lt, rng, less);
            lo = gt;
        } else {
            quicksort3(a, gt, hi, rng, less);
            hi = lt;
        }
    }
}
}  // namespace detail

// Quicksort with three-way partitioning: equal keys are finished in one step,
// so an array with few distinct values sorts in O(n * distinct) instead of O(n^2).
template <typename T, typename Less = std::less<T>>
void quicksort3Way(std::vector<T>& a, Less less = Less()) {
    std::mt19937 rng(12345);
    detail::quicksort3(a, 0, a.size(), rng, less);
}
