#pragma once
// Largest rectangle in a histogram with a monotonic stack, and its 2D extension:
// the largest all-ones rectangle in a binary matrix.

#include <algorithm>
#include <cstddef>
#include <string>
#include <vector>

inline long long largestRectangleArea(const std::vector<int>& h) {
    std::vector<std::size_t> st;  // indices with increasing heights
    long long best = 0;
    const std::size_t n = h.size();
    // A virtual bar of height 0 at index n flushes the stack at the end.
    for (std::size_t i = 0; i <= n; ++i) {
        int cur = i == n ? 0 : h[i];
        while (!st.empty() && h[st.back()] >= cur) {
            long long height = h[st.back()];
            st.pop_back();
            // The popped bar extends right up to i-1 and left to just after the new stack top.
            std::size_t left = st.empty() ? 0 : st.back() + 1;
            best = std::max(best, height * static_cast<long long>(i - left));
        }
        st.push_back(i);
    }
    return best;
}

// Treat each row as the base of a histogram of consecutive '1's above it.
inline long long maximalRectangle(const std::vector<std::string>& grid) {
    if (grid.empty()) return 0;
    std::vector<int> heights(grid[0].size(), 0);
    long long best = 0;
    for (const auto& row : grid) {
        for (std::size_t c = 0; c < row.size(); ++c) heights[c] = row[c] == '1' ? heights[c] + 1 : 0;
        best = std::max(best, largestRectangleArea(heights));
    }
    return best;
}
