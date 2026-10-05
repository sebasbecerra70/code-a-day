#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <random>

using SV = std::vector<std::string_view>;
using VS = std::vector<std::string>;

template <typename F>
static bool throws(F f) {
    try { f(); } catch (const std::invalid_argument&) { return true; }
    return false;
}

static void test_split_basic() {
    assert((split("a,b,c", ",") == SV{"a", "b", "c"}));
    assert((split("a, b;c", ", ;") == SV{"a", "b", "c"}));
}

static void test_split_empty_fields() {
    assert((split(",a,,b,", ",") == SV{"a", "b"}));
    assert((split(",a,,b,", ",", true) == SV{"", "a", "", "b", ""}));
    assert(split("", ",").empty());
    assert((split("", ",", true) == SV{""}));
}

static void test_split_no_delimiter() {
    assert((split("hello", ",") == SV{"hello"}));
}

static void test_split_round_trip_random() {
    std::mt19937 rng(1);
    for (int t = 0; t < 500; ++t) {
        std::string s;
        int len = static_cast<int>(rng() % 20);
        for (int i = 0; i < len; ++i) s += "ab,"[rng() % 3];
        auto parts = split(s, ",", true);
        std::string joined;
        for (std::size_t i = 0; i < parts.size(); ++i) {
            if (i) joined += ',';
            joined += parts[i];
        }
        assert(joined == s);
    }
}

static void test_quoted() {
    assert((splitQuoted(R"(ls -la "my file.txt")") == VS{"ls", "-la", "my file.txt"}));
    assert((splitQuoted(R"(  a   b  )") == VS{"a", "b"}));
    assert((splitQuoted(R"(say "" x)") == VS{"say", "", "x"}));
    assert((splitQuoted(R"(a"b c"d)") == VS{"ab cd"}));
}

static void test_quoted_escapes_and_errors() {
    assert((splitQuoted(R"(a\ b \"q\")") == VS{"a b", "\"q\""}));
    assert(throws([] { splitQuoted(R"("open)"); }));
    assert(throws([] { splitQuoted("bad\\"); }));
    assert(splitQuoted("   ").empty());
}

static void test_lex_expression() {
    auto toks = lex("3.5*(x_1 + 42) ** 2");
    std::vector<Token> expected = {
        {TokenKind::Number, "3.5"},  {TokenKind::Operator, "*"},   {TokenKind::LParen, "("},
        {TokenKind::Identifier, "x_1"}, {TokenKind::Operator, "+"}, {TokenKind::Number, "42"},
        {TokenKind::RParen, ")"},    {TokenKind::Operator, "**"},  {TokenKind::Number, "2"}};
    assert(toks == expected);
}

static void test_lex_edge_cases() {
    assert(lex("").empty());
    assert(lex("   ").empty());
    auto t = lex(".5+1.2.3");  // "1.2" then ".3"
    assert(t.size() == 4 && t[0].text == ".5" && t[2].text == "1.2" && t[3].text == ".3");
    assert(throws([] { lex("1 $ 2"); }));
}

int main() {
    void (*tests[])() = {test_split_basic, test_split_empty_fields, test_split_no_delimiter,
                         test_split_round_trip_random, test_quoted, test_quoted_escapes_and_errors,
                         test_lex_expression, test_lex_edge_cases};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
