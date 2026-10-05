# Fast modular exponentiation (with Miller-Rabin)

**Problem:** Compute `base^exp mod m` for 64-bit values in O(log exp) without overflow. Use it for modular inverses mod a prime and a deterministic 64-bit primality test.

## Approach
- **Binary exponentiation:** write `exp` in binary. Walk its bits from low to high, squaring `base` each step and multiplying it into `result` when the bit is set. `a^13 = a^8 · a^4 · a^1`.
- **Overflow:** the product of two 64-bit residues needs 128 bits, so `mulMod` widens to `unsigned __int128` (a GCC/Clang extension) before reducing.
- Start with `result = 1 % m` so `m == 1` correctly gives 0.
- **Inverse:** by Fermat's little theorem, `a^(p-2) ≡ a⁻¹ (mod p)` for prime `p`.
- **Miller-Rabin:** write `n−1 = d·2^s`. For each witness `a`, `n` passes if `a^d ≡ 1` or `a^(d·2^r) ≡ −1` for some `r < s`. The first 12 primes as witnesses are proven sufficient for all `n < 2⁶⁴`.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| powMod | O(log exp) multiplications | O(1) |
| inverseModPrime | O(log p) | O(1) |
| isPrime | O(k · log n), k = 12 witnesses | O(1) |

## Interview talking points
- The same squaring idea works for any associative operation: matrix powers (Fibonacci in O(log n)), composing permutations, string repetition.
- Without 128-bit integers, use "Russian peasant" multiplication (`mulMod` by doubling) or Montgomery multiplication.
- Carmichael numbers like 561 pass the plain Fermat test for every coprime base; Miller-Rabin's square-root-of-1 check catches them.
- For a non-prime modulus, compute inverses with the extended Euclidean algorithm instead.
- Used everywhere in cryptography: RSA encryption/decryption, Diffie-Hellman.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
