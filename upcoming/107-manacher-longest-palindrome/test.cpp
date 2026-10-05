#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <random>

static bool isPal(const std::string& s, std::size_t i, std::size_t len) {
    for (std::size_t a = i, b = i + len - 1; a < b; ++a, --b)
        if (s[a] != s[b]) return false;
    return true;
}

static void test_examples() {
    std::string r = longestPalindrome("babad");
    assert(r == "bab");  // leftmost of "bab"/"aba"
    assert(longestPalindrome("cbbd") == "bb");
    assert(longestPalindrome("forgeeksskeegfor") == "geeksskeeg");
}

static void test_edge_cases() {
    assert(longestPalindrome("").empty());
    assert(longestPalindrome("z") == "z");
    assert(longestPalindrome("ab") == "a");
    assert(longestPalindrome("aaaa") == "aaaa");
    assert(longestPalindrome("racecar") == "racecar");
}

static void test_radii_values() {
    auto [d1, d2] = manacher("abacaba");
    assert((d1 == std::vector<int>{1, 2, 1, 4, 1, 2, 1}));
    assert((d2 == std::vector<int>{0, 0, 0, 0, 0, 0, 0}));
    auto r = manacher("abba");
    assert((r.d2 == std::vector<int>{0, 0, 2, 0}));
}

static void test_count_substrings() {
    assert(countPalindromicSubstrings("abc") == 3);
    assert(countPalindromicSubstrings("aaa") == 6);
    assert(countPalindromicSubstrings("") == 0);
}

static void test_random_against_brute() {
    std::mt19937 rng(107);
    for (int t = 0; t < 2000; ++t) {
        std::string s(rng() % 25, 'a');
        for (auto& c : s) c = static_cast<char>('a' + rng() % 3);
        std::size_t bestLen = 0, bestStart = 0;
        long long count = 0;
        for (std::size_t i = 0; i < s.size(); ++i)
            for (std::size_t len = 1; i + len <= s.size(); ++len)
                if (isPal(s, i, len)) {
                    ++count;
                    if (len > bestLen) { bestLen = len; bestStart = i; }
                }
        std::string got = longestPalindrome(s);
        assert(got.size() == bestLen);
        assert(got == s.substr(bestStart, bestLen));
        assert(countPalindromicSubstrings(s) == count);
    }
}

static void test_long_uniform_string_is_linear() {
    std::string s(200000, 'a');
    assert(longestPalindrome(s).size() == s.size());
    assert(countPalindromicSubstrings(s) == 200000LL * 200001 / 2);
}

int main() {
    void (*tests[])() = {test_examples, test_edge_cases, test_radii_values, test_count_substrings,
                         test_random_against_brute, test_long_uniform_string_is_linear};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
