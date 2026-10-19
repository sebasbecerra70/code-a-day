#pragma once
// Three tokenizers of increasing difficulty:
//   split()      - split on a delimiter set, optionally keeping empty fields
//   splitQuoted()- shell-like split on whitespace honoring "double quotes" and \ escapes
//   lex()        - lexer for arithmetic expressions (numbers, identifiers, operators, parens)

#include <cctype>
#include <stdexcept>
#include <string>
#include <string_view>
#include <vector>

// Returns views into `s`, so `s` must outlive the result.
inline std::vector<std::string_view> split(std::string_view s, std::string_view delims,
                                           bool keepEmpty = false) {
    std::vector<std::string_view> out;
    std::size_t start = 0;
    while (true) {
        std::size_t pos = s.find_first_of(delims, start);
        std::string_view piece = s.substr(start, pos == std::string_view::npos ? pos : pos - start);
        if (keepEmpty || !piece.empty()) out.push_back(piece);
        if (pos == std::string_view::npos) break;
        start = pos + 1;
    }
    return out;
}

inline std::vector<std::string> splitQuoted(std::string_view s) {
    std::vector<std::string> out;
    std::string cur;
    bool inToken = false, inQuotes = false;
    for (std::size_t i = 0; i < s.size(); ++i) {
        char c = s[i];
        if (c == '\\') {
            if (i + 1 == s.size()) throw std::invalid_argument("dangling escape");
            cur += s[++i];
            inToken = true;
        } else if (c == '"') {
            inQuotes = !inQuotes;
            inToken = true;  // "" is a valid empty token
        } else if (!inQuotes && std::isspace(static_cast<unsigned char>(c))) {
            if (inToken) out.push_back(std::move(cur));
            cur.clear();
            inToken = false;
        } else {
            cur += c;
            inToken = true;
        }
    }
    if (inQuotes) throw std::invalid_argument("unterminated quote");
    if (inToken) out.push_back(std::move(cur));
    return out;
}

enum class TokenKind { Number, Identifier, Operator, LParen, RParen };

struct Token {
    TokenKind kind;
    std::string text;
    bool operator==(const Token& o) const { return kind == o.kind && text == o.text; }
};

inline std::vector<Token> lex(std::string_view s) {
    std::vector<Token> out;
    std::size_t i = 0;
    auto isDigit = [](char c) { return std::isdigit(static_cast<unsigned char>(c)) != 0; };
    auto isIdent = [](char c) { return std::isalnum(static_cast<unsigned char>(c)) || c == '_'; };
    while (i < s.size()) {
        char c = s[i];
        if (std::isspace(static_cast<unsigned char>(c))) {
            ++i;
        } else if (isDigit(c) || (c == '.' && i + 1 < s.size() && isDigit(s[i + 1]))) {
            std::size_t j = i;
            bool seenDot = false;
            while (j < s.size() && (isDigit(s[j]) || (s[j] == '.' && !seenDot))) {
                if (s[j] == '.') seenDot = true;
                ++j;
            }
            out.push_back({TokenKind::Number, std::string(s.substr(i, j - i))});
            i = j;
        } else if (std::isalpha(static_cast<unsigned char>(c)) || c == '_') {
            std::size_t j = i;
            while (j < s.size() && isIdent(s[j])) ++j;
            out.push_back({TokenKind::Identifier, std::string(s.substr(i, j - i))});
            i = j;
        } else if (c == '(' || c == ')') {
            out.push_back({c == '(' ? TokenKind::LParen : TokenKind::RParen, std::string(1, c)});
            ++i;
        } else if (c == '*' && i + 1 < s.size() && s[i + 1] == '*') {
            out.push_back({TokenKind::Operator, "**"});  // maximal munch
            i += 2;
        } else if (std::string_view("+-*/%^").find(c) != std::string_view::npos) {
            out.push_back({TokenKind::Operator, std::string(1, c)});
            ++i;
        } else {
            throw std::invalid_argument("unexpected character at " + std::to_string(i));
        }
    }
    return out;
}
