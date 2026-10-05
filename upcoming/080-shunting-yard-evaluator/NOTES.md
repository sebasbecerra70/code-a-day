# Shunting-yard expression evaluator

**Problem:** Evaluate arithmetic expressions like `3 + 4 * 2 / (1 - 5) ^ 2 ^ 3` with correct precedence, associativity, unary minus, parentheses, and variables, and report malformed input clearly.

## Approach
- **Tokenize** with one regex: numbers (`2`, `2.`, `.5`), identifiers, and single-character symbols. Anything else is a `SyntaxError`.
- **Shunting-yard (Dijkstra)** converts infix to Reverse Polish Notation with an operator stack:
  - operands go straight to output;
  - an operator first pops operators that bind tighter (or equally tight, if it's left-associative), then is pushed;
  - `(` is pushed; `)` pops until the matching `(`.
- An `expect_operand` flag tells unary from binary `-`/`+` and catches errors like `1 2`, `* 2`, `1 +`, `()`. Unary minus becomes a prefix `neg` operator that never pops anything; its precedence sits below `^`, so `-2^2 = -4` and `2^-1 = 0.5`.
- **Evaluate RPN** with a value stack. Division/modulo by zero and unknown variables raise.
- Tests cross-check random expressions against Python's own `eval`.

## Complexity
| Step | Time | Space |
|------|------|-------|
| tokenize / to_rpn / eval_rpn | O(n) each | O(n) |

## Interview talking points
- Precedence climbing / Pratt parsing is the recursive cousin, easier to extend with function calls and postfix operators.
- Building an AST instead of evaluating directly enables constant folding, pretty-printing, and differentiation.
- Right associativity of `^` is the classic trap: `2^3^2` is `2^(3^2) = 512`.
- Why RPN? It needs no parentheses or precedence rules: HP calculators, Forth, and the JVM operand stack.
- Never `eval()` user input; a small parser like this is the safe alternative.

## Run
From this folder: `python -m pytest -q`
