# Extended Euclidean algorithm (inverses, Diophantine, CRT)

**Problem:** Compute `gcd(a, b)` together with integers `x, y` such that `a·x + b·y = gcd(a, b)` (Bézout's identity). Use it to find modular inverses for any modulus, solve linear Diophantine equations `a·x + b·y = c`, and combine two congruences with the Chinese Remainder Theorem (moduli not necessarily coprime).

## Approach
- **Euclid:** `gcd(a, b) = gcd(b, a mod b)` until `b = 0`.
- **Extended, iterative:** keep the remainder sequence `r` alongside coefficient sequences `x`, `y`, so each `r = a·x + b·y` holds at every step. Each step applies the same update `new = old − q·cur` to all three. When `r` reaches 0, the previous row holds the gcd and its coefficients.
- **Inverse mod m:** `a·x + m·y = 1` means `a·x ≡ 1 (mod m)`. It exists iff `gcd(a, m) = 1`.
- **Diophantine:** solvable iff `g | c`; scale the Bézout pair by `c / g`.
- **CRT:** `x = r1 + m1·k` and we need `m1·k ≡ r2 − r1 (mod m2)`. That's solvable iff `g | (r2 − r1)`, with `k = p · (r2 − r1)/g mod (m2/g)` where `p` is `m1`'s Bézout coefficient. The answer is unique mod `lcm(m1, m2)`.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| gcd / extendedGcd | O(log min(a, b)) | O(1) |
| modInverse, solveDiophantine, crt | O(log min) | O(1) |

## Interview talking points
- Lamé's theorem: the worst case is consecutive Fibonacci numbers, and the step count is O(log φ of min(a, b)).
- Fermat's `a^(p−2)` only works for prime moduli; extended Euclid works for any modulus and is usually faster.
- The general solution of the Diophantine equation is `x + k·(b/g), y − k·(a/g)` for any integer `k`.
- The iterative version avoids recursion depth issues and makes the invariant `r_i = a·x_i + b·y_i` explicit.
- CRT is used for RSA decryption speedups, combining hashes, and calendar/scheduling puzzles.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
