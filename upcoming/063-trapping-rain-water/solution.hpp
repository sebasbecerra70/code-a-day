#pragma once
// Trapping rain water three ways: two pointers (O(1) space), monotonic stack,
// and the 2D version with a min-heap flood fill.

#include <algorithm>
#include <cstddef>
#include <functional>
#include <queue>
#include <tuple>
#include <vector>

// Water above bar i = min(maxLeft, maxRight) - h[i]. Move the side with the smaller max:
// that side's bound is already decided because the other side is at least as tall.
inline long long trapTwoPointers(const std::vector<int>& h) {
    if (h.empty()) return 0;
    std::size_t l = 0, r = h.size() - 1;
    int leftMax = 0, rightMax = 0;
    long long water = 0;
    while (l < r) {
        if (h[l] < h[r]) {
            leftMax = std::max(leftMax, h[l]);
            water += leftMax - h[l];
            ++l;
        } else {
            rightMax = std::max(rightMax, h[r]);
            water += rightMax - h[r];
            --r;
        }
    }
    return water;
}

// Fills water layer by layer: when a taller bar arrives, the popped bar is a basin floor
// bounded by the new stack top (left wall) and the current bar (right wall).
inline long long trapStack(const std::vector<int>& h) {
    std::vector<std::size_t> st;
    long long water = 0;
    for (std::size_t i = 0; i < h.size(); ++i) {
        while (!st.empty() && h[st.back()] < h[i]) {
            int floor = h[st.back()];
            st.pop_back();
            if (st.empty()) break;  // no left wall
            std::size_t left = st.back();
            long long width = static_cast<long long>(i - left - 1);
            water += width * (std::min(h[left], h[i]) - floor);
        }
        st.push_back(i);
    }
    return water;
}

// 2D: water can escape through the border. Grow inward from the lowest boundary cell
// with a min-heap; a neighbor lower than the current water level holds the difference.
inline long long trap2D(const std::vector<std::vector<int>>& g) {
    if (g.size() < 3 || g[0].size() < 3) return 0;
    const std::size_t R = g.size(), C = g[0].size();
    using Cell = std::tuple<int, std::size_t, std::size_t>;  // (level, r, c)
    std::priority_queue<Cell, std::vector<Cell>, std::greater<>> pq;
    std::vector<std::vector<bool>> seen(R, std::vector<bool>(C, false));
    for (std::size_t r = 0; r < R; ++r)
        for (std::size_t c = 0; c < C; ++c)
            if (r == 0 || c == 0 || r == R - 1 || c == C - 1) {
                pq.emplace(g[r][c], r, c);
                seen[r][c] = true;
            }
    long long water = 0;
    const int dr[] = {1, -1, 0, 0}, dc[] = {0, 0, 1, -1};
    while (!pq.empty()) {
        auto [level, r, c] = pq.top();
        pq.pop();
        for (int k = 0; k < 4; ++k) {
            std::size_t nr = r + dr[k], nc = c + dc[k];  // unsigned wrap is caught by bounds check
            if (nr >= R || nc >= C || seen[nr][nc]) continue;
            seen[nr][nc] = true;
            water += std::max(0, level - g[nr][nc]);
            pq.emplace(std::max(level, g[nr][nc]), nr, nc);
        }
    }
    return water;
}
