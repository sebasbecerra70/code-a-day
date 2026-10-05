#pragma once
// Monotonic stack patterns: next greater element (linear and circular),
// daily temperatures, and the stock span problem.

#include <cstddef>
#include <utility>
#include <vector>

// For each i, the value of the first element to the right that is strictly greater, or -1.
inline std::vector<int> nextGreater(const std::vector<int>& a) {
    std::vector<int> ans(a.size(), -1);
    std::vector<std::size_t> st;  // indices whose answer is still unknown; values decrease bottom->top
    for (std::size_t i = 0; i < a.size(); ++i) {
        while (!st.empty() && a[st.back()] < a[i]) {
            ans[st.back()] = a[i];
            st.pop_back();
        }
        st.push_back(i);
    }
    return ans;
}

// Same, but the array wraps around: walk it twice and only push indices on the first pass.
inline std::vector<int> nextGreaterCircular(const std::vector<int>& a) {
    const std::size_t n = a.size();
    std::vector<int> ans(n, -1);
    std::vector<std::size_t> st;
    for (std::size_t k = 0; k < 2 * n; ++k) {
        std::size_t i = k % n;
        while (!st.empty() && a[st.back()] < a[i]) {
            ans[st.back()] = a[i];
            st.pop_back();
        }
        if (k < n) st.push_back(i);
    }
    return ans;
}

// Days to wait until a warmer temperature (0 if never).
inline std::vector<int> dailyTemperatures(const std::vector<int>& t) {
    std::vector<int> ans(t.size(), 0);
    std::vector<std::size_t> st;
    for (std::size_t i = 0; i < t.size(); ++i) {
        while (!st.empty() && t[st.back()] < t[i]) {
            ans[st.back()] = static_cast<int>(i - st.back());
            st.pop_back();
        }
        st.push_back(i);
    }
    return ans;
}

// Stock span: number of consecutive days ending today with price <= today's price.
// Online: call next() once per day.
class StockSpanner {
public:
    int next(int price) {
        int span = 1;
        // Collapse every previous day with price <= today; their spans are absorbed.
        while (!st_.empty() && st_.back().first <= price) {
            span += st_.back().second;
            st_.pop_back();
        }
        st_.push_back({price, span});
        return span;
    }

private:
    std::vector<std::pair<int, int>> st_;  // (price, span), prices strictly decreasing
};
