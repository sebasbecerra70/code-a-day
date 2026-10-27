#include "solution.hpp"

#include <algorithm>
#include <cassert>
#include <iostream>
#include <random>
#include <sstream>
#include <vector>

using R = Rational;

static void test_normalization() {
    assert(R(2, 4) == R(1, 2));
    assert(R(1, -2).num() == -1 && R(1, -2).den() == 2);
    assert(R(-3, -6) == R(1, 2));
    assert(R(0, -5).num() == 0 && R(0, -5).den() == 1);
    assert(R(7).den() == 1);
}

static void test_zero_denominator() {
    bool threw = false;
    try { R(1, 0); } catch (const std::domain_error&) { threw = true; }
    assert(threw);
}

static void test_arithmetic() {
    assert(R(1, 2) + R(1, 3) == R(5, 6));
    assert(R(1, 2) - R(3, 4) == R(-1, 4));
    assert(R(2, 3) * R(9, 4) == R(3, 2));
    assert(R(1, 2) / R(1, 4) == R(2));
    assert(-R(3, 5) == R(-3, 5));
    assert(R(1, 3) + 2 == R(7, 3));  // implicit conversion from int
}

static void test_divide_by_zero() {
    bool threw = false;
    try { R(1, 2) / R(0); } catch (const std::domain_error&) { threw = true; }
    assert(threw);
}

static void test_comparison_and_sort() {
    assert(R(1, 3) < R(1, 2));
    assert(R(-1, 2) < R(1, 3));
    assert(R(2, 4) <= R(1, 2) && R(2, 4) >= R(1, 2));
    std::vector<R> v{R(3, 4), R(-1, 2), R(1, 3), R(2, 3)};
    std::sort(v.begin(), v.end());
    assert((v == std::vector<R>{R(-1, 2), R(1, 3), R(2, 3), R(3, 4)}));
}

static void test_harmonic_sum() {
    R h;
    for (int i = 1; i <= 10; ++i) h += R(1, i);
    assert(h == R(7381, 2520));
}

static void test_printing() {
    std::ostringstream os;
    os << R(6, -8) << ' ' << R(10, 5);
    assert(os.str() == "-3/4 2");
}

static void test_random_field_laws() {
    std::mt19937 rng(11);
    auto rnd = [&] {
        std::int64_t d = 0;
        while (d == 0) d = static_cast<std::int64_t>(rng() % 41) - 20;
        return R(static_cast<std::int64_t>(rng() % 41) - 20, d);
    };
    for (int t = 0; t < 2000; ++t) {
        R a = rnd(), b = rnd(), c = rnd();
        assert(a + b == b + a && a * b == b * a);
        assert((a + b) + c == a + (b + c));
        assert(a * (b + c) == a * b + a * c);
        assert(a - a == R(0));
        if (b != R(0)) assert((a / b) * b == a);
        assert((a < b) == (a.toDouble() < b.toDouble()) || a == b);
    }
}

int main() {
    void (*tests[])() = {test_normalization, test_zero_denominator, test_arithmetic,
                         test_divide_by_zero, test_comparison_and_sort, test_harmonic_sum,
                         test_printing, test_random_field_laws};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
