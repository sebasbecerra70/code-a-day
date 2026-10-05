#pragma once
// 0-1 BFS: single-source shortest paths when every edge weight is 0 or 1, using a deque
// instead of a priority queue. Plus a grid application: the minimum number of wall
// cells to break to get from one corner to the other.

#include <climits>
#include <deque>
#include <stdexcept>
#include <string>
#include <vector>

struct Edge {
    int to;
    int w;  // 0 or 1
};

// Returns dist[v], or -1 if v is unreachable.
inline std::vector<int> zeroOneBfs(const std::vector<std::vector<Edge>>& adj, int src) {
    const int n = static_cast<int>(adj.size());
    if (src < 0 || src >= n) throw std::out_of_range("bad source");
    std::vector<int> dist(n, INT_MAX);
    std::deque<int> dq;
    dist[src] = 0;
    dq.push_back(src);
    // The deque holds at most two distinct distances, d and d+1, in sorted order,
    // so popping from the front always yields a minimum, like Dijkstra.
    while (!dq.empty()) {
        int u = dq.front();
        dq.pop_front();
        for (auto [v, w] : adj[u]) {
            if (w != 0 && w != 1) throw std::invalid_argument("weights must be 0 or 1");
            if (dist[u] + w < dist[v]) {
                dist[v] = dist[u] + w;
                if (w == 0) dq.push_front(v);
                else dq.push_back(v);
            }
        }
    }
    for (int& d : dist)
        if (d == INT_MAX) d = -1;
    return dist;
}

// Grid of '.' (free) and '#' (wall). Moving into a wall costs 1 (you break it), into
// a free cell costs 0. Returns the minimum walls broken from top-left to bottom-right.
inline int minWallsToBreak(const std::vector<std::string>& grid) {
    if (grid.empty() || grid[0].empty()) throw std::invalid_argument("empty grid");
    const int R = static_cast<int>(grid.size()), C = static_cast<int>(grid[0].size());
    std::vector<std::vector<Edge>> adj(R * C);
    const int dr[] = {1, -1, 0, 0}, dc[] = {0, 0, 1, -1};
    for (int r = 0; r < R; ++r)
        for (int c = 0; c < C; ++c)
            for (int k = 0; k < 4; ++k) {
                int nr = r + dr[k], nc = c + dc[k];
                if (nr < 0 || nr >= R || nc < 0 || nc >= C) continue;
                adj[r * C + c].push_back({nr * C + nc, grid[nr][nc] == '#' ? 1 : 0});
            }
    int startCost = grid[0][0] == '#' ? 1 : 0;
    return startCost + zeroOneBfs(adj, 0)[R * C - 1];
}
