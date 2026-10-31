#include "solution.hpp"

#include <cassert>
#include <climits>
#include <iostream>
#include <random>

static void test_parse_and_print() {
    assert(BigInt("0").str() == "0");
    assert(BigInt("-0").str() == "0");
    assert(BigInt("000123").str() == "123");
    assert(BigInt("+42").str() == "42");
    assert(BigInt("-1000000000000000000000").str() == "-1000000000000000000000");
    assert(BigInt("1000000001").str() == "1000000001");  // inner limb zero-padding
}

static void test_bad_input() {
    for (const char* s : {"", "-", "12a", "1 2"}) {
        bool threw = false;
        try { BigInt b{std::string(s)}; } catch (const std::invalid_argument&) { threw = true; }
        assert(threw);
    }
}

static void test_from_long_long_extremes() {
    assert(BigInt(LLONG_MIN).str() == "-9223372036854775808");
    assert(BigInt(LLONG_MAX).str() == "9223372036854775807");
}

static void test_add_sub_carry() {
    BigInt a("999999999999999999");
    assert((a + 1).str() == "1000000000000000000");
    assert((BigInt("1000000000000000000") - 1) == a);
    assert((BigInt(5) - 8).str() == "-3");
    assert((BigInt(-5) + 5).str() == "0");
}

static void test_factorial() {
    BigInt f = 1;
    for (int i = 2; i <= 30; ++i) f *= i;
    assert(f.str() == "265252859812191058636308480000000");
}

static void test_power_of_two() {
    BigInt p = 1;
    for (int i = 0; i < 128; ++i) p *= 2;
    assert(p.str() == "340282366920938463463374607431768211456");
    assert(p / BigInt("18446744073709551616") == BigInt("18446744073709551616"));
}

static void test_division_signs() {
    // Matches C++ truncation semantics.
    assert(BigInt(7) / 2 == 3 && BigInt(7) % 2 == 1);
    assert(BigInt(-7) / 2 == -3 && BigInt(-7) % 2 == -1);
    assert(BigInt(7) / -2 == -3 && BigInt(7) % -2 == 1);
    bool threw = false;
    try { BigInt(1) / 0; } catch (const std::domain_error&) { threw = true; }
    assert(threw);
}

static void test_comparisons() {
    assert(BigInt(-10) < BigInt(-2));
    assert(BigInt(-1) < BigInt(0));
    assert(BigInt("100000000000") > BigInt("99999999999"));
}

static void test_random_against_int128() {
    std::mt19937_64 rng(5);
    auto to128 = [](const BigInt& b) {
        __int128 v = 0;
        std::string s = b.str();
        bool neg = s[0] == '-';
        for (std::size_t i = neg; i < s.size(); ++i) v = v * 10 + (s[i] - '0');
        return neg ? -v : v;
    };
    for (int t = 0; t < 3000; ++t) {
        long long x = static_cast<long long>(rng()) >> (rng() % 63);
        long long y = static_cast<long long>(rng()) >> (rng() % 63);
        BigInt a(x), b(y);
        assert(to128(a + b) == static_cast<__int128>(x) + y);
        assert(to128(a - b) == static_cast<__int128>(x) - y);
        assert(to128(a * b) == static_cast<__int128>(x) * y);
        assert((a < b) == (x < y));
        if (y != 0) {
            auto [q, r] = BigInt::divmod(a, b);
            assert(to128(q) == static_cast<__int128>(x) / y);
            assert(to128(r) == static_cast<__int128>(x) % y);
        }
    }
}

static void test_big_division_identity() {
    std::mt19937 rng(9);
    for (int t = 0; t < 30; ++t) {
        std::string sa = "1", sb = "1";
        for (int i = 0; i < 60; ++i) sa += static_cast<char>('0' + rng() % 10);
        for (int i = 0; i < 25; ++i) sb += static_cast<char>('0' + rng() % 10);
        BigInt a(sa), b(sb);
        auto [q, r] = BigInt::divmod(a, b);
        assert(q * b + r == a && r >= 0 && r < b);
    }
}

int main() {
    void (*tests[])() = {test_parse_and_print, test_bad_input, test_from_long_long_extremes,
                         test_add_sub_carry, test_factorial, test_power_of_two,
                         test_division_signs, test_comparisons, test_random_against_int128,
                         test_big_division_identity};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
