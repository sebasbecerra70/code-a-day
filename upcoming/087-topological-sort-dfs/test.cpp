#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <random>
#include <set>

using Edges = std::vector<std::pair<int, int>>;

static bool isTopological(int n, const Edges& edges, const std::vector<int>& order) {
    if (static_cast<int>(order.size()) != n) return false;
    std::vector<int> pos(n, -1);
    for (int i = 0; i < n; ++i) {
        if (order[i] < 0 || order[i] >= n || pos[order[i]] != -1) return false;
        pos[order[i]] = i;
    }
    for (auto [u, v] : edges)
        if (pos[u] >= pos[v]) return false;
    return true;
}

static bool isRealCycle(const Edges& edges, const std::vector<int>& cyc) {
    if (cyc.size() < 2 || cyc.front() != cyc.back()) return false;
    std::set<std::pair<int, int>> es(edges.begin(), edges.end());
    for (std::size_t i = 0; i + 1 < cyc.size(); ++i)
        if (!es.count({cyc[i], cyc[i + 1]})) return false;
    return true;
}

static void test_course_schedule() {
    // 0 -> 1 -> 3, 0 -> 2 -> 3
    Edges e{{0, 1}, {0, 2}, {1, 3}, {2, 3}};
    auto r = topoSortDFS(4, e);
    assert(r.ok() && isTopological(4, e, r.order));
    assert(r.order.front() == 0 && r.order.back() == 3);
}

static void test_no_edges_and_empty() {
    auto r = topoSortDFS(3, {});
    assert(r.ok() && r.order.size() == 3);
    assert(topoSortDFS(0, {}).order.empty());
}

static void test_simple_cycle() {
    Edges e{{0, 1}, {1, 2}, {2, 0}};
    auto r = topoSortDFS(3, e);
    assert(!r.ok() && isRealCycle(e, r.cycle) && r.cycle.size() == 4);
}

static void test_self_loop() {
    Edges e{{0, 1}, {1, 1}};
    auto r = topoSortDFS(2, e);
    assert(!r.ok() && (r.cycle == std::vector<int>{1, 1}));
}

static void test_cycle_reachable_from_dag_part() {
    Edges e{{0, 1}, {1, 2}, {2, 3}, {3, 1}, {4, 0}};
    auto r = topoSortDFS(5, e);
    assert(!r.ok() && isRealCycle(e, r.cycle));
}

static void test_diamond_is_not_a_cycle() {
    // Cross/forward edges to Black vertices must not be reported as cycles.
    Edges e{{0, 1}, {0, 2}, {2, 1}, {0, 3}, {3, 1}};
    assert(topoSortDFS(4, e).ok());
}

static void test_kahn_lexicographic() {
    Edges e{{5, 2}, {5, 0}, {4, 0}, {4, 1}, {2, 3}, {3, 1}};
    auto k = topoSortKahn(6, e);
    assert(k && (*k == std::vector<int>{4, 5, 0, 2, 3, 1}));
    assert(!topoSortKahn(2, {{0, 1}, {1, 0}}));
}

static void test_random_dags_and_cycles() {
    std::mt19937 rng(81);
    for (int t = 0; t < 500; ++t) {
        int n = static_cast<int>(rng() % 12) + 1;
        std::vector<int> perm(n);
        for (int i = 0; i < n; ++i) perm[i] = i;
        std::shuffle(perm.begin(), perm.end(), rng);
        Edges e;
        for (int k = 0; k < n * 2; ++k) {  // edges go forward in perm: always a DAG
            int a = static_cast<int>(rng() % n), b = static_cast<int>(rng() % n);
            if (a < b) e.push_back({perm[a], perm[b]});
        }
        bool addCycle = n > 1 && rng() % 2;
        if (addCycle) {
            int a = static_cast<int>(rng() % (n - 1));
            e.push_back({perm[a], perm[a + 1]});
            e.push_back({perm[a + 1], perm[a]});
        }
        auto r = topoSortDFS(n, e);
        auto k = topoSortKahn(n, e);
        assert(r.ok() == !addCycle && k.has_value() == !addCycle);
        if (r.ok()) assert(isTopological(n, e, r.order) && isTopological(n, e, *k));
        else assert(isRealCycle(e, r.cycle));
    }
}

static void test_deep_chain_no_stack_overflow() {
    const int n = 200000;
    Edges e;
    for (int i = 0; i + 1 < n; ++i) e.push_back({i, i + 1});
    auto r = topoSortDFS(n, e);
    assert(r.ok() && r.order.front() == 0 && r.order.back() == n - 1);
}

int main() {
    void (*tests[])() = {test_course_schedule, test_no_edges_and_empty, test_simple_cycle,
                         test_self_loop, test_cycle_reachable_from_dag_part,
                         test_diamond_is_not_a_cycle, test_kahn_lexicographic,
                         test_random_dags_and_cycles, test_deep_chain_no_stack_overflow};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
