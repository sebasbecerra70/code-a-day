#include "solution.hpp"

#include <cassert>
#include <climits>
#include <iostream>
#include <random>

using V = std::vector<int>;

static double bruteMedian(V a, const V& b) {
    a.insert(a.end(), b.begin(), b.end());
    std::sort(a.begin(), a.end());
    std::size_t n = a.size();
    return n % 2 ? a[n / 2] : (static_cast<double>(a[n / 2 - 1]) + a[n / 2]) / 2.0;
}

static void test_examples() {
    assert(findMedianSortedArrays({1, 3}, {2}) == 2.0);
    assert(findMedianSortedArrays({1, 2}, {3, 4}) == 2.5);
}

static void test_one_empty() {
    assert(findMedianSortedArrays({}, {1}) == 1.0);
    assert(findMedianSortedArrays({2, 3}, {}) == 2.5);
    bool threw = false;
    try { findMedianSortedArrays({}, {}); } catch (const std::invalid_argument&) { threw = true; }
    assert(threw);
}

static void test_disjoint_ranges() {
    assert(findMedianSortedArrays({1, 2, 3}, {10, 11, 12}) == 6.5);
    assert(findMedianSortedArrays({10, 11, 12}, {1, 2}) == 10.0);
}

static void test_duplicates_and_negatives() {
    assert(findMedianSortedArrays({1, 1, 1}, {1, 1}) == 1.0);
    assert(findMedianSortedArrays({-5, -3}, {-4}) == -4.0);
}

static void test_extreme_values() {
    assert(findMedianSortedArrays({INT_MIN}, {INT_MAX}) == -0.5);
    assert(findMedianSortedArrays({INT_MAX, INT_MAX}, {INT_MAX}) == static_cast<double>(INT_MAX));
}

static void test_random_against_sort() {
    std::mt19937 rng(31);
    for (int t = 0; t < 3000; ++t) {
        V a(rng() % 10), b(rng() % 10);
        if (a.empty() && b.empty()) continue;
        for (auto& x : a) x = static_cast<int>(rng() % 50) - 25;
        for (auto& x : b) x = static_cast<int>(rng() % 50) - 25;
        std::sort(a.begin(), a.end());
        std::sort(b.begin(), b.end());
        assert(findMedianSortedArrays(a, b) == bruteMedian(a, b));
    }
}

static void test_kth_smallest() {
    V a{2, 3, 6, 7, 9}, b{1, 4, 8, 10};
    V all{1, 2, 3, 4, 6, 7, 8, 9, 10};
    for (int k = 1; k <= 9; ++k) assert(kthSmallest(a, b, k) == all[k - 1]);
    bool threw = false;
    try { kthSmallest(a, b, 10); } catch (const std::out_of_range&) { threw = true; }
    assert(threw);
}

static void test_kth_random() {
    std::mt19937 rng(32);
    for (int t = 0; t < 1000; ++t) {
        V a(rng() % 8), b(rng() % 8);
        for (auto& x : a) x = static_cast<int>(rng() % 20);
        for (auto& x : b) x = static_cast<int>(rng() % 20);
        std::sort(a.begin(), a.end());
        std::sort(b.begin(), b.end());
        V all = a;
        all.insert(all.end(), b.begin(), b.end());
        std::sort(all.begin(), all.end());
        for (int k = 1; k <= static_cast<int>(all.size()); ++k) assert(kthSmallest(a, b, k) == all[k - 1]);
    }
}

int main() {
    void (*tests[])() = {test_examples, test_one_empty, test_disjoint_ranges,
                         test_duplicates_and_negatives, test_extreme_values,
                         test_random_against_sort, test_kth_smallest, test_kth_random};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
