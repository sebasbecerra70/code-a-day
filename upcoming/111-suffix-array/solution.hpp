#pragma once
// Suffix array by prefix doubling in O(n log n) (counting sort per round), the LCP array
// by Kasai's algorithm in O(n), and two applications built on them.

#include <algorithm>
#include <cstddef>
#include <string>
#include <string_view>
#include <vector>

// sa[i] = start index of the i-th smallest suffix of text. text must not contain '\0'.
inline std::vector<int> suffixArray(std::string_view text) {
    // Append a sentinel smaller than every character so all suffixes are distinct
    // in rank and the cyclic shifts we sort behave like suffixes.
    std::string s(text);
    s.push_back('\0');
    const int n = static_cast<int>(s.size());
    const int alphabet = 256;
    std::vector<int> sa(n), rank(n), tmp(n), cnt(std::max(alphabet, n), 0);

    // Round 0: sort by first character.
    for (char c : s) ++cnt[static_cast<unsigned char>(c)];
    for (int i = 1; i < alphabet; ++i) cnt[i] += cnt[i - 1];
    for (int i = n - 1; i >= 0; --i) sa[--cnt[static_cast<unsigned char>(s[i])]] = i;
    rank[sa[0]] = 0;
    int classes = 1;
    for (int i = 1; i < n; ++i) {
        if (s[sa[i]] != s[sa[i - 1]]) ++classes;
        rank[sa[i]] = classes - 1;
    }

    // Round k: sort cyclic shifts of length 2^(k+1) by the pair (rank[i], rank[i + 2^k]).
    for (int len = 1; len < n && classes < n; len <<= 1) {
        // Sorting by second key is free: shifting the previous order left by len does it.
        for (int i = 0; i < n; ++i) tmp[i] = (sa[i] - len + n) % n;
        // Stable counting sort by first key.
        std::fill(cnt.begin(), cnt.begin() + classes, 0);
        for (int i = 0; i < n; ++i) ++cnt[rank[tmp[i]]];
        for (int i = 1; i < classes; ++i) cnt[i] += cnt[i - 1];
        for (int i = n - 1; i >= 0; --i) sa[--cnt[rank[tmp[i]]]] = tmp[i];
        // Recompute equivalence classes.
        tmp[sa[0]] = 0;
        classes = 1;
        for (int i = 1; i < n; ++i) {
            int a = sa[i], b = sa[i - 1];
            if (rank[a] != rank[b] || rank[(a + len) % n] != rank[(b + len) % n]) ++classes;
            tmp[a] = classes - 1;
        }
        rank.swap(tmp);
    }
    sa.erase(sa.begin());  // drop the sentinel suffix, which is always first
    return sa;
}

// lcp[i] = longest common prefix of suffixes sa[i] and sa[i+1] (size n-1).
// Kasai: walking suffixes in text order, the LCP drops by at most 1 each step.
inline std::vector<int> lcpArray(std::string_view s, const std::vector<int>& sa) {
    const int n = static_cast<int>(s.size());
    if (n == 0) return {};
    std::vector<int> rank(n), lcp(n - 1, 0);
    for (int i = 0; i < n; ++i) rank[sa[i]] = i;
    int h = 0;
    for (int i = 0; i < n; ++i) {
        if (rank[i] == n - 1) { h = 0; continue; }
        int j = sa[rank[i] + 1];
        while (i + h < n && j + h < n && s[i + h] == s[j + h]) ++h;
        lcp[rank[i]] = h;
        if (h > 0) --h;
    }
    return lcp;
}

// Number of distinct non-empty substrings: all prefixes of all suffixes, minus shared ones.
inline long long countDistinctSubstrings(std::string_view s) {
    auto sa = suffixArray(s);
    auto lcp = lcpArray(s, sa);
    long long n = static_cast<long long>(s.size());
    long long total = n * (n + 1) / 2;
    for (int x : lcp) total -= x;
    return total;
}

// Longest substring that occurs at least twice (occurrences may overlap).
inline std::string longestRepeatedSubstring(std::string_view s) {
    auto sa = suffixArray(s);
    auto lcp = lcpArray(s, sa);
    int best = 0, at = 0;
    for (std::size_t i = 0; i < lcp.size(); ++i)
        if (lcp[i] > best) { best = lcp[i]; at = sa[i]; }
    return std::string(s.substr(at, best));
}
