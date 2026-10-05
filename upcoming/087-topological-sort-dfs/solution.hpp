#pragma once
// Topological ordering of a directed graph via DFS post-order, with three-color cycle
// detection that also reports one concrete cycle. Kahn's algorithm is included for comparison.

#include <algorithm>
#include <functional>
#include <optional>
#include <queue>
#include <stdexcept>
#include <utility>
#include <vector>

struct TopoResult {
    std::vector<int> order;  // valid only if cycle is empty
    std::vector<int> cycle;  // e.g. {2, 5, 3, 2} if a cycle exists
    bool ok() const { return cycle.empty(); }
};

inline TopoResult topoSortDFS(int n, const std::vector<std::pair<int, int>>& edges) {
    std::vector<std::vector<int>> adj(n);
    for (auto [u, v] : edges) {
        if (u < 0 || u >= n || v < 0 || v >= n) throw std::out_of_range("edge endpoint out of range");
        adj[u].push_back(v);
    }
    enum Color : char { White, Gray, Black };  // unvisited, on current path, finished
    std::vector<Color> color(n, White);
    std::vector<int> parent(n, -1), post;
    post.reserve(n);

    // Iterative DFS: each frame is (vertex, index of next neighbor to try).
    std::vector<std::pair<int, std::size_t>> stack;
    for (int s = 0; s < n; ++s) {
        if (color[s] != White) continue;
        stack.push_back({s, 0});
        color[s] = Gray;
        while (!stack.empty()) {
            auto& [u, i] = stack.back();
            if (i < adj[u].size()) {
                int v = adj[u][i++];
                if (color[v] == White) {
                    color[v] = Gray;
                    parent[v] = u;
                    stack.push_back({v, 0});
                } else if (color[v] == Gray) {
                    // Back edge u -> v: walk parents from u back to v to recover the cycle.
                    std::vector<int> cyc{v};
                    for (int x = u; x != v; x = parent[x]) cyc.push_back(x);
                    cyc.push_back(v);
                    std::reverse(cyc.begin(), cyc.end());
                    return {{}, cyc};
                }
            } else {
                color[u] = Black;
                post.push_back(u);  // all descendants are done, so u can come before them
                stack.pop_back();
            }
        }
    }
    std::reverse(post.begin(), post.end());  // reverse post-order is a topological order
    return {post, {}};
}

// Kahn's algorithm (BFS on in-degree 0). Uses a min-heap so the result is the
// lexicographically smallest topological order. Returns nullopt on a cycle.
inline std::optional<std::vector<int>> topoSortKahn(int n, const std::vector<std::pair<int, int>>& edges) {
    std::vector<std::vector<int>> adj(n);
    std::vector<int> indeg(n, 0);
    for (auto [u, v] : edges) {
        adj[u].push_back(v);
        ++indeg[v];
    }
    std::priority_queue<int, std::vector<int>, std::greater<>> ready;
    for (int v = 0; v < n; ++v)
        if (indeg[v] == 0) ready.push(v);
    std::vector<int> order;
    while (!ready.empty()) {
        int u = ready.top();
        ready.pop();
        order.push_back(u);
        for (int v : adj[u])
            if (--indeg[v] == 0) ready.push(v);
    }
    if (static_cast<int>(order.size()) != n) return std::nullopt;
    return order;
}
