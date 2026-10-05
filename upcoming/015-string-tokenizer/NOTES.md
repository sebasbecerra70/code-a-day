# String tokenizer (split, quoted fields, arithmetic lexer)

**Problem:** C++ has no built-in `split`. Implement three tokenizers: (1) split on any of a set of delimiter characters, optionally keeping empty fields; (2) a shell-like splitter that honors `"double quotes"` and `\` escapes; (3) a lexer that turns an arithmetic expression into typed tokens.

## Approach
- **split:** repeatedly `find_first_of(delims, start)` and slice with `string_view::substr`. Returning `string_view`s means no copies, but the input must outlive the result.
- **splitQuoted:** a small state machine with two flags, `inQuotes` and `inToken`. Whitespace ends a token only outside quotes. `inToken` lets `""` produce an empty token.
- **lex:** a hand-written scanner. Look at the current character, consume the longest valid token (*maximal munch*, so `**` is one operator, not two), and record its kind.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| split | O(n) | O(k) views |
| splitQuoted | O(n) | O(n) |
| lex | O(n) | O(n) |

## Interview talking points
- `string_view` avoids allocation but can dangle. Never return views into a temporary `std::string`.
- Always cast to `unsigned char` before `std::isspace`/`isdigit`: passing a negative `char` is undefined behavior.
- Keep-empty vs skip-empty matters for CSV (`a,,b` has three fields) versus whitespace splitting.
- Lexing vs parsing: the lexer produces a flat token stream; a parser (for example shunting-yard or recursive descent) then builds structure.
- `std::getline(stream, piece, ',')` and `strtok` are alternatives; `strtok` mutates the input and is not reentrant.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
