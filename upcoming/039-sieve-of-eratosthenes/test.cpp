#include "solution.hpp"

#include <cassert>
#include <iostream>

static bool isPrimeNaive(std::int64_t x) {
    if (x < 2) return false;
    for (std::int64_t d = 2; d * d <= x; ++d)
        if (x % d == 0) return false;
    return true;
}

static void test_small_cases() {
    assert(sieve(0).empty() && sieve(1).empty());
    assert((sieve(2) == std::vector<int>{2}));
    assert((sieve(30) == std::vector<int>{2, 3, 5, 7, 11, 13, 17, 19, 23, 29}));
}

static void test_prime_counting() {
    assert(sieve(100).size() == 25);
    assert(sieve(1'000'000).size() == 78498);
    assert(sieve(1'000'000).back() == 999983);
}

static void test_sieve_matches_trial_division() {
    auto primes = sieve(5000);
    std::vector<int> naive;
    for (int i = 0; i <= 5000; ++i)
        if (isPrimeNaive(i)) naive.push_back(i);
    assert(primes == naive);
}

static void test_spf_table() {
    auto spf = smallestPrimeFactor(100);
    assert(spf[2] == 2 && spf[97] == 97 && spf[91] == 7 && spf[100] == 2 && spf[45] == 3);
    for (int i = 2; i <= 100; ++i) {
        assert(i % spf[i] == 0 && isPrimeNaive(spf[i]));
        for (int d = 2; d < spf[i]; ++d) assert(i % d != 0);
    }
}

static void test_factorize() {
    auto spf = smallestPrimeFactor(100000);
    assert((factorize(360, spf) == std::vector<std::pair<int, int>>{{2, 3}, {3, 2}, {5, 1}}));
    assert(factorize(1, spf).empty());
    assert((factorize(99991, spf) == std::vector<std::pair<int, int>>{{99991, 1}}));
    for (int x = 2; x <= 100000; x += 37) {
        long long prod = 1;
        for (auto [p, e] : factorize(x, spf))
            for (int k = 0; k < e; ++k) prod *= p;
        assert(prod == x);
    }
}

static void test_segmented_matches_full() {
    auto full = sieve(20000);
    for (std::int64_t lo : {0, 1, 2, 97, 1000, 19990}) {
        auto seg = segmentedSieve(lo, 20000);
        std::vector<std::int64_t> expected;
        for (int p : full)
            if (p >= lo) expected.push_back(p);
        assert(seg == expected);
    }
}

static void test_segmented_large_window() {
    // Primes in a window near 10^12, checked by trial division.
    std::int64_t lo = 1'000'000'000'000LL, hi = lo + 1000;
    auto seg = segmentedSieve(lo, hi);
    std::vector<std::int64_t> expected;
    for (std::int64_t x = lo; x <= hi; ++x)
        if (isPrimeNaive(x)) expected.push_back(x);
    assert(seg == expected && !seg.empty());
    assert(segmentedSieve(10, 5).empty() && segmentedSieve(0, 1).empty());
}

int main() {
    void (*tests[])() = {test_small_cases, test_prime_counting, test_sieve_matches_trial_division,
                         test_spf_table, test_factorize, test_segmented_matches_full,
                         test_segmented_large_window};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
