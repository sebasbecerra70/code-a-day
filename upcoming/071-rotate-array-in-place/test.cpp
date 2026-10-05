#include "solution.hpp"

#include <algorithm>
#include <cassert>
#include <iostream>
#include <memory>
#include <random>
#include <string>

using V = std::vector<int>;

static V expectedRotation(const V& a, std::size_t k) {
    V out(a.size());
    for (std::size_t i = 0; i < a.size(); ++i) out[(i + k) % a.size()] = a[i];
    return out;
}

static void checkBoth(V a, std::size_t k, const V& expected) {
    V b = a;
    rotateRightReversal(a, k);
    rotateRightCycles(b, k);
    assert(a == expected && b == expected);
}

static void test_example() {
    checkBoth({1, 2, 3, 4, 5, 6, 7}, 3, {5, 6, 7, 1, 2, 3, 4});
}

static void test_k_zero_and_multiple_of_n() {
    checkBoth({1, 2, 3}, 0, {1, 2, 3});
    checkBoth({1, 2, 3}, 3, {1, 2, 3});
    checkBoth({1, 2, 3}, 7, {3, 1, 2});  // 7 % 3 == 1
}

static void test_empty_and_single() {
    checkBoth({}, 5, {});
    checkBoth({42}, 9, {42});
}

static void test_multiple_cycles() {
    // n = 6, k = 2 -> gcd 2, so the cycle method needs two cycles.
    checkBoth({1, 2, 3, 4, 5, 6}, 2, {5, 6, 1, 2, 3, 4});
    checkBoth({1, 2, 3, 4, 5, 6}, 3, {4, 5, 6, 1, 2, 3});
}

static void test_random_against_std_rotate() {
    std::mt19937 rng(41);
    for (int t = 0; t < 2000; ++t) {
        V a(rng() % 20);
        std::iota(a.begin(), a.end(), 0);
        std::size_t k = rng() % 50;
        V expected = a;
        if (!a.empty()) std::rotate(expected.begin(), expected.end() - static_cast<long>(k % a.size()), expected.end());
        assert(expected == expectedRotation(a, k % std::max<std::size_t>(a.size(), 1)));
        checkBoth(a, k, expected);
    }
}

static void test_move_only_elements() {
    std::vector<std::unique_ptr<int>> a;
    for (int i = 0; i < 6; ++i) a.push_back(std::make_unique<int>(i));
    rotateRightCycles(a, 4);
    std::vector<int> got;
    for (auto& p : a) got.push_back(*p);
    assert((got == V{2, 3, 4, 5, 0, 1}));
}

static void test_rotate_matrix() {
    std::vector<V> m = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
    rotateMatrix90(m);
    assert((m == std::vector<V>{{7, 4, 1}, {8, 5, 2}, {9, 6, 3}}));
    for (int i = 0; i < 3; ++i) rotateMatrix90(m);  // four rotations total
    assert((m == std::vector<V>{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}));
    std::vector<V> empty;
    rotateMatrix90(empty);
    bool threw = false;
    std::vector<V> bad = {{1, 2}};
    try { rotateMatrix90(bad); } catch (const std::invalid_argument&) { threw = true; }
    assert(threw);
}

int main() {
    void (*tests[])() = {test_example, test_k_zero_and_multiple_of_n, test_empty_and_single,
                         test_multiple_cycles, test_random_against_std_rotate,
                         test_move_only_elements, test_rotate_matrix};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
