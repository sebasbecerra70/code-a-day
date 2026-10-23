#pragma once
// Dense row-major matrix with arithmetic operators, transpose, identity and fast power.

#include <cstddef>
#include <initializer_list>
#include <ostream>
#include <stdexcept>
#include <vector>

template <typename T>
class Matrix {
public:
    Matrix(std::size_t rows, std::size_t cols, T fill = T{})
        : rows_(rows), cols_(cols), data_(rows * cols, fill) {}

    Matrix(std::initializer_list<std::initializer_list<T>> init)
        : rows_(init.size()), cols_(init.size() ? init.begin()->size() : 0) {
        data_.reserve(rows_ * cols_);
        for (const auto& row : init) {
            if (row.size() != cols_) throw std::invalid_argument("ragged initializer");
            data_.insert(data_.end(), row.begin(), row.end());
        }
    }

    static Matrix identity(std::size_t n) {
        Matrix m(n, n);
        for (std::size_t i = 0; i < n; ++i) m(i, i) = T{1};
        return m;
    }

    std::size_t rows() const { return rows_; }
    std::size_t cols() const { return cols_; }

    T& operator()(std::size_t r, std::size_t c) { return data_[r * cols_ + c]; }
    const T& operator()(std::size_t r, std::size_t c) const { return data_[r * cols_ + c]; }

    // Compound operators do the work; binary operators are written in terms of them.
    Matrix& operator+=(const Matrix& o) {
        requireSameShape(o);
        for (std::size_t i = 0; i < data_.size(); ++i) data_[i] += o.data_[i];
        return *this;
    }
    Matrix& operator-=(const Matrix& o) {
        requireSameShape(o);
        for (std::size_t i = 0; i < data_.size(); ++i) data_[i] -= o.data_[i];
        return *this;
    }
    Matrix& operator*=(const T& k) {
        for (auto& x : data_) x *= k;
        return *this;
    }

    friend Matrix operator+(Matrix a, const Matrix& b) { return a += b; }
    friend Matrix operator-(Matrix a, const Matrix& b) { return a -= b; }
    friend Matrix operator*(Matrix a, const T& k) { return a *= k; }
    friend Matrix operator*(const T& k, Matrix a) { return a *= k; }
    Matrix operator-() const { return *this * T{-1}; }

    friend Matrix operator*(const Matrix& a, const Matrix& b) {
        if (a.cols_ != b.rows_) throw std::invalid_argument("dimension mismatch");
        Matrix out(a.rows_, b.cols_);
        // i-k-j loop order walks both a and b row-wise, which is cache friendly.
        for (std::size_t i = 0; i < a.rows_; ++i)
            for (std::size_t k = 0; k < a.cols_; ++k) {
                const T aik = a(i, k);
                for (std::size_t j = 0; j < b.cols_; ++j) out(i, j) += aik * b(k, j);
            }
        return out;
    }
    Matrix& operator*=(const Matrix& o) { return *this = *this * o; }

    friend bool operator==(const Matrix& a, const Matrix& b) {
        return a.rows_ == b.rows_ && a.cols_ == b.cols_ && a.data_ == b.data_;
    }
    friend bool operator!=(const Matrix& a, const Matrix& b) { return !(a == b); }

    Matrix transpose() const {
        Matrix t(cols_, rows_);
        for (std::size_t i = 0; i < rows_; ++i)
            for (std::size_t j = 0; j < cols_; ++j) t(j, i) = (*this)(i, j);
        return t;
    }

    // Exponentiation by squaring: O(n^3 log p).
    Matrix pow(unsigned long long p) const {
        if (rows_ != cols_) throw std::invalid_argument("pow needs a square matrix");
        Matrix result = identity(rows_), base = *this;
        while (p) {
            if (p & 1) result *= base;
            base *= base;
            p >>= 1;
        }
        return result;
    }

    friend std::ostream& operator<<(std::ostream& os, const Matrix& m) {
        for (std::size_t i = 0; i < m.rows_; ++i) {
            os << '[';
            for (std::size_t j = 0; j < m.cols_; ++j) os << (j ? " " : "") << m(i, j);
            os << "]\n";
        }
        return os;
    }

private:
    void requireSameShape(const Matrix& o) const {
        if (rows_ != o.rows_ || cols_ != o.cols_) throw std::invalid_argument("shape mismatch");
    }

    std::size_t rows_, cols_;
    std::vector<T> data_;
};
