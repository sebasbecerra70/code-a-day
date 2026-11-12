# Sieve of Eratosthenes (plus linear and segmented sieves)

**Problem:** List all primes up to `n` efficiently. Extensions: build a smallest-prime-factor table for fast factorization, and list primes in a window `[lo, hi]` where `hi` is too large to sieve from 0 (for example near 10¹²).

## Approach
- **Eratosthenes:** for each prime `i` with `i² <= n`, cross out `i², i² + i, ...`. Start at `i²` because smaller multiples have a smaller prime factor and are already crossed out.
- **Linear sieve:** for each `i`, mark `i * p` for each prime `p <= spf[i]`. Every composite is marked exactly once, by its smallest prime factor, giving an `spf` table. Factorizing `x` is then repeated division by `spf[x]`.
- **Segmented sieve:** sieve the base primes up to `√hi`, then for each base prime cross out its multiples inside a boolean array covering only `[lo, hi]`.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| sieve(n) | O(n log log n) | O(n) bits |
| smallestPrimeFactor(n) | O(n) | O(n) ints |
| factorize(x) with spf | O(log x) | O(1) |
| segmentedSieve(lo, hi) | O((hi−lo) log log hi + √hi) | O(√hi + hi − lo) |

## Interview talking points
- Why only up to √n? Any composite `≤ n` has a prime factor `≤ √n`.
- `std::vector<bool>` is bit-packed (8x less memory), which also helps cache behavior; a wheel that skips even numbers halves it again.
- Segmented sieving also fixes the cache problem for large `n`: sieve in blocks the size of L1/L2 cache.
- For a single large number, use trial division up to √x or Miller-Rabin instead of a sieve.
- Watch for `i * i` overflow in `int`; loop with `long long`.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
