#pragma once
// Lowest common ancestor on a rooted tree with binary lifting:
// up[j][v] is the 2^j-th ancestor of v. O(n log n) preprocessing, O(log n) per query.

#include <cstddef>
#include <stdexcept>
#include <utility>
#include <vector>

class LCA {
public:
    // `edges` are undirected tree edges over vertices 0..n-1.
    LCA(int n, const std::vector<std::pair<int, int>>& edges, int root = 0)
        : n_(n), depth_(n, 0) {
        if (n <= 0 || root < 0 || root >= n) throw std::invalid_argument("bad tree");
        if (static_cast<int>(edges.size()) != n - 1) throw std::invalid_argument("a tree has n-1 edges");
        std::vector<std::vector<int>> adj(n);
        for (auto [u, v] : edges) {
            adj[u].push_back(v);
            adj[v].push_back(u);
        }
        while ((1 << log_) < n) ++log_;
        up_.assign(log_ + 1, std::vector<int>(n, root));

        // Iterative DFS (avoids stack overflow on path-shaped trees) to set parent and depth.
        std::vector<bool> seen(n, false);
        std::vector<int> stack{root};
        seen[root] = true;
        int visited = 0;
        while (!stack.empty()) {
            int u = stack.back();
            stack.pop_back();
            ++visited;
            for (int v : adj[u]) {
                if (seen[v]) continue;
                seen[v] = true;
                up_[0][v] = u;
                depth_[v] = depth_[u] + 1;
                stack.push_back(v);
            }
        }
        if (visited != n) throw std::invalid_argument("graph is not connected");

        // The 2^j-th ancestor is the 2^(j-1)-th ancestor of the 2^(j-1)-th ancestor.
        for (int j = 1; j <= log_; ++j)
            for (int v = 0; v < n; ++v) up_[j][v] = up_[j - 1][up_[j - 1][v]];
    }

    int depth(int v) const { return depth_[v]; }

    // k-th ancestor of v, or -1 if it doesn't exist. Jump by each set bit of k.
    int kthAncestor(int v, int k) const {
        if (k > depth_[v]) return -1;
        for (int j = 0; k; ++j, k >>= 1)
            if (k & 1) v = up_[j][v];
        return v;
    }

    int query(int a, int b) const {
        check(a);
        check(b);
        if (depth_[a] < depth_[b]) std::swap(a, b);
        a = kthAncestor(a, depth_[a] - depth_[b]);  // lift a to b's depth
        if (a == b) return a;
        // Jump both as high as possible while they stay different; they end just below the LCA.
        for (int j = log_; j >= 0; --j) {
            if (up_[j][a] != up_[j][b]) {
                a = up_[j][a];
                b = up_[j][b];
            }
        }
        return up_[0][a];
    }

    int distance(int a, int b) const { return depth_[a] + depth_[b] - 2 * depth_[query(a, b)]; }

private:
    void check(int v) const {
        if (v < 0 || v >= n_) throw std::out_of_range("vertex out of range");
    }

    int n_;
    int log_ = 0;
    std::vector<int> depth_;
    std::vector<std::vector<int>> up_;  // root's ancestor is itself
};
