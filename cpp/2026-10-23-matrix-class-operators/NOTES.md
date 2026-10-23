# Matrix class with operator overloading

**Problem:** Write a generic dense `Matrix<T>` that supports `+`, `-`, unary `-`, scalar `*`, matrix `*`, `==`, `transpose`, `identity`, fast `pow`, and printing with `<<`, throwing on dimension mismatches.

## Approach
- Store the elements in one row-major `std::vector<T>`; element `(r, c)` is at `r * cols + c`. One contiguous block is faster and simpler than `vector<vector<T>>`.
- Implement the **compound** operators (`+=`, `-=`, `*=`) as members, and the **binary** operators as friends that take the left side by value and reuse them (`a += b; return a;`).
- Multiply with **i-k-j** loop order so the inner loop walks `b` and `out` row-wise.
- `pow` uses exponentiation by squaring. For example, `[[1,1],[1,0]]^n` gives Fibonacci numbers in O(log n) multiplications.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| `+`, `-`, scalar `*`, `==` | O(rc) | O(rc) |
| matrix `*` (n×m by m×p) | O(nmp) | O(np) |
| transpose | O(rc) | O(rc) |
| pow (n×n) | O(n³ log p) | O(n²) |

## Interview talking points
- Binary operators as **non-member friends** allow implicit conversions on both sides and symmetric `k * M` and `M * k`.
- Taking the left operand by value in `operator+` gives a free copy (or move from a temporary) to mutate.
- Loop order matters: i-j-k strides down columns of `b` and misses cache; i-k-j is often several times faster. Real libraries tile/block for cache and use SIMD.
- Strassen runs in O(n^2.81) but is rarely worth it below large n because of constant factors and numerical stability.
- Expression templates (Eigen) avoid temporaries in `a + b + c` by building a lazy expression tree.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
