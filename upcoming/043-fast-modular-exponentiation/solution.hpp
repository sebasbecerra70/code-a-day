#pragma once
// Binary exponentiation mod m, overflow-safe for any 64-bit modulus, plus two classic
// applications: modular inverse via Fermat and deterministic Miller-Rabin primality.

#include <array>
#include <cstdint>
#include <stdexcept>

using u64 = std::uint64_t;
using u128 = unsigned __int128;

// (a * b) mod m without overflow: widen to 128 bits.
inline u64 mulMod(u64 a, u64 b, u64 m) { return static_cast<u64>(static_cast<u128>(a) * b % m); }

// Right-to-left binary exponentiation: square the base, multiply in when the bit is set.
inline u64 powMod(u64 base, u64 exp, u64 m) {
    if (m == 0) throw std::invalid_argument("modulus must be positive");
    u64 result = 1 % m;  // handles m == 1
    base %= m;
    while (exp) {
        if (exp & 1) result = mulMod(result, base, m);
        base = mulMod(base, base, m);
        exp >>= 1;
    }
    return result;
}

// a^(p-2) is a^-1 mod p when p is prime and p does not divide a (Fermat's little theorem).
inline u64 inverseModPrime(u64 a, u64 p) {
    if (a % p == 0) throw std::domain_error("no inverse");
    return powMod(a, p - 2, p);
}

inline constexpr std::array<u64, 12> kSmallPrimes = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};

// Deterministic Miller-Rabin for all 64-bit n using the first 12 primes as witnesses.
inline bool isPrime(u64 n) {
    if (n < 2) return false;
    for (u64 p : kSmallPrimes) {
        if (n % p == 0) return n == p;
    }
    // Write n - 1 = d * 2^s with d odd.
    u64 d = n - 1;
    int s = 0;
    while ((d & 1) == 0) { d >>= 1; ++s; }
    for (u64 a : kSmallPrimes) {
        u64 x = powMod(a, d, n);
        if (x == 1 || x == n - 1) continue;
        bool composite = true;
        for (int r = 1; r < s; ++r) {
            x = mulMod(x, x, n);
            if (x == n - 1) { composite = false; break; }
        }
        if (composite) return false;  // a is a witness that n is composite
    }
    return true;
}
