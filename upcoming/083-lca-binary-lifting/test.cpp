#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <random>

using Edges = std::vector<std::pair<int, int>>;

// Tree rooted at 0: children 0->{1,2,3}, 1->{4,5}, 3->{6}, 4->{7}.
static Edges sampleTree() { return {{0, 1}, {0, 2}, {0, 3}, {1, 4}, {1, 5}, {3, 6}, {4, 7}}; }

static int naiveLca(const std::vector<int>& parent, const std::vector<int>& depth, int a, int b) {
    while (depth[a] > depth[b]) a = parent[a];
    while (depth[b] > depth[a]) b = parent[b];
    while (a != b) { a = parent[a]; b = parent[b]; }
    return a;
}

static void test_sample_queries() {
    LCA lca(8, sampleTree());
    assert(lca.query(7, 5) == 1);
    assert(lca.query(7, 6) == 0);
    assert(lca.query(4, 7) == 4);  // ancestor of itself's descendant
    assert(lca.query(2, 2) == 2);
    assert(lca.query(5, 4) == 1);
}

static void test_depth_and_distance() {
    LCA lca(8, sampleTree());
    assert(lca.depth(0) == 0 && lca.depth(7) == 3);
    assert(lca.distance(7, 6) == 5);
    assert(lca.distance(5, 5) == 0);
}

static void test_kth_ancestor() {
    LCA lca(8, sampleTree());
    assert(lca.kthAncestor(7, 0) == 7);
    assert(lca.kthAncestor(7, 1) == 4);
    assert(lca.kthAncestor(7, 3) == 0);
    assert(lca.kthAncestor(7, 4) == -1);
}

static void test_single_vertex() {
    LCA lca(1, {});
    assert(lca.query(0, 0) == 0 && lca.kthAncestor(0, 1) == -1);
}

static void test_different_root() {
    LCA lca(8, sampleTree(), 7);
    assert(lca.query(0, 5) == 1);
    assert(lca.query(6, 2) == 0);
}

static void test_invalid_input() {
    bool threw = false;
    try { LCA bad(3, {{0, 1}}); } catch (const std::invalid_argument&) { threw = true; }
    assert(threw);
    threw = false;
    try { LCA bad(4, {{0, 1}, {1, 0}, {2, 3}}); } catch (const std::invalid_argument&) { threw = true; }
    assert(threw);
    threw = false;
    LCA ok(2, {{0, 1}});
    try { ok.query(0, 5); } catch (const std::out_of_range&) { threw = true; }
    assert(threw);
}

static void test_long_path() {
    const int n = 100000;
    Edges e;
    for (int i = 1; i < n; ++i) e.push_back({i - 1, i});
    LCA lca(n, e);
    assert(lca.query(n - 1, 50000) == 50000);
    assert(lca.kthAncestor(n - 1, n - 1) == 0);
}

static void test_random_trees_against_naive() {
    std::mt19937 rng(71);
    for (int t = 0; t < 50; ++t) {
        int n = static_cast<int>(rng() % 200) + 1;
        Edges e;
        std::vector<int> parent(n, 0), depth(n, 0);
        for (int v = 1; v < n; ++v) {  // parent[v] < v makes a random rooted tree
            parent[v] = static_cast<int>(rng() % v);
            depth[v] = depth[parent[v]] + 1;
            e.push_back({parent[v], v});
        }
        LCA lca(n, e);
        for (int q = 0; q < 200; ++q) {
            int a = static_cast<int>(rng() % n), b = static_cast<int>(rng() % n);
            assert(lca.query(a, b) == naiveLca(parent, depth, a, b));
        }
    }
}

int main() {
    void (*tests[])() = {test_sample_queries, test_depth_and_distance, test_kth_ancestor,
                         test_single_vertex, test_different_root, test_invalid_input,
                         test_long_path, test_random_trees_against_naive};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
