#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <numeric>
#include <random>
#include <set>

static std::vector<int> naiveSA(const std::string& s) {
    std::vector<int> sa(s.size());
    std::iota(sa.begin(), sa.end(), 0);
    std::sort(sa.begin(), sa.end(), [&](int a, int b) { return s.compare(a, std::string::npos, s, b, std::string::npos) < 0; });
    return sa;
}

static void test_banana() {
    std::string s = "banana";
    auto sa = suffixArray(s);
    assert((sa == std::vector<int>{5, 3, 1, 0, 4, 2}));  // a, ana, anana, banana, na, nana
    assert((lcpArray(s, sa) == std::vector<int>{1, 3, 0, 0, 2}));
}

static void test_empty_and_single() {
    assert(suffixArray("").empty());
    assert((suffixArray("x") == std::vector<int>{0}));
    assert(lcpArray("x", {0}).empty());
}

static void test_repeated_character() {
    assert((suffixArray("aaaa") == std::vector<int>{3, 2, 1, 0}));
    assert((lcpArray("aaaa", {3, 2, 1, 0}) == std::vector<int>{1, 2, 3}));
}

static void test_random_against_naive() {
    std::mt19937 rng(111);
    for (int t = 0; t < 1000; ++t) {
        std::string s(rng() % 40, 'a');
        for (auto& c : s) c = static_cast<char>('a' + rng() % 3);
        auto sa = suffixArray(s);
        assert(sa == naiveSA(s));
        auto lcp = lcpArray(s, sa);
        for (std::size_t i = 0; i + 1 < sa.size(); ++i) {
            int h = 0;
            while (sa[i] + h < static_cast<int>(s.size()) && sa[i + 1] + h < static_cast<int>(s.size()) &&
                   s[sa[i] + h] == s[sa[i + 1] + h])
                ++h;
            assert(lcp[i] == h);
        }
    }
}

static void test_distinct_substrings() {
    assert(countDistinctSubstrings("abc") == 6);
    assert(countDistinctSubstrings("aaa") == 3);
    assert(countDistinctSubstrings("") == 0);
    std::mt19937 rng(112);
    for (int t = 0; t < 200; ++t) {
        std::string s(rng() % 20, 'a');
        for (auto& c : s) c = static_cast<char>('a' + rng() % 2);
        std::set<std::string> all;
        for (std::size_t i = 0; i < s.size(); ++i)
            for (std::size_t j = i + 1; j <= s.size(); ++j) all.insert(s.substr(i, j - i));
        assert(countDistinctSubstrings(s) == static_cast<long long>(all.size()));
    }
}

static void test_longest_repeated() {
    assert(longestRepeatedSubstring("banana") == "ana");
    assert(longestRepeatedSubstring("abcd").empty());
    assert(longestRepeatedSubstring("aaaa") == "aaa");  // overlapping occurrences allowed
}

static void test_binary_and_high_chars() {
    std::string s = "b\xff" "a\x01" "b\xff";
    assert(suffixArray(s) == naiveSA(s));
}

static void test_large_input_runs_fast() {
    std::string s;
    std::mt19937 rng(113);
    for (int i = 0; i < 200000; ++i) s += static_cast<char>('a' + rng() % 4);
    auto sa = suffixArray(s);
    for (std::size_t i = 0; i + 1 < sa.size(); i += 997)
        assert(s.compare(sa[i], std::string::npos, s, sa[i + 1], std::string::npos) < 0);
}

int main() {
    void (*tests[])() = {test_banana, test_empty_and_single, test_repeated_character,
                         test_random_against_naive, test_distinct_substrings,
                         test_longest_repeated, test_binary_and_high_chars,
                         test_large_input_runs_fast};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
