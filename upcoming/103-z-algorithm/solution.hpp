#pragma once
// Z-function: z[i] = length of the longest common prefix of s and s.substr(i).
// Computed in O(n) by reusing the rightmost match window [l, r).
// Applications: all occurrences of a pattern, and the smallest period of a string.

#include <algorithm>
#include <cstddef>
#include <string>
#include <string_view>
#include <vector>

inline std::vector<std::size_t> zFunction(std::string_view s) {
    const std::size_t n = s.size();
    std::vector<std::size_t> z(n, 0);
    if (n == 0) return z;
    z[0] = n;  // by convention the whole string matches itself
    std::size_t l = 0, r = 0;  // s[l, r) == s[0, r - l), with r as far right as found so far
    for (std::size_t i = 1; i < n; ++i) {
        // Inside the window, s[i..] starts like s[i-l..], so reuse z[i-l] (capped at the window).
        if (i < r) z[i] = std::min(r - i, z[i - l]);
        // Extend by direct comparison; every successful step moves r right, so O(n) total.
        while (i + z[i] < n && s[z[i]] == s[i + z[i]]) ++z[i];
        if (i + z[i] > r) {
            l = i;
            r = i + z[i];
        }
    }
    return z;
}

// All start indices of `pattern` in `text`. No separator character is needed between
// pattern and text: z[i] >= |pattern| already means the pattern occurs at i.
inline std::vector<std::size_t> findAll(std::string_view text, std::string_view pattern) {
    std::vector<std::size_t> out;
    const std::size_t m = pattern.size();
    if (m == 0 || m > text.size()) return out;
    std::string combined;
    combined.reserve(m + text.size());
    combined.append(pattern).append(text);
    auto z = zFunction(combined);
    for (std::size_t i = m; i < combined.size(); ++i)
        if (z[i] >= m) out.push_back(i - m);
    return out;
}

// Smallest p such that s is a repetition of its prefix of length p (s = t^k).
inline std::size_t smallestPeriod(std::string_view s) {
    const std::size_t n = s.size();
    auto z = zFunction(s);
    for (std::size_t p = 1; p < n; ++p)
        if (n % p == 0 && p + z[p] == n) return p;
    return n;
}
