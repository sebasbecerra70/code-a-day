#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <random>
#include <sstream>

using M = Matrix<long long>;

template <typename F>
static bool throws(F f) {
    try { f(); } catch (const std::invalid_argument&) { return true; }
    return false;
}

static void test_construct_and_index() {
    M m{{1, 2, 3}, {4, 5, 6}};
    assert(m.rows() == 2 && m.cols() == 3);
    assert(m(1, 2) == 6);
    m(0, 0) = 9;
    assert(m(0, 0) == 9);
    assert(throws([] { M bad{{1, 2}, {3}}; }));
}

static void test_add_sub_scalar() {
    M a{{1, 2}, {3, 4}}, b{{5, 6}, {7, 8}};
    assert((a + b == M{{6, 8}, {10, 12}}));
    assert((b - a == M{{4, 4}, {4, 4}}));
    assert((2 * a == M{{2, 4}, {6, 8}}));
    assert((a * 3 == M{{3, 6}, {9, 12}}));
    assert((-a == M{{-1, -2}, {-3, -4}}));
    assert(throws([&] { a + M(2, 3); }));
}

static void test_multiply() {
    M a{{1, 2, 3}, {4, 5, 6}};
    M b{{7, 8}, {9, 10}, {11, 12}};
    assert((a * b == M{{58, 64}, {139, 154}}));
    assert(throws([&] { a * a; }));
}

static void test_identity() {
    M a{{2, 3}, {5, 7}};
    assert(a * M::identity(2) == a && M::identity(2) * a == a);
}

static void test_transpose() {
    M a{{1, 2, 3}, {4, 5, 6}};
    M t = a.transpose();
    assert(t.rows() == 3 && t.cols() == 2 && t(2, 1) == 6);
    assert(t.transpose() == a);
}

static void test_pow_fibonacci() {
    M f{{1, 1}, {1, 0}};
    assert(f.pow(0) == M::identity(2));
    assert(f.pow(10)(0, 1) == 55);
    assert(f.pow(90)(0, 1) == 2880067194370816120LL);
}

static void test_random_properties() {
    std::mt19937 rng(3);
    auto rnd = [&](std::size_t r, std::size_t c) {
        M m(r, c);
        for (std::size_t i = 0; i < r; ++i)
            for (std::size_t j = 0; j < c; ++j) m(i, j) = static_cast<long long>(rng() % 21) - 10;
        return m;
    };
    for (int t = 0; t < 50; ++t) {
        M a = rnd(3, 4), b = rnd(4, 2), c = rnd(2, 5);
        assert((a * b) * c == a * (b * c));                       // associativity
        assert((a * b).transpose() == b.transpose() * a.transpose());
        M d = rnd(4, 2);
        assert(a * (b + d) == a * b + a * d);                     // distributivity
    }
}

static void test_stream_output() {
    std::ostringstream os;
    os << M{{1, 2}, {3, 4}};
    assert(os.str() == "[1 2]\n[3 4]\n");
}

int main() {
    void (*tests[])() = {test_construct_and_index, test_add_sub_scalar, test_multiply,
                         test_identity, test_transpose, test_pow_fibonacci,
                         test_random_properties, test_stream_output};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
