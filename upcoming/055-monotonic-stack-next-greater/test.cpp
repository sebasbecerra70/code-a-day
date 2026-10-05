#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <random>

using V = std::vector<int>;

static V bruteNextGreater(const V& a, bool circular) {
    const std::size_t n = a.size();
    V ans(n, -1);
    for (std::size_t i = 0; i < n; ++i) {
        std::size_t limit = circular ? n - 1 : n - 1 - i;
        for (std::size_t d = 1; d <= limit; ++d) {
            int v = a[(i + d) % n];
            if (v > a[i]) { ans[i] = v; break; }
        }
    }
    return ans;
}

static void test_examples() {
    assert((nextGreater({4, 5, 2, 25}) == V{5, 25, 25, -1}));
    assert((nextGreater({13, 7, 6, 12}) == V{-1, 12, 12, -1}));
}

static void test_edge_cases() {
    assert(nextGreater({}).empty());
    assert((nextGreater({1}) == V{-1}));
    assert((nextGreater({5, 4, 3}) == V{-1, -1, -1}));       // strictly decreasing
    assert((nextGreater({1, 2, 3}) == V{2, 3, -1}));          // strictly increasing
    assert((nextGreater({2, 2, 2}) == V{-1, -1, -1}));        // equal is not greater
}

static void test_circular() {
    assert((nextGreaterCircular({1, 2, 1}) == V{2, -1, 2}));
    assert((nextGreaterCircular({1, 2, 3, 4, 3}) == V{2, 3, 4, -1, 4}));
    assert(nextGreaterCircular({}).empty());
}

static void test_random_against_brute() {
    std::mt19937 rng(6);
    for (int t = 0; t < 500; ++t) {
        V a(rng() % 15);
        for (auto& x : a) x = static_cast<int>(rng() % 10);
        assert(nextGreater(a) == bruteNextGreater(a, false));
        assert(nextGreaterCircular(a) == bruteNextGreater(a, true));
    }
}

static void test_daily_temperatures() {
    assert((dailyTemperatures({73, 74, 75, 71, 69, 72, 76, 73}) == V{1, 1, 4, 2, 1, 1, 0, 0}));
    assert((dailyTemperatures({30, 60, 90}) == V{1, 1, 0}));
}

static void test_stock_spanner() {
    StockSpanner s;
    V got;
    for (int p : {100, 80, 60, 70, 60, 75, 85}) got.push_back(s.next(p));
    assert((got == V{1, 1, 1, 2, 1, 4, 6}));
}

static void test_stock_spanner_random() {
    std::mt19937 rng(9);
    StockSpanner s;
    V prices;
    for (int t = 0; t < 1000; ++t) {
        int p = static_cast<int>(rng() % 50);
        prices.push_back(p);
        int expected = 0;
        for (std::size_t i = prices.size(); i-- > 0 && prices[i] <= p;) ++expected;
        assert(s.next(p) == expected);
    }
}

int main() {
    void (*tests[])() = {test_examples, test_edge_cases, test_circular, test_random_against_brute,
                         test_daily_temperatures, test_stock_spanner, test_stock_spanner_random};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
