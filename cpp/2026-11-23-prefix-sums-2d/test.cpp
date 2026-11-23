#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <random>

static long long bruteSum(const Grid& g, std::size_t r1, std::size_t c1, std::size_t r2, std::size_t c2) {
    long long s = 0;
    for (std::size_t r = r1; r <= r2; ++r)
        for (std::size_t c = c1; c <= c2; ++c) s += g[r][c];
    return s;
}

static Grid randomGrid(std::mt19937& rng, std::size_t R, std::size_t C) {
    Grid g(R, std::vector<long long>(C));
    for (auto& row : g)
        for (auto& x : row) x = static_cast<long long>(rng() % 201) - 100;
    return g;
}

static void test_leetcode_example() {
    Grid g = {{3, 0, 1, 4, 2}, {5, 6, 3, 2, 1}, {1, 2, 0, 1, 5}, {4, 1, 0, 1, 7}, {1, 0, 3, 0, 5}};
    PrefixSum2D ps(g);
    assert(ps.sum(2, 1, 4, 3) == 8);
    assert(ps.sum(1, 1, 2, 2) == 11);
    assert(ps.sum(1, 2, 2, 4) == 12);
}

static void test_single_cell_and_whole_grid() {
    Grid g = {{1, 2}, {3, 4}};
    PrefixSum2D ps(g);
    assert(ps.sum(1, 0, 1, 0) == 3);
    assert(ps.sum(0, 0, 1, 1) == 10);
}

static void test_invalid_rectangles() {
    PrefixSum2D ps(Grid{{1, 2}, {3, 4}});
    for (auto f : {+[](const PrefixSum2D& p) { p.sum(1, 0, 0, 0); },
                   +[](const PrefixSum2D& p) { p.sum(0, 0, 2, 0); }}) {
        bool threw = false;
        try { f(ps); } catch (const std::out_of_range&) { threw = true; }
        assert(threw);
    }
    bool threw = false;
    try { PrefixSum2D bad(Grid{{1, 2}, {3}}); } catch (const std::invalid_argument&) { threw = true; }
    assert(threw);
}

static void test_empty_grid() {
    PrefixSum2D ps(Grid{});
    bool threw = false;
    try { ps.sum(0, 0, 0, 0); } catch (const std::out_of_range&) { threw = true; }
    assert(threw);
}

static void test_random_queries() {
    std::mt19937 rng(4);
    for (int t = 0; t < 30; ++t) {
        std::size_t R = rng() % 8 + 1, C = rng() % 8 + 1;
        Grid g = randomGrid(rng, R, C);
        PrefixSum2D ps(g);
        for (int q = 0; q < 50; ++q) {
            std::size_t r1 = rng() % R, r2 = rng() % R, c1 = rng() % C, c2 = rng() % C;
            if (r1 > r2) std::swap(r1, r2);
            if (c1 > c2) std::swap(c1, c2);
            assert(ps.sum(r1, c1, r2, c2) == bruteSum(g, r1, c1, r2, c2));
        }
    }
}

static void test_rect_adder() {
    RectAdder ra(3, 3);
    ra.add(0, 0, 1, 1, 5);
    ra.add(1, 1, 2, 2, 2);
    Grid expected = {{5, 5, 0}, {5, 7, 2}, {0, 2, 2}};
    assert(ra.build() == expected);
}

static void test_rect_adder_random() {
    std::mt19937 rng(8);
    const std::size_t R = 6, C = 7;
    RectAdder ra(R, C);
    Grid naive(R, std::vector<long long>(C, 0));
    for (int t = 0; t < 200; ++t) {
        std::size_t r1 = rng() % R, r2 = rng() % R, c1 = rng() % C, c2 = rng() % C;
        if (r1 > r2) std::swap(r1, r2);
        if (c1 > c2) std::swap(c1, c2);
        long long v = static_cast<long long>(rng() % 21) - 10;
        ra.add(r1, c1, r2, c2, v);
        for (std::size_t r = r1; r <= r2; ++r)
            for (std::size_t c = c1; c <= c2; ++c) naive[r][c] += v;
    }
    assert(ra.build() == naive);
}

static void test_max_square_sum() {
    Grid g = {{1, 1, 1}, {1, 9, 9}, {1, 9, 9}};
    assert(maxSquareSum(g, 2) == 36);
    assert(maxSquareSum(g, 3) == 41);
    assert(maxSquareSum(Grid{{-5, -2}, {-3, -4}}, 1) == -2);
    std::mt19937 rng(2);
    Grid r = randomGrid(rng, 7, 9);
    long long best = std::numeric_limits<long long>::min();
    for (std::size_t i = 0; i + 3 <= 7; ++i)
        for (std::size_t j = 0; j + 3 <= 9; ++j) best = std::max(best, bruteSum(r, i, j, i + 2, j + 2));
    assert(maxSquareSum(r, 3) == best);
}

int main() {
    void (*tests[])() = {test_leetcode_example, test_single_cell_and_whole_grid,
                         test_invalid_rectangles, test_empty_grid, test_random_queries,
                         test_rect_adder, test_rect_adder_random, test_max_square_sum};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
