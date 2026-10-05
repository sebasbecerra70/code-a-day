#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <random>

static long long bruteHistogram(const std::vector<int>& h) {
    long long best = 0;
    for (std::size_t i = 0; i < h.size(); ++i) {
        int mn = h[i];
        for (std::size_t j = i; j < h.size(); ++j) {
            mn = std::min(mn, h[j]);
            best = std::max(best, static_cast<long long>(mn) * static_cast<long long>(j - i + 1));
        }
    }
    return best;
}

static void test_classic_example() {
    assert(largestRectangleArea({2, 1, 5, 6, 2, 3}) == 10);
    assert(largestRectangleArea({2, 4}) == 4);
}

static void test_edge_cases() {
    assert(largestRectangleArea({}) == 0);
    assert(largestRectangleArea({0, 0}) == 0);
    assert(largestRectangleArea({7}) == 7);
    assert(largestRectangleArea({3, 3, 3, 3}) == 12);  // duplicates
}

static void test_monotonic_inputs() {
    assert(largestRectangleArea({1, 2, 3, 4, 5}) == 9);
    assert(largestRectangleArea({5, 4, 3, 2, 1}) == 9);
}

static void test_large_values_no_overflow() {
    std::vector<int> h(100000, 1'000'000'000);
    assert(largestRectangleArea(h) == 100'000'000'000'000LL);
}

static void test_random_against_brute() {
    std::mt19937 rng(12);
    for (int t = 0; t < 500; ++t) {
        std::vector<int> h(rng() % 20);
        for (auto& x : h) x = static_cast<int>(rng() % 8);
        assert(largestRectangleArea(h) == bruteHistogram(h));
    }
}

static void test_maximal_rectangle() {
    std::vector<std::string> g = {"10100", "10111", "11111", "10010"};
    assert(maximalRectangle(g) == 6);
    assert(maximalRectangle({}) == 0);
    assert(maximalRectangle({"0"}) == 0);
    assert(maximalRectangle({"1"}) == 1);
}

static void test_maximal_rectangle_random() {
    std::mt19937 rng(13);
    for (int t = 0; t < 100; ++t) {
        std::size_t R = rng() % 6 + 1, C = rng() % 6 + 1;
        std::vector<std::string> g(R, std::string(C, '0'));
        for (auto& row : g)
            for (auto& ch : row) ch = rng() % 4 ? '1' : '0';
        long long best = 0;
        for (std::size_t r1 = 0; r1 < R; ++r1)
            for (std::size_t r2 = r1; r2 < R; ++r2)
                for (std::size_t c1 = 0; c1 < C; ++c1)
                    for (std::size_t c2 = c1; c2 < C; ++c2) {
                        bool ok = true;
                        for (std::size_t r = r1; r <= r2 && ok; ++r)
                            for (std::size_t c = c1; c <= c2 && ok; ++c) ok = g[r][c] == '1';
                        if (ok) best = std::max(best, static_cast<long long>((r2 - r1 + 1) * (c2 - c1 + 1)));
                    }
        assert(maximalRectangle(g) == best);
    }
}

int main() {
    void (*tests[])() = {test_classic_example, test_edge_cases, test_monotonic_inputs,
                         test_large_values_no_overflow, test_random_against_brute,
                         test_maximal_rectangle, test_maximal_rectangle_random};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
