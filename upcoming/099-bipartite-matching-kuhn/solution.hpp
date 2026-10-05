#pragma once
// Kuhn's algorithm: maximum matching in a bipartite graph by repeatedly searching for
// augmenting paths with DFS. Left vertices 0..L-1, right vertices 0..R-1.

#include <stdexcept>
#include <utility>
#include <vector>

class BipartiteMatcher {
public:
    BipartiteMatcher(int left, int right) : adj_(left), right_(right) {}

    void addEdge(int l, int r) {
        if (l < 0 || l >= static_cast<int>(adj_.size()) || r < 0 || r >= right_)
            throw std::out_of_range("vertex out of range");
        adj_[l].push_back(r);
    }

    // Returns the size of a maximum matching.
    int solve() {
        const int L = static_cast<int>(adj_.size());
        matchL_.assign(L, -1);
        matchR_.assign(right_, -1);
        int size = 0;
        // Greedy warm start: cheap and often matches most vertices immediately.
        for (int l = 0; l < L; ++l)
            for (int r : adj_[l])
                if (matchR_[r] == -1) {
                    matchL_[l] = r;
                    matchR_[r] = l;
                    ++size;
                    break;
                }
        for (int l = 0; l < L; ++l) {
            if (matchL_[l] != -1) continue;
            visited_.assign(right_, false);
            if (tryAugment(l)) ++size;
        }
        return size;
    }

    std::vector<std::pair<int, int>> pairs() const {
        std::vector<std::pair<int, int>> out;
        for (int l = 0; l < static_cast<int>(matchL_.size()); ++l)
            if (matchL_[l] != -1) out.push_back({l, matchL_[l]});
        return out;
    }

private:
    // Can left vertex l be matched, possibly by re-matching others along an alternating path?
    bool tryAugment(int l) {
        for (int r : adj_[l]) {
            if (visited_[r]) continue;
            visited_[r] = true;
            // r is free, or its current partner can move to another right vertex.
            if (matchR_[r] == -1 || tryAugment(matchR_[r])) {
                matchL_[l] = r;
                matchR_[r] = l;
                return true;
            }
        }
        return false;
    }

    std::vector<std::vector<int>> adj_;
    int right_;
    std::vector<int> matchL_, matchR_;
    std::vector<bool> visited_;
};
