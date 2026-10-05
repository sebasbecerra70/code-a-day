#include "solution.hpp"

#include <algorithm>
#include <cassert>
#include <iostream>
#include <random>
#include <set>

using Edges = std::vector<std::pair<int, int>>;

static BipartiteMatcher build(int L, int R, const Edges& es) {
    BipartiteMatcher m(L, R);
    for (auto [l, r] : es) m.addEdge(l, r);
    return m;
}

static bool validMatching(const Edges& es, const Edges& pairs) {
    std::set<std::pair<int, int>> edgeSet(es.begin(), es.end());
    std::set<int> usedL, usedR;
    for (auto [l, r] : pairs) {
        if (!edgeSet.count({l, r}) || !usedL.insert(l).second || !usedR.insert(r).second) return false;
    }
    return true;
}

// Brute force: try every subset of edges.
static int bruteMax(int L, int R, const Edges& es) {
    int best = 0;
    for (int mask = 0; mask < (1 << es.size()); ++mask) {
        std::vector<bool> ul(L), ur(R);
        int cnt = 0;
        bool ok = true;
        for (std::size_t i = 0; i < es.size() && ok; ++i) {
            if (!(mask >> i & 1)) continue;
            auto [l, r] = es[i];
            if (ul[l] || ur[r]) ok = false;
            ul[l] = ur[r] = true;
            ++cnt;
        }
        if (ok) best = std::max(best, cnt);
    }
    return best;
}

static void test_perfect_matching() {
    Edges es{{0, 0}, {0, 1}, {1, 0}, {2, 2}};
    auto m = build(3, 3, es);
    assert(m.solve() == 3);
    assert(validMatching(es, m.pairs()));
}

static void test_needs_augmenting_path() {
    // Greedy matches 0-0, then 1 can only use 0: must re-route 0 to 1.
    Edges es{{0, 0}, {0, 1}, {1, 0}};
    auto m = build(2, 2, es);
    assert(m.solve() == 2);
    assert(validMatching(es, m.pairs()));
}

static void test_long_alternating_chain() {
    // Left i connects to right i and i+1; greedy takes i->i for all but the last.
    const int n = 50;
    Edges es;
    for (int i = 0; i < n; ++i) {
        es.push_back({i, i + 1});
        es.push_back({i, i});
    }
    auto m = build(n, n + 1, es);
    assert(m.solve() == n);
}

static void test_no_edges_and_empty_sides() {
    assert(build(3, 3, {}).solve() == 0);
    assert(build(0, 5, {}).solve() == 0);
    assert(build(4, 0, {}).solve() == 0);
}

static void test_star_graph() {
    Edges es{{0, 0}, {1, 0}, {2, 0}, {3, 0}};
    assert(build(4, 1, es).solve() == 1);
}

static void test_job_assignment() {
    // Workers 0..3, jobs 0..3; worker 3 can only do job 0.
    Edges es{{0, 0}, {0, 1}, {1, 1}, {1, 2}, {2, 2}, {2, 3}, {3, 0}};
    auto m = build(4, 4, es);
    assert(m.solve() == 4);
    auto p = m.pairs();
    assert(std::find(p.begin(), p.end(), std::make_pair(3, 0)) != p.end());
}

static void test_invalid_edge() {
    BipartiteMatcher m(2, 2);
    bool threw = false;
    try { m.addEdge(0, 2); } catch (const std::out_of_range&) { threw = true; }
    assert(threw);
}

static void test_random_against_brute() {
    std::mt19937 rng(99);
    for (int t = 0; t < 300; ++t) {
        int L = static_cast<int>(rng() % 5) + 1, R = static_cast<int>(rng() % 5) + 1;
        Edges es;
        std::set<std::pair<int, int>> seen;
        int m = static_cast<int>(rng() % 12);
        for (int i = 0; i < m; ++i) {
            std::pair<int, int> e{static_cast<int>(rng() % L), static_cast<int>(rng() % R)};
            if (seen.insert(e).second) es.push_back(e);
        }
        auto bm = build(L, R, es);
        int got = bm.solve();
        assert(got == bruteMax(L, R, es));
        assert(validMatching(es, bm.pairs()) && static_cast<int>(bm.pairs().size()) == got);
    }
}

int main() {
    void (*tests[])() = {test_perfect_matching, test_needs_augmenting_path,
                         test_long_alternating_chain, test_no_edges_and_empty_sides,
                         test_star_graph, test_job_assignment, test_invalid_edge,
                         test_random_against_brute};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
