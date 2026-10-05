#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <random>

static u64 naivePowMod(u64 b, u64 e, u64 m) {
    u64 r = 1 % m;
    for (u64 i = 0; i < e; ++i) r = mulMod(r, b, m);
    return r;
}

static bool naiveIsPrime(u64 n) {
    if (n < 2) return false;
    for (u64 d = 2; d * d <= n; ++d)
        if (n % d == 0) return false;
    return true;
}

static void test_basic_values() {
    assert(powMod(2, 10, 1000) == 24);
    assert(powMod(3, 0, 7) == 1);
    assert(powMod(0, 0, 7) == 1);  // convention: 0^0 = 1
    assert(powMod(0, 5, 7) == 0);
    assert(powMod(5, 3, 1) == 0);
}

static void test_zero_modulus() {
    bool threw = false;
    try { powMod(2, 3, 0); } catch (const std::invalid_argument&) { threw = true; }
    assert(threw);
}

static void test_random_against_naive() {
    std::mt19937_64 rng(1);
    for (int t = 0; t < 2000; ++t) {
        u64 m = rng() % 1000 + 1, b = rng(), e = rng() % 300;
        assert(powMod(b, e, m) == naivePowMod(b, e, m));
    }
}

static void test_large_modulus_no_overflow() {
    const u64 m = 0xFFFFFFFFFFFFFFC5ULL;  // largest 64-bit prime
    // Fermat: a^(p-1) == 1 mod p.
    assert(powMod(123456789, m - 1, m) == 1);
    assert(powMod(m - 1, 2, m) == 1);  // (-1)^2
}

static void test_fermat_inverse() {
    const u64 p = 1'000'000'007;
    for (u64 a : {1ULL, 2ULL, 12345ULL, p - 1}) assert(mulMod(a, inverseModPrime(a, p), p) == 1);
    bool threw = false;
    try { inverseModPrime(p * 3, p); } catch (const std::domain_error&) { threw = true; }
    assert(threw);
}

static void test_is_prime_small_range() {
    for (u64 n = 0; n < 20000; ++n) assert(isPrime(n) == naiveIsPrime(n));
}

static void test_is_prime_hard_cases() {
    // Carmichael numbers fool the plain Fermat test but not Miller-Rabin.
    for (u64 c : {561ULL, 1105ULL, 1729ULL, 2465ULL, 41041ULL, 825265ULL}) assert(!isPrime(c));
    // Strong pseudoprime to bases 2,3,5,7,11,13,17,19,23 (needs witness 29+).
    assert(!isPrime(3825123056546413051ULL));
    assert(isPrime(1'000'000'007ULL) && isPrime(998244353ULL));
    assert(isPrime(0xFFFFFFFFFFFFFFC5ULL));
    assert(!isPrime(1'000'000'007ULL * 998244353ULL));
}

int main() {
    void (*tests[])() = {test_basic_values, test_zero_modulus, test_random_against_naive,
                         test_large_modulus_no_overflow, test_fermat_inverse,
                         test_is_prime_small_range, test_is_prime_hard_cases};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
