#pragma once
// Exact fraction type. Invariant: den > 0 and gcd(|num|, den) == 1, so equal values
// have identical representations and == is a plain field comparison.

#include <cstdint>
#include <numeric>
#include <ostream>
#include <stdexcept>
#include <string>

class Rational {
public:
    Rational(std::int64_t num = 0, std::int64_t den = 1) : num_(num), den_(den) {
        if (den_ == 0) throw std::domain_error("zero denominator");
        normalize();
    }

    std::int64_t num() const { return num_; }
    std::int64_t den() const { return den_; }

    Rational& operator+=(const Rational& o) {
        // Use lcm of denominators to keep intermediates small.
        std::int64_t g = std::gcd(den_, o.den_);
        num_ = num_ * (o.den_ / g) + o.num_ * (den_ / g);
        den_ = den_ / g * o.den_;
        normalize();
        return *this;
    }
    Rational& operator-=(const Rational& o) { return *this += -o; }
    Rational& operator*=(const Rational& o) {
        // Cross-cancel first to reduce overflow risk.
        std::int64_t g1 = std::gcd(num_, o.den_), g2 = std::gcd(o.num_, den_);
        num_ = (num_ / g1) * (o.num_ / g2);
        den_ = (den_ / g2) * (o.den_ / g1);
        normalize();
        return *this;
    }
    Rational& operator/=(const Rational& o) {
        if (o.num_ == 0) throw std::domain_error("division by zero");
        return *this *= Rational(o.den_, o.num_);
    }

    Rational operator-() const { return Rational(-num_, den_); }

    friend Rational operator+(Rational a, const Rational& b) { return a += b; }
    friend Rational operator-(Rational a, const Rational& b) { return a -= b; }
    friend Rational operator*(Rational a, const Rational& b) { return a *= b; }
    friend Rational operator/(Rational a, const Rational& b) { return a /= b; }

    friend bool operator==(const Rational& a, const Rational& b) {
        return a.num_ == b.num_ && a.den_ == b.den_;
    }
    friend bool operator!=(const Rational& a, const Rational& b) { return !(a == b); }
    // a/b < c/d  <=>  a*d < c*b, valid because denominators are positive.
    friend bool operator<(const Rational& a, const Rational& b) {
        return static_cast<__int128>(a.num_) * b.den_ < static_cast<__int128>(b.num_) * a.den_;
    }
    friend bool operator>(const Rational& a, const Rational& b) { return b < a; }
    friend bool operator<=(const Rational& a, const Rational& b) { return !(b < a); }
    friend bool operator>=(const Rational& a, const Rational& b) { return !(a < b); }

    double toDouble() const { return static_cast<double>(num_) / static_cast<double>(den_); }

    std::string str() const {
        return den_ == 1 ? std::to_string(num_) : std::to_string(num_) + "/" + std::to_string(den_);
    }
    friend std::ostream& operator<<(std::ostream& os, const Rational& r) { return os << r.str(); }

private:
    void normalize() {
        if (den_ < 0) { num_ = -num_; den_ = -den_; }
        std::int64_t g = std::gcd(num_, den_);  // std::gcd takes absolute values
        if (g > 1) { num_ /= g; den_ /= g; }
    }

    std::int64_t num_, den_;
};
