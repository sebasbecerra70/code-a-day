# Rational number class (exact fractions)

**Problem:** Implement an exact fraction type with `+ - * /`, unary minus, all comparisons, printing and conversion to `double`. Reject zero denominators and division by zero.

## Approach
- Keep a **canonical form**: denominator positive, numerator and denominator coprime. Then `==` is just comparing the two fields, and printing is unambiguous.
- `normalize()` moves the sign to the numerator and divides by `std::gcd`.
- To limit overflow, add with the **lcm** of denominators and **cross-cancel** before multiplying (`a/b * c/d`: divide `a` and `d` by their gcd, and `c` and `b` by theirs).
- Compare with cross-multiplication (`a*d < c*b`), using a 128-bit intermediate. That's only valid because denominators are positive.
- The non-explicit `Rational(int64_t num = 0, int64_t den = 1)` constructor lets integers mix in naturally: `R(1, 3) + 2`.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| construct / + - * / | O(log min(a, b)) for gcd | O(1) |
| comparison | O(1) | O(1) |

## Interview talking points
- Why normalize eagerly? Canonical form makes equality and hashing trivial and keeps numbers small. The alternative (normalize lazily) needs gcd at every comparison.
- Overflow is the real enemy with fixed-width integers; production code uses big integers (Python's `fractions.Fraction`, GMP's `mpq_t`).
- `std::gcd` (C++17) works with negative arguments and returns a non-negative result.
- Floats can't represent 1/3 or 0.1 exactly; rationals are exact for financial or geometric predicates.
- Binary operators defined as friends in terms of the compound operators keep the code DRY and symmetric.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
