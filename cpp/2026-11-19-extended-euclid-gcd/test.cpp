#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <random>

static void test_gcd_basic() {
    assert(gcd(48, 18) == 6);
    assert(gcd(17, 5) == 1);
    assert(gcd(0, 7) == 7 && gcd(7, 0) == 7 && gcd(0, 0) == 0);
    assert(gcd(-12, 18) == 6);
}

static void test_bezout_random() {
    std::mt19937_64 rng(1);
    for (int t = 0; t < 5000; ++t) {
        i64 a = static_cast<i64>(rng() % 2000001) - 1000000;
        i64 b = static_cast<i64>(rng() % 2000001) - 1000000;
        auto [g, x, y] = extendedGcd(a, b);
        assert(g == gcd(a, b));
        assert(a * x + b * y == g);
    }
}

static void test_bezout_edge_cases() {
    auto r = extendedGcd(0, 0);
    assert(r.g == 0);
    r = extendedGcd(0, -5);
    assert(r.g == 5 && 0 * r.x + -5 * r.y == 5);
    r = extendedGcd(240, 46);
    assert(r.g == 2 && 240 * r.x + 46 * r.y == 2);
}

static void test_mod_inverse() {
    assert(*modInverse(3, 11) == 4);
    assert(*modInverse(10, 17) == 12);
    assert(*modInverse(-3, 11) == 7);  // -3 ≡ 8, 8*7 = 56 ≡ 1
    assert(!modInverse(6, 9).has_value());
    for (i64 m : {2, 9, 10, 97, 1000}) {
        for (i64 a = 0; a < m; ++a) {
            auto inv = modInverse(a, m);
            assert(inv.has_value() == (gcd(a, m) == 1));
            if (inv) assert(a * *inv % m == 1);
        }
    }
    bool threw = false;
    try { modInverse(3, 1); } catch (const std::invalid_argument&) { threw = true; }
    assert(threw);
}

static void test_diophantine() {
    auto s = solveDiophantine(6, 9, 21);
    assert(s && 6 * s->first + 9 * s->second == 21);
    assert(!solveDiophantine(6, 9, 20));
    assert(!solveDiophantine(0, 0, 1) && solveDiophantine(0, 0, 0));
    s = solveDiophantine(0, 4, 8);
    assert(s && 4 * s->second == 8);
}

static void test_crt_coprime() {
    // x ≡ 2 (mod 3), x ≡ 3 (mod 5) -> x = 8 (mod 15)
    auto r = crt(2, 3, 3, 5);
    assert(r && r->first == 8 && r->second == 15);
}

static void test_crt_non_coprime() {
    auto r = crt(2, 4, 4, 6);  // x ≡ 2 mod 4, x ≡ 4 mod 6 -> 10 mod 12
    assert(r && r->first == 10 && r->second == 12);
    assert(!crt(1, 4, 2, 6));  // parity conflict
}

static void test_crt_brute_force() {
    for (i64 m1 = 1; m1 <= 12; ++m1)
        for (i64 m2 = 1; m2 <= 12; ++m2)
            for (i64 r1 = 0; r1 < m1; ++r1)
                for (i64 r2 = 0; r2 < m2; ++r2) {
                    auto res = crt(r1, m1, r2, m2);
                    i64 found = -1;
                    for (i64 x = 0; x < m1 * m2 && found < 0; ++x)
                        if (x % m1 == r1 && x % m2 == r2) found = x;
                    assert(res.has_value() == (found >= 0));
                    if (res) assert(res->first == found);
                }
}

int main() {
    void (*tests[])() = {test_gcd_basic, test_bezout_random, test_bezout_edge_cases,
                         test_mod_inverse, test_diophantine, test_crt_coprime,
                         test_crt_non_coprime, test_crt_brute_force};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
