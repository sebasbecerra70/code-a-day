#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <map>
#include <random>
#include <tuple>

using Edges = std::vector<std::tuple<int, int, long long>>;

static MaxFlow build(int n, const Edges& es) {
    MaxFlow f(n);
    for (auto [u, v, c] : es) f.addEdge(u, v, c);
    return f;
}

// Brute force min cut: try every partition with s on one side and t on the other.
static long long bruteMinCut(int n, const Edges& es, int s, int t) {
    long long best = LLONG_MAX;
    for (int mask = 0; mask < (1 << n); ++mask) {
        if (!(mask >> s & 1) || (mask >> t & 1)) continue;
        long long cut = 0;
        for (auto [u, v, c] : es)
            if ((mask >> u & 1) && !(mask >> v & 1)) cut += c;
        best = std::min(best, cut);
    }
    return best;
}

static void test_clrs_example() {
    // CLRS figure 26.1: max flow 23.
    Edges es{{0, 1, 16}, {0, 2, 13}, {1, 3, 12}, {2, 1, 4}, {2, 4, 14},
             {3, 2, 9},  {3, 5, 20}, {4, 3, 7},  {4, 5, 4}};
    auto f = build(6, es);
    assert(f.maxflow(0, 5) == 23);
    long long cutCap = 0;
    std::map<std::pair<int, int>, long long> cap;
    for (auto [u, v, c] : es) cap[{u, v}] += c;
    for (auto e : f.minCut(0)) cutCap += cap[e];
    assert(cutCap == 23);  // max-flow min-cut theorem
}

static void test_needs_reverse_edge() {
    // Greedy path 0-1-2-3 blocks both routes unless flow can be pushed back along 1-2.
    auto f = build(4, {{0, 1, 1}, {0, 2, 1}, {1, 2, 1}, {1, 3, 1}, {2, 3, 1}});
    assert(f.maxflow(0, 3) == 2);
}

static void test_disconnected() {
    auto f = build(4, {{0, 1, 5}, {2, 3, 5}});
    assert(f.maxflow(0, 3) == 0);
    assert(f.minCut(0).empty());
}

static void test_parallel_and_antiparallel_edges() {
    auto f = build(2, {{0, 1, 3}, {0, 1, 4}, {1, 0, 10}});
    assert(f.maxflow(0, 1) == 7);
}

static void test_bottleneck_chain() {
    auto f = build(5, {{0, 1, 100}, {1, 2, 1}, {2, 3, 100}, {3, 4, 100}});
    assert(f.maxflow(0, 4) == 1);
    auto cut = f.minCut(0);
    assert(cut.size() == 1 && cut[0] == std::make_pair(1, 2));
}

static void test_large_capacities() {
    const long long big = 1'000'000'000'000LL;
    auto f = build(3, {{0, 1, big}, {1, 2, big}, {0, 2, big}});
    assert(f.maxflow(0, 2) == 2 * big);
}

static void test_invalid_arguments() {
    MaxFlow f(2);
    bool threw = false;
    try { f.addEdge(0, 1, -1); } catch (const std::invalid_argument&) { threw = true; }
    assert(threw);
    threw = false;
    try { f.addEdge(0, 2, 1); } catch (const std::out_of_range&) { threw = true; }
    assert(threw);
    threw = false;
    try { f.maxflow(1, 1); } catch (const std::invalid_argument&) { threw = true; }
    assert(threw);
}

static void test_random_against_brute_min_cut() {
    std::mt19937 rng(95);
    for (int t = 0; t < 300; ++t) {
        int n = static_cast<int>(rng() % 6) + 2;
        Edges es;
        int m = static_cast<int>(rng() % 15);
        for (int i = 0; i < m; ++i)
            es.push_back({static_cast<int>(rng() % n), static_cast<int>(rng() % n), static_cast<long long>(rng() % 10)});
        auto f = build(n, es);
        long long flow = f.maxflow(0, n - 1);
        assert(flow == bruteMinCut(n, es, 0, n - 1));
        std::map<std::pair<int, int>, long long> cap;
        for (auto [u, v, c] : es) cap[{u, v}] += c;
        long long cutCap = 0;
        auto cut = f.minCut(0);
        std::sort(cut.begin(), cut.end());
        cut.erase(std::unique(cut.begin(), cut.end()), cut.end());  // parallel edges
        for (auto e : cut) cutCap += cap[e];
        assert(cutCap == flow);
    }
}

int main() {
    void (*tests[])() = {test_clrs_example, test_needs_reverse_edge, test_disconnected,
                         test_parallel_and_antiparallel_edges, test_bottleneck_chain,
                         test_large_capacities, test_invalid_arguments,
                         test_random_against_brute_min_cut};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
