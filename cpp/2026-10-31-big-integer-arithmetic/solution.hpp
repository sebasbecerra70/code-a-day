#pragma once
// Arbitrary-precision signed integer. Magnitude is stored little-endian in base 10^9,
// which makes decimal parsing/printing trivial and keeps limb products within 64 bits.

#include <algorithm>
#include <cstdint>
#include <ostream>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

class BigInt {
public:
    static constexpr std::uint32_t BASE = 1'000'000'000;

    BigInt(long long v = 0) {
        neg_ = v < 0;
        // Negate via unsigned to handle LLONG_MIN safely.
        unsigned long long m = neg_ ? 0ULL - static_cast<unsigned long long>(v) : v;
        while (m) { d_.push_back(static_cast<std::uint32_t>(m % BASE)); m /= BASE; }
    }

    explicit BigInt(const std::string& s) {
        std::size_t i = 0;
        if (!s.empty() && (s[0] == '-' || s[0] == '+')) { neg_ = s[0] == '-'; i = 1; }
        if (i == s.size()) throw std::invalid_argument("no digits");
        for (std::size_t k = i; k < s.size(); ++k)
            if (s[k] < '0' || s[k] > '9') throw std::invalid_argument("bad digit");
        // Consume 9 digits at a time from the right.
        for (std::size_t end = s.size(); end > i;) {
            std::size_t start = end >= i + 9 ? end - 9 : i;
            d_.push_back(static_cast<std::uint32_t>(std::stoul(s.substr(start, end - start))));
            end = start;
        }
        trim();
    }

    std::string str() const {
        if (d_.empty()) return "0";
        std::string out = neg_ ? "-" : "";
        out += std::to_string(d_.back());
        for (std::size_t i = d_.size() - 1; i-- > 0;) {
            std::string chunk = std::to_string(d_[i]);
            out += std::string(9 - chunk.size(), '0') + chunk;  // inner limbs are zero-padded
        }
        return out;
    }

    bool isZero() const { return d_.empty(); }
    BigInt operator-() const { BigInt r = *this; if (!r.isZero()) r.neg_ = !neg_; return r; }

    friend bool operator==(const BigInt& a, const BigInt& b) { return a.neg_ == b.neg_ && a.d_ == b.d_; }
    friend bool operator!=(const BigInt& a, const BigInt& b) { return !(a == b); }
    friend bool operator<(const BigInt& a, const BigInt& b) {
        if (a.neg_ != b.neg_) return a.neg_;
        int c = cmpAbs(a.d_, b.d_);
        return a.neg_ ? c > 0 : c < 0;
    }
    friend bool operator>(const BigInt& a, const BigInt& b) { return b < a; }
    friend bool operator<=(const BigInt& a, const BigInt& b) { return !(b < a); }
    friend bool operator>=(const BigInt& a, const BigInt& b) { return !(a < b); }

    friend BigInt operator+(const BigInt& a, const BigInt& b) {
        BigInt r;
        if (a.neg_ == b.neg_) {
            r.d_ = addAbs(a.d_, b.d_);
            r.neg_ = a.neg_;
        } else if (cmpAbs(a.d_, b.d_) >= 0) {
            r.d_ = subAbs(a.d_, b.d_);
            r.neg_ = a.neg_;
        } else {
            r.d_ = subAbs(b.d_, a.d_);
            r.neg_ = b.neg_;
        }
        r.trim();
        return r;
    }
    friend BigInt operator-(const BigInt& a, const BigInt& b) { return a + (-b); }

    // Schoolbook multiplication, O(n*m).
    friend BigInt operator*(const BigInt& a, const BigInt& b) {
        if (a.isZero() || b.isZero()) return BigInt();
        std::vector<std::uint64_t> acc(a.d_.size() + b.d_.size(), 0);
        for (std::size_t i = 0; i < a.d_.size(); ++i) {
            std::uint64_t carry = 0;
            for (std::size_t j = 0; j < b.d_.size(); ++j) {
                std::uint64_t cur = acc[i + j] + static_cast<std::uint64_t>(a.d_[i]) * b.d_[j] + carry;
                acc[i + j] = cur % BASE;
                carry = cur / BASE;
            }
            for (std::size_t k = i + b.d_.size(); carry; ++k) {
                std::uint64_t cur = acc[k] + carry;
                acc[k] = cur % BASE;
                carry = cur / BASE;
            }
        }
        BigInt r;
        r.d_.assign(acc.begin(), acc.end());
        r.neg_ = a.neg_ != b.neg_;
        r.trim();
        return r;
    }

