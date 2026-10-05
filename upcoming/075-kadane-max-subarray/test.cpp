#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <random>

using V = std::vector<int>;

static long long bruteMax(const V& a) {
    long long best = a[0];
    for (std::size_t i = 0; i < a.size(); ++i) {
        long long s = 0;
        for (std::size_t j = i; j < a.size(); ++j) best = std::max(best, s += a[j]);
    }
    return best;
}

static long long bruteCircular(const V& a) {
    const std::size_t n = a.size();
    long long best = a[0];
    for (std::size_t i = 0; i < n; ++i) {
        long long s = 0;
        for (std::size_t len = 1; len <= n; ++len) best = std::max(best, s += a[(i + len - 1) % n]);
    }
    return best;
}

static long long bruteProduct(const V& a) {
    long long best = a[0];
    for (std::size_t i = 0; i < a.size(); ++i) {
        long long p = 1;
        for (std::size_t j = i; j < a.size(); ++j) best = std::max(best, p *= a[j]);
    }
    return best;
}

static void test_classic() {
    auto r = maxSubarray({-2, 1, -3, 4, -1, 2, 1, -5, 4});
    assert(r.sum == 6 && r.begin == 3 && r.end == 7);
}

static void test_all_negative() {
    auto r = maxSubarray({-3, -1, -2});
    assert(r.sum == -1 && r.begin == 1 && r.end == 2);
}

static void test_single_and_empty() {
    auto r = maxSubarray({5});
    assert(r.sum == 5 && r.begin == 0 && r.end == 1);
    bool threw = false;
    try { maxSubarray({}); } catch (const std::invalid_argument&) { threw = true; }
    assert(threw);
}

static void test_indices_sum_matches() {
    std::mt19937 rng(51);
    for (int t = 0; t < 2000; ++t) {
        V a(rng() % 15 + 1);
        for (auto& x : a) x = static_cast<int>(rng() % 21) - 10;
        auto r = maxSubarray(a);
        assert(r.sum == bruteMax(a));
        long long s = 0;
        for (std::size_t i = r.begin; i < r.end; ++i) s += a[i];
        assert(s == r.sum && r.begin < r.end);
    }
}

static void test_circular() {
    assert(maxSubarrayCircular({5, -3, 5}) == 10);
    assert(maxSubarrayCircular({1, -2, 3, -2}) == 3);
    assert(maxSubarrayCircular({-3, -2, -3}) == -2);
}

static void test_circular_random() {
    std::mt19937 rng(52);
    for (int t = 0; t < 2000; ++t) {
        V a(rng() % 12 + 1);
        for (auto& x : a) x = static_cast<int>(rng() % 21) - 10;
        assert(maxSubarrayCircular(a) == bruteCircular(a));
    }
}

static void test_max_product() {
    assert(maxProductSubarray({2, 3, -2, 4}) == 6);
    assert(maxProductSubarray({-2, 0, -1}) == 0);
    assert(maxProductSubarray({-2, 3, -4}) == 24);
    assert(maxProductSubarray({-5}) == -5);
}

static void test_max_product_random() {
    std::mt19937 rng(53);
    for (int t = 0; t < 2000; ++t) {
        V a(rng() % 10 + 1);
        for (auto& x : a) x = static_cast<int>(rng() % 9) - 4;
        assert(maxProductSubarray(a) == bruteProduct(a));
    }
}

int main() {
    void (*tests[])() = {test_classic, test_all_negative, test_single_and_empty,
                         test_indices_sum_matches, test_circular, test_circular_random,
                         test_max_product, test_max_product_random};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
