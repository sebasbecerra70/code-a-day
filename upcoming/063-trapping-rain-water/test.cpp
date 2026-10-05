#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <random>

static long long bruteTrap(const std::vector<int>& h) {
    long long w = 0;
    for (std::size_t i = 0; i < h.size(); ++i) {
        int lm = 0, rm = 0;
        for (std::size_t j = 0; j <= i; ++j) lm = std::max(lm, h[j]);
        for (std::size_t j = i; j < h.size(); ++j) rm = std::max(rm, h[j]);
        w += std::min(lm, rm) - h[i];
    }
    return w;
}

static void check1D(const std::vector<int>& h, long long expected) {
    assert(trapTwoPointers(h) == expected);
    assert(trapStack(h) == expected);
}

static void test_classic_examples() {
    check1D({0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}, 6);
    check1D({4, 2, 0, 3, 2, 5}, 9);
}

static void test_no_water() {
    check1D({}, 0);
    check1D({5}, 0);
    check1D({1, 2}, 0);
    check1D({1, 2, 3, 4}, 0);
    check1D({4, 3, 2, 1}, 0);
    check1D({3, 3, 3}, 0);
}

static void test_single_basin() {
    check1D({3, 0, 3}, 3);
    check1D({5, 0, 0, 0, 2}, 6);  // bounded by the shorter wall
}

static void test_random_against_brute() {
    std::mt19937 rng(21);
    for (int t = 0; t < 1000; ++t) {
        std::vector<int> h(rng() % 25);
        for (auto& x : h) x = static_cast<int>(rng() % 10);
        long long expected = bruteTrap(h);
        check1D(h, expected);
    }
}

static void test_2d_example() {
    std::vector<std::vector<int>> g = {{1, 4, 3, 1, 3, 2}, {3, 2, 1, 3, 2, 4}, {2, 3, 3, 2, 3, 1}};
    assert(trap2D(g) == 4);
    std::vector<std::vector<int>> g2 = {{3, 3, 3, 3, 3}, {3, 2, 2, 2, 3}, {3, 2, 1, 2, 3},
                                        {3, 2, 2, 2, 3}, {3, 3, 3, 3, 3}};
    assert(trap2D(g2) == 10);
}

static void test_2d_leak_and_small() {
    assert(trap2D({{5, 5, 5}, {5, 1, 5}, {5, 5, 5}}) == 4);
    assert(trap2D({{5, 5, 5}, {5, 1, 0}, {5, 5, 5}}) == 0);  // leaks through the border
    assert(trap2D({{1, 2}, {3, 4}}) == 0);
    assert(trap2D({}) == 0);
}

static void test_2d_walled_row_matches_1d() {
    // A 3-row grid whose middle row equals a 1D profile walled on top/bottom by tall bars
    // must trap the same as the 1D profile (with the ends acting as walls).
    std::mt19937 rng(5);
    for (int t = 0; t < 200; ++t) {
        std::size_t n = rng() % 10 + 3;
        std::vector<int> mid(n);
        for (auto& x : mid) x = static_cast<int>(rng() % 10);
        std::vector<std::vector<int>> g = {std::vector<int>(n, 100), mid, std::vector<int>(n, 100)};
        assert(trap2D(g) == trapTwoPointers(mid));
    }
}

int main() {
    void (*tests[])() = {test_classic_examples, test_no_water, test_single_basin,
                         test_random_against_brute, test_2d_example, test_2d_leak_and_small,
                         test_2d_walled_row_matches_1d};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
