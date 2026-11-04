#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <random>

using Dense = std::vector<std::vector<double>>;

template <typename E, typename F>
static bool throws(F f) {
    try { f(); } catch (const E&) { return true; }
    return false;
}

static SparseMatrix sample() {
    // [[1 0 2]
    //  [0 0 0]
    //  [0 3 0]]
    return SparseMatrix::fromTriplets(3, 3, {{2, 1, 3}, {0, 2, 2}, {0, 0, 1}});
}

static void test_construction_and_get() {
    auto m = sample();
    assert(m.nnz() == 3);
    assert(m.get(0, 0) == 1 && m.get(0, 2) == 2 && m.get(2, 1) == 3);
    assert(m.get(1, 1) == 0 && m.get(0, 1) == 0);
    assert(throws<std::out_of_range>([&] { m.get(3, 0); }));
}

static void test_duplicates_summed_and_zeros_dropped() {
    auto m = SparseMatrix::fromTriplets(2, 2, {{0, 0, 1}, {0, 0, 2}, {1, 1, 0}, {1, 0, 5}, {1, 0, -5}});
    assert(m.nnz() == 1 && m.get(0, 0) == 3);
}

static void test_empty_matrix() {
    SparseMatrix m(4, 5);
    assert(m.nnz() == 0 && m.get(3, 4) == 0);
    assert((m.multiply(std::vector<double>(5, 1.0)) == std::vector<double>(4, 0.0)));
    assert(throws<std::out_of_range>([] { SparseMatrix::fromTriplets(1, 1, {{0, 1, 1}}); }));
}

static void test_matvec() {
    auto y = sample().multiply(std::vector<double>{1, 2, 3});
    assert((y == std::vector<double>{7, 0, 6}));
    assert(throws<std::invalid_argument>([] { sample().multiply(std::vector<double>{1}); }));
}

static void test_transpose() {
    auto t = sample().transpose();
    assert(t.get(2, 0) == 2 && t.get(1, 2) == 3 && t.get(0, 2) == 0);
}

static void test_add_cancels() {
    auto m = sample();
    auto neg = SparseMatrix::fromTriplets(3, 3, {{0, 0, -1}});
    auto s = m.add(neg);
    assert(s.nnz() == 2 && s.get(0, 0) == 0);
}

static Dense denseMul(const Dense& a, const Dense& b) {
    Dense c(a.size(), std::vector<double>(b[0].size(), 0));
    for (std::size_t i = 0; i < a.size(); ++i)
        for (std::size_t k = 0; k < b.size(); ++k)
            for (std::size_t j = 0; j < b[0].size(); ++j) c[i][j] += a[i][k] * b[k][j];
    return c;
}

static void test_random_against_dense() {
    std::mt19937 rng(17);
    auto rnd = [&](std::size_t r, std::size_t c) {
        std::vector<SparseMatrix::Triplet> ts;
        for (int k = 0; k < 8; ++k)
            ts.push_back({rng() % r, rng() % c, static_cast<double>(static_cast<int>(rng() % 9) - 4)});
        return SparseMatrix::fromTriplets(r, c, ts);
    };
    for (int t = 0; t < 200; ++t) {
        auto a = rnd(5, 4), b = rnd(4, 6), a2 = rnd(5, 4);
        assert(a.multiply(b).toDense() == denseMul(a.toDense(), b.toDense()));
        assert(a.transpose().transpose().toDense() == a.toDense());
        Dense sum = a.toDense();
        auto d2 = a2.toDense();
        for (std::size_t i = 0; i < 5; ++i)
            for (std::size_t j = 0; j < 4; ++j) sum[i][j] += d2[i][j];
        assert(a.add(a2).toDense() == sum);
        std::vector<double> x{1, -2, 3, 0.5};
        auto y = a.multiply(x);
        auto dense = a.toDense();
        for (std::size_t i = 0; i < 5; ++i) {
            double e = 0;
            for (std::size_t j = 0; j < 4; ++j) e += dense[i][j] * x[j];
            assert(y[i] == e);
        }
    }
}

int main() {
    void (*tests[])() = {test_construction_and_get, test_duplicates_summed_and_zeros_dropped,
                         test_empty_matrix, test_matvec, test_transpose, test_add_cancels,
                         test_random_against_dense};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