    // Truncating division (like C++ built-ins): quotient rounds toward zero,
    // remainder takes the sign of the dividend.
    static std::pair<BigInt, BigInt> divmod(const BigInt& a, const BigInt& b) {
        if (b.isZero()) throw std::domain_error("division by zero");
        BigInt q, rem;
        BigInt divisor = b.abs();
        q.d_.assign(a.d_.size(), 0);
        // Long division one base-10^9 limb at a time; binary search each quotient limb.
        for (std::size_t i = a.d_.size(); i-- > 0;) {
            rem.d_.insert(rem.d_.begin(), a.d_[i]);  // rem = rem * BASE + limb
            rem.trim();
            std::uint32_t lo = 0, hi = BASE - 1;
            while (lo < hi) {
                std::uint32_t mid = lo + (hi - lo + 1) / 2;
                if (divisor * BigInt(mid) <= rem) lo = mid; else hi = mid - 1;
            }
            q.d_[i] = lo;
            rem = rem - divisor * BigInt(lo);
        }
        q.neg_ = a.neg_ != b.neg_;
        q.trim();
        rem.neg_ = a.neg_;
        rem.trim();
        return {q, rem};
    }
    friend BigInt operator/(const BigInt& a, const BigInt& b) { return divmod(a, b).first; }
    friend BigInt operator%(const BigInt& a, const BigInt& b) { return divmod(a, b).second; }

    BigInt& operator+=(const BigInt& o) { return *this = *this + o; }
    BigInt& operator-=(const BigInt& o) { return *this = *this - o; }
    BigInt& operator*=(const BigInt& o) { return *this = *this * o; }

    BigInt abs() const { BigInt r = *this; r.neg_ = false; return r; }

    friend std::ostream& operator<<(std::ostream& os, const BigInt& b) { return os << b.str(); }

private:
    using Limbs = std::vector<std::uint32_t>;

    // Remove leading zero limbs; zero is always non-negative.
    void trim() {
        while (!d_.empty() && d_.back() == 0) d_.pop_back();
        if (d_.empty()) neg_ = false;
    }

    static int cmpAbs(const Limbs& a, const Limbs& b) {
        if (a.size() != b.size()) return a.size() < b.size() ? -1 : 1;
        for (std::size_t i = a.size(); i-- > 0;)
            if (a[i] != b[i]) return a[i] < b[i] ? -1 : 1;
        return 0;
    }

    static Limbs addAbs(const Limbs& a, const Limbs& b) {
        Limbs r;
        std::uint32_t carry = 0;
        for (std::size_t i = 0; i < std::max(a.size(), b.size()) || carry; ++i) {
            std::uint64_t cur = carry;
            if (i < a.size()) cur += a[i];
            if (i < b.size()) cur += b[i];
            r.push_back(static_cast<std::uint32_t>(cur % BASE));
            carry = static_cast<std::uint32_t>(cur / BASE);
        }
        return r;
    }

    // Requires |a| >= |b|.
    static Limbs subAbs(const Limbs& a, const Limbs& b) {
        Limbs r(a);
        std::int64_t borrow = 0;
        for (std::size_t i = 0; i < r.size(); ++i) {
            std::int64_t cur = static_cast<std::int64_t>(r[i]) - borrow - (i < b.size() ? b[i] : 0);
            borrow = cur < 0;
            if (cur < 0) cur += BASE;
            r[i] = static_cast<std::uint32_t>(cur);
        }
        return r;
    }

    bool neg_ = false;
    Limbs d_;  // little-endian limbs, no leading zeros
};
