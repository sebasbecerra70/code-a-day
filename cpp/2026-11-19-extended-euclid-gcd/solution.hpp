#pragma once
// Euclid's gcd and the extended version that also finds Bezout coefficients,
// with the standard applications built on top.

#include <cstdint>
#include <optional>
#include <stdexcept>
#include <utility>

using i64 = std::int64_t;

inline i64 gcd(i64 a, i64 b) {
    a = a < 0 ? -a : a;
    b = b < 0 ? -b : b;
    while (b) {
        i64 t = a % b;
        a = b;
        b = t;
    }
    return a;
}

struct Bezout {
    i64 g, x, y;  // a*x + b*y == g == gcd(a, b)
};

// Iterative extended Euclid: keep (old_r, r), (old_x, x), (old_y, y) in lockstep.
inline Bezout extendedGcd(i64 a, i64 b) {
    i64 oldR = a, r = b, oldX = 1, x = 0, oldY = 0, y = 1;
    while (r != 0) {
        i64 q = oldR / r;
        oldR = std::exchange(r, oldR - q * r);
        oldX = std::exchange(x, oldX - q * x);
        oldY = std::exchange(y, oldY - q * y);
    }
    if (oldR < 0) return {-oldR, -oldX, -oldY};  // make gcd non-negative
    return {oldR, oldX, oldY};
}

// Inverse of a modulo m (m > 1), if gcd(a, m) == 1. Result in [0, m).
inline std::optional<i64> modInverse(i64 a, i64 m) {
    if (m <= 1) throw std::invalid_argument("modulus must be > 1");
    auto [g, x, y] = extendedGcd(((a % m) + m) % m, m);
    (void)y;
    if (g != 1) return std::nullopt;
    return ((x % m) + m) % m;
}

// One solution (x, y) of a*x + b*y == c, if any exists.
inline std::optional<std::pair<i64, i64>> solveDiophantine(i64 a, i64 b, i64 c) {
    if (a == 0 && b == 0) {
        if (c == 0) return std::pair<i64, i64>{0, 0};
        return std::nullopt;
    }
    auto [g, x, y] = extendedGcd(a, b);
    if (c % g != 0) return std::nullopt;
    return std::pair<i64, i64>{x * (c / g), y * (c / g)};
}

// Chinese Remainder Theorem for two congruences x ≡ r1 (mod m1), x ≡ r2 (mod m2),
// moduli need not be coprime. Returns (x, lcm) with 0 <= x < lcm, or nullopt if inconsistent.
inline std::optional<std::pair<i64, i64>> crt(i64 r1, i64 m1, i64 r2, i64 m2) {
    auto [g, p, q] = extendedGcd(m1, m2);
    (void)q;
    if ((r2 - r1) % g != 0) return std::nullopt;
    i64 lcm = m1 / g * m2;
    // x = r1 + m1 * k where m1*k ≡ r2 - r1 (mod m2)  =>  k = p * (r2 - r1)/g mod (m2/g)
    i64 mod = m2 / g;
    __int128 k = static_cast<__int128>(p) * ((r2 - r1) / g) % mod;
    __int128 x = (r1 + static_cast<__int128>(m1) * k) % lcm;
    if (x < 0) x += lcm;
    return std::pair<i64, i64>{static_cast<i64>(x), lcm};
}
