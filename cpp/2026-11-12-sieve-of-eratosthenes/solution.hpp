#pragma once
// Prime sieves:
//   sieve(n)              - classic Eratosthenes, primes <= n
//   smallestPrimeFactor(n)- linear sieve; spf table enables O(log x) factorization
//   segmentedSieve(lo,hi) - primes in [lo, hi] using O(sqrt(hi) + (hi-lo)) memory

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <utility>
#include <vector>

inline std::vector<int> sieve(int n) {
    std::vector<int> primes;
    if (n < 2) return primes;
    std::vector<bool> composite(n + 1, false);
    for (long long i = 2; i * i <= n; ++i) {
        if (composite[i]) continue;
        // Smaller multiples were already crossed out by smaller primes.
        for (long long j = i * i; j <= n; j += i) composite[j] = true;
    }
    for (int i = 2; i <= n; ++i)
        if (!composite[i]) primes.push_back(i);
    return primes;
}

// Linear sieve: every composite is marked exactly once, by its smallest prime factor.
inline std::vector<int> smallestPrimeFactor(int n) {
    std::vector<int> spf(n + 1, 0), primes;
    for (int i = 2; i <= n; ++i) {
        if (spf[i] == 0) { spf[i] = i; primes.push_back(i); }
        for (int p : primes) {
            if (p > spf[i] || static_cast<long long>(i) * p > n) break;
            spf[i * p] = p;
        }
    }
    return spf;
}

// Prime factorization as (prime, exponent) pairs using a precomputed spf table.
inline std::vector<std::pair<int, int>> factorize(int x, const std::vector<int>& spf) {
    std::vector<std::pair<int, int>> out;
    while (x > 1) {
        int p = spf[x], e = 0;
        while (x % p == 0) { x /= p; ++e; }
        out.push_back({p, e});
    }
    return out;
}

inline std::vector<std::int64_t> segmentedSieve(std::int64_t lo, std::int64_t hi) {
    std::vector<std::int64_t> out;
    if (hi < 2 || hi < lo) return out;
    if (lo < 2) lo = 2;
    auto limit = static_cast<int>(std::sqrt(static_cast<double>(hi)));
    while (static_cast<std::int64_t>(limit + 1) * (limit + 1) <= hi) ++limit;  // guard rounding
    std::vector<bool> composite(hi - lo + 1, false);
    for (int p : sieve(limit)) {
        // First multiple of p in range, but never p itself.
        std::int64_t start = std::max<std::int64_t>(static_cast<std::int64_t>(p) * p, (lo + p - 1) / p * p);
        for (std::int64_t j = start; j <= hi; j += p) composite[j - lo] = true;
    }
    for (std::int64_t i = lo; i <= hi; ++i)
        if (!composite[i - lo]) out.push_back(i);
    return out;
}
