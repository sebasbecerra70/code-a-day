#include "solution.hpp"

#include <algorithm>
#include <cassert>
#include <iostream>
#include <string>

using V = std::vector<int>;

static void test_sort_colors_example() {
    V c{2, 0, 2, 1, 1, 0};
    sortColors(c);
    assert((c == V{0, 0, 1, 1, 2, 2}));
}

static void test_sort_colors_edge_cases() {
    V empty;
    sortColors(empty);
    assert(empty.empty());
    V one{2};
    sortColors(one);
    assert(one == V{2});
    V same{1, 1, 1};
    sortColors(same);
    assert((same == V{1, 1, 1}));
    V noOnes{2, 0, 2, 0};
    sortColors(noOnes);
    assert((noOnes == V{0, 0, 2, 2}));
}

static void test_sort_colors_random() {
    std::mt19937 rng(61);
    for (int t = 0; t < 1000; ++t) {
        V c(rng() % 30);
        for (auto& x : c) x = static_cast<int>(rng() % 3);
        V expected = c;
        std::sort(expected.begin(), expected.end());
        sortColors(c);
        assert(c == expected);
    }
}

static void test_partition_invariant_and_bounds() {
    std::mt19937 rng(62);
    for (int t = 0; t < 1000; ++t) {
        V a(rng() % 25);
        for (auto& x : a) x = static_cast<int>(rng() % 6);
        int pivot = static_cast<int>(rng() % 6);
        V before = a;
        auto [lt, gt] = threeWayPartition(a, 0, a.size(), pivot);
        for (std::size_t i = 0; i < a.size(); ++i) {
            if (i < lt) assert(a[i] < pivot);
            else if (i < gt) assert(a[i] == pivot);
            else assert(a[i] > pivot);
        }
        std::sort(before.begin(), before.end());
        V after = a;
        std::sort(after.begin(), after.end());
        assert(before == after);  // same multiset
    }
}

static void test_partition_subrange_only() {
    V a{9, 3, 1, 2, 3, 0, 9};
    auto [lt, gt] = threeWayPartition(a, 1, 6, 2);
    assert(a.front() == 9 && a.back() == 9);  // outside the range untouched
    assert(lt == 3 && gt == 4 && a[3] == 2);
}

static void test_quicksort_random_and_duplicates() {
    std::mt19937 rng(63);
    for (int t = 0; t < 300; ++t) {
        V a(rng() % 200);
        int range = static_cast<int>(rng() % 10) + 1;
        for (auto& x : a) x = static_cast<int>(rng() % range);
        V expected = a;
        std::sort(expected.begin(), expected.end());
        quicksort3Way(a);
        assert(a == expected);
    }
}

static void test_quicksort_all_equal_large() {
    V a(200000, 7);  // classic 2-way quicksort degrades badly here
    quicksort3Way(a);
    assert(std::all_of(a.begin(), a.end(), [](int x) { return x == 7; }));
}

static void test_quicksort_custom_comparator() {
    std::vector<std::string> s{"pear", "fig", "apple", "kiwi", "banana"};
    quicksort3Way(s, [](const std::string& x, const std::string& y) { return x.size() < y.size(); });
    for (std::size_t i = 1; i < s.size(); ++i) assert(s[i - 1].size() <= s[i].size());
}

int main() {
    void (*tests[])() = {test_sort_colors_example, test_sort_colors_edge_cases,
                         test_sort_colors_random, test_partition_invariant_and_bounds,
                         test_partition_subrange_only, test_quicksort_random_and_duplicates,
                         test_quicksort_all_equal_large, test_quicksort_custom_comparator};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
