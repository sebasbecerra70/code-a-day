#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <random>

using Z = std::vector<std::size_t>;

static Z bruteZ(const std::string& s) {
    Z z(s.size(), 0);
    for (std::size_t i = 0; i < s.size(); ++i)
        while (i + z[i] < s.size() && s[z[i]] == s[i + z[i]]) ++z[i];
    return z;
}

static std::vector<std::size_t> bruteFind(const std::string& t, const std::string& p) {
    std::vector<std::size_t> out;
    if (p.empty()) return out;
    for (std::size_t i = 0; i + p.size() <= t.size(); ++i)
        if (t.compare(i, p.size(), p) == 0) out.push_back(i);
    return out;
}

static void test_known_z_arrays() {
    assert((zFunction("aaaaa") == Z{5, 4, 3, 2, 1}));
    assert((zFunction("aaabaab") == Z{7, 2, 1, 0, 2, 1, 0}));
    assert((zFunction("abacaba") == Z{7, 0, 1, 0, 3, 0, 1}));
}

static void test_empty_and_single() {
    assert(zFunction("").empty());
    assert((zFunction("x") == Z{1}));
}

static void test_random_z_against_brute() {
    std::mt19937 rng(103);
    for (int t = 0; t < 2000; ++t) {
        std::string s(rng() % 30, 'a');
        for (auto& c : s) c = static_cast<char>('a' + rng() % 3);
        assert(zFunction(s) == bruteZ(s));
    }
}

static void test_find_all_overlapping() {
    assert((findAll("aaaa", "aa") == std::vector<std::size_t>{0, 1, 2}));
    assert((findAll("abracadabra", "abra") == std::vector<std::size_t>{0, 7}));
}

static void test_find_all_edge_cases() {
    assert(findAll("abc", "").empty());
    assert(findAll("ab", "abc").empty());
    assert((findAll("abc", "abc") == std::vector<std::size_t>{0}));
    assert(findAll("", "a").empty());
}

static void test_find_all_random() {
    std::mt19937 rng(104);
    for (int t = 0; t < 2000; ++t) {
        std::string text(rng() % 40, 'a'), pat(rng() % 4 + 1, 'a');
        for (auto& c : text) c = static_cast<char>('a' + rng() % 2);
        for (auto& c : pat) c = static_cast<char>('a' + rng() % 2);
        assert(findAll(text, pat) == bruteFind(text, pat));
    }
}

static void test_smallest_period() {
    assert(smallestPeriod("abcabcabc") == 3);
    assert(smallestPeriod("aaaa") == 1);
    assert(smallestPeriod("abcab") == 5);  // not a full repetition
    assert(smallestPeriod("abab") == 2);
    assert(smallestPeriod("") == 0);
}

int main() {
    void (*tests[])() = {test_known_z_arrays, test_empty_and_single, test_random_z_against_brute,
                         test_find_all_overlapping, test_find_all_edge_cases,
                         test_find_all_random, test_smallest_period};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
