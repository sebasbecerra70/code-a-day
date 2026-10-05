#pragma once
// Edmonds-Karp max flow: Ford-Fulkerson where each augmenting path is the shortest one
// (found by BFS) in the residual graph. Also extracts a minimum s-t cut.

#include <algorithm>
#include <climits>
#include <queue>
#include <stdexcept>
#include <utility>
#include <vector>

class MaxFlow {
public:
    explicit MaxFlow(int n) : g_(n) {}

    // Each edge stores the index of its reverse edge, so pushing flow updates both in O(1).
    void addEdge(int u, int v, long long cap) {
        if (cap < 0) throw std::invalid_argument("negative capacity");
        if (u < 0 || v < 0 || u >= size() || v >= size()) throw std::out_of_range("vertex out of range");
        g_[u].push_back({v, cap, cap, static_cast<int>(g_[v].size())});
        g_[v].push_back({u, 0, 0, static_cast<int>(g_[u].size()) - 1});
    }

    long long maxflow(int s, int t) {
        if (s == t) throw std::invalid_argument("source equals sink");
        long long flow = 0;
        while (true) {
            // BFS for the shortest augmenting path; remember the edge used to reach each vertex.
            std::vector<std::pair<int, int>> prev(size(), {-1, -1});  // (vertex, edge index)
            std::queue<int> q;
            q.push(s);
            prev[s] = {s, -1};
            while (!q.empty() && prev[t].first == -1) {
                int u = q.front();
                q.pop();
                for (int i = 0; i < static_cast<int>(g_[u].size()); ++i) {
                    const Edge& e = g_[u][i];
                    if (e.cap > 0 && prev[e.to].first == -1) {
                        prev[e.to] = {u, i};
                        q.push(e.to);
                    }
                }
            }
            if (prev[t].first == -1) return flow;  // no augmenting path left

            long long bottleneck = LLONG_MAX;
            for (int v = t; v != s; v = prev[v].first)
                bottleneck = std::min(bottleneck, g_[prev[v].first][prev[v].second].cap);
            for (int v = t; v != s; v = prev[v].first) {
                Edge& e = g_[prev[v].first][prev[v].second];
                e.cap -= bottleneck;
                g_[e.to][e.rev].cap += bottleneck;  // residual capacity lets later paths undo flow
            }
            flow += bottleneck;
        }
    }

    // Call after maxflow(). Vertices reachable from s in the residual graph form the source
    // side of a minimum cut; the cut is every original edge leaving that side.
    std::vector<std::pair<int, int>> minCut(int s) const {
        std::vector<bool> sourceSide(size(), false);
        std::queue<int> q;
        q.push(s);
        sourceSide[s] = true;
        while (!q.empty()) {
            int u = q.front();
            q.pop();
            for (const Edge& e : g_[u])
                if (e.cap > 0 && !sourceSide[e.to]) {
                    sourceSide[e.to] = true;
                    q.push(e.to);
                }
        }
        std::vector<std::pair<int, int>> cut;
        for (int u = 0; u < size(); ++u)
            for (const Edge& e : g_[u])
                if (sourceSide[u] && !sourceSide[e.to] && e.original > 0) cut.push_back({u, e.to});
        return cut;
    }

    int size() const { return static_cast<int>(g_.size()); }

private:
    struct Edge {
        int to;
        long long cap;       // residual capacity
        long long original;  // capacity as added (0 for reverse edges)
        int rev;             // index of the reverse edge in g_[to]
    };

    std::vector<std::vector<Edge>> g_;
};
