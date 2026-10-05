#include "solution.hpp"

#include <cassert>
#include <functional>
#include <iostream>
#include <queue>
#include <random>

using Graph = std::vector<std::vector<Edge>>;

static std::vector<int> dijkstra(const Graph& adj, int src) {
    std::vector<int> dist(adj.size(), INT_MAX);
    using P = std::pair<int, int>;
    std::priority_queue<P, std::vector<P>, std::greater<>> pq;
    dist[src] = 0;
    pq.push({0, src});
    while (!pq.empty()) {
        auto [d, u] = pq.top();
        pq.pop();
        if (d > dist[u]) continue;
        for (auto [v, w] : adj[u])
            if (d + w < dist[v]) pq.push({dist[v] = d + w, v});
    }
    for (int& d : dist)
        if (d == INT_MAX) d = -1;
    return dist;
}

static void test_small_graph() {
    Graph g(4);
    g[0] = {{1, 1}, {2, 0}};
    g[2] = {{1, 0}, {3, 1}};
    g[1] = {{3, 1}};
    assert((zeroOneBfs(g, 0) == std::vector<int>{0, 0, 0, 1}));
}

static void test_zero_edge_found_late() {
    // 0 -1-> 1 is seen first, but 0 -1-> 2 -0-> 1 is not better; 0 -0-> 3 -0-> 1 is.
    Graph g(4);
    g[0] = {{1, 1}, {2, 1}, {3, 0}};
    g[2] = {{1, 0}};
    g[3] = {{1, 0}};
    assert((zeroOneBfs(g, 0) == std::vector<int>{0, 0, 1, 0}));
}

static void test_unreachable_and_single() {
    Graph g(3);
    g[0] = {{1, 1}};
    assert((zeroOneBfs(g, 0) == std::vector<int>{0, 1, -1}));
    assert((zeroOneBfs(Graph(1), 0) == std::vector<int>{0}));
}

static void test_invalid_input() {
    Graph g(2);
    g[0] = {{1, 2}};
    bool threw = false;
    try { zeroOneBfs(g, 0); } catch (const std::invalid_argument&) { threw = true; }
    assert(threw);
    threw = false;
    try { zeroOneBfs(g, 5); } catch (const std::out_of_range&) { threw = true; }
    assert(threw);
}

static void test_zero_weight_cycle() {
    Graph g(3);
    g[0] = {{1, 0}};
    g[1] = {{0, 0}, {2, 1}};
    assert((zeroOneBfs(g, 0) == std::vector<int>{0, 0, 1}));
}

static void test_random_against_dijkstra() {
    std::mt19937 rng(91);
    for (int t = 0; t < 300; ++t) {
        int n = static_cast<int>(rng() % 30) + 1;
        Graph g(n);
        int m = static_cast<int>(rng() % (n * 4 + 1));
        for (int i = 0; i < m; ++i)
            g[rng() % n].push_back({static_cast<int>(rng() % n), static_cast<int>(rng() % 2)});
        int src = static_cast<int>(rng() % n);
        assert(zeroOneBfs(g, src) == dijkstra(g, src));
    }
}

static void test_walls_grid() {
    std::vector<std::string> open = {"...", "...", "..."};
    assert(minWallsToBreak(open) == 0);
    std::vector<std::string> wall = {"..#..", "..#..", "..#.."};
    assert(minWallsToBreak(wall) == 1);
    std::vector<std::string> detour = {".#...", ".#.#.", "...#."};
    assert(minWallsToBreak(detour) == 0);  // path around exists
    std::vector<std::string> blocked = {"#"};
    assert(minWallsToBreak(blocked) == 1);
}

int main() {
    void (*tests[])() = {test_small_graph, test_zero_edge_found_late, test_unreachable_and_single,
                         test_invalid_input, test_zero_weight_cycle, test_random_against_dijkstra,
                         test_walls_grid};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
