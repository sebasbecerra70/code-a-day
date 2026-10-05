#pragma once
// Manacher's algorithm: palindrome radii around every center in O(n).
// d1[i] = number of odd palindromes centered at i   (radius incl. center: "aba" at b -> 2)
// d2[i] = number of even palindromes centered between i-1 and i ("abba" at 2nd b -> 2)

#include <algorithm>
#include <cstddef>
#include <string>
#include <string_view>
#include <vector>

struct PalindromeRadii {
    std::vector<int> d1, d2;
};

inline PalindromeRadii manacher(std::string_view s) {
    const int n = static_cast<int>(s.size());
    PalindromeRadii r{std::vector<int>(n), std::vector<int>(n)};

    // Odd lengths. [l, r] is the rightmost palindrome found so far.
    for (int i = 0, l = 0, rr = -1; i < n; ++i) {
        // Mirror position j = l + rr - i has a known radius; it's valid up to the window edge.
        int k = i > rr ? 1 : std::min(r.d1[l + rr - i], rr - i + 1);
        while (i - k >= 0 && i + k < n && s[i - k] == s[i + k]) ++k;
        r.d1[i] = k;
        if (i + k - 1 > rr) {
            l = i - k + 1;
            rr = i + k - 1;
        }
    }
    // Even lengths, same idea with the center between i-1 and i.
    for (int i = 0, l = 0, rr = -1; i < n; ++i) {
        int k = i > rr ? 0 : std::min(r.d2[l + rr - i + 1], rr - i + 1);
        while (i - k - 1 >= 0 && i + k < n && s[i - k - 1] == s[i + k]) ++k;
        r.d2[i] = k;
        if (i + k - 1 > rr) {
            l = i - k;
            rr = i + k - 1;
        }
    }
    return r;
}

// Longest palindromic substring (leftmost on ties).
inline std::string longestPalindrome(std::string_view s) {
    if (s.empty()) return "";
    auto [d1, d2] = manacher(s);
    int bestLen = 0, bestStart = 0;
    for (int i = 0; i < static_cast<int>(s.size()); ++i) {
        int odd = 2 * d1[i] - 1, even = 2 * d2[i];
        if (odd > bestLen) { bestLen = odd; bestStart = i - d1[i] + 1; }
        if (even > bestLen) { bestLen = even; bestStart = i - d2[i]; }
    }
    return std::string(s.substr(bestStart, bestLen));
}

// Number of palindromic substrings (counted by position): each radius is a count.
inline long long countPalindromicSubstrings(std::string_view s) {
    auto [d1, d2] = manacher(s);
    long long total = 0;
    for (std::size_t i = 0; i < s.size(); ++i) total += d1[i] + d2[i];
    return total;
}
