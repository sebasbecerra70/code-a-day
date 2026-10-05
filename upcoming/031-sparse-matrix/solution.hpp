#pragma once
// Compressed Sparse Row (CSR) matrix. Built from (row, col, value) triplets;
// supports lookup, matrix-vector product, transpose, sparse-sparse product and addition.

#include <algorithm>
#include <cstddef>
#include <map>
#include <stdexcept>
#include <tuple>
#include <vector>

class SparseMatrix {
public:
    struct Triplet {
        std::size_t row, col;
        double value;
    };

    SparseMatrix(std::size_t rows, std::size_t cols) : rows_(rows), cols_(cols), rowPtr_(rows + 1, 0) {}

    // Duplicate coordinates are summed; explicit zeros are dropped.
    static SparseMatrix fromTriplets(std::size_t rows, std::size_t cols, std::vector<Triplet> ts) {
        for (const auto& t : ts)
            if (t.row >= rows || t.col >= cols) throw std::out_of_range("triplet out of bounds");
        std::sort(ts.begin(), ts.end(), [](const Triplet& a, const Triplet& b) {
            return std::tie(a.row, a.col) < std::tie(b.row, b.col);
        });
        SparseMatrix m(rows, cols);
        for (std::size_t i = 0; i < ts.size();) {
            std::size_t j = i;
            double sum = 0;
            while (j < ts.size() && ts[j].row == ts[i].row && ts[j].col == ts[i].col) sum += ts[j++].value;
            if (sum != 0) {
                m.colIdx_.push_back(ts[i].col);
                m.values_.push_back(sum);
                ++m.rowPtr_[ts[i].row + 1];
            }
            i = j;
        }
        // Prefix-sum the per-row counts into row start offsets.
        for (std::size_t r = 0; r < rows; ++r) m.rowPtr_[r + 1] += m.rowPtr_[r];
        return m;
    }

    std::size_t rows() const { return rows_; }
    std::size_t cols() const { return cols_; }
    std::size_t nnz() const { return values_.size(); }

    // Binary search within the row: O(log nnz_in_row).
    double get(std::size_t r, std::size_t c) const {
        if (r >= rows_ || c >= cols_) throw std::out_of_range("index out of bounds");
        auto first = colIdx_.begin() + rowPtr_[r], last = colIdx_.begin() + rowPtr_[r + 1];
        auto it = std::lower_bound(first, last, c);
        return (it != last && *it == c) ? values_[it - colIdx_.begin()] : 0.0;
    }

    std::vector<double> multiply(const std::vector<double>& x) const {
        if (x.size() != cols_) throw std::invalid_argument("vector size mismatch");
        std::vector<double> y(rows_, 0.0);
        for (std::size_t r = 0; r < rows_; ++r)
            for (std::size_t k = rowPtr_[r]; k < rowPtr_[r + 1]; ++k) y[r] += values_[k] * x[colIdx_[k]];
        return y;
    }

    SparseMatrix transpose() const { return fromTriplets(cols_, rows_, triplets(true)); }

    // Row-by-row product: for each nonzero A[i][k], add A[i][k] * row k of B.
    SparseMatrix multiply(const SparseMatrix& b) const {
        if (cols_ != b.rows_) throw std::invalid_argument("dimension mismatch");
        std::vector<Triplet> out;
        for (std::size_t i = 0; i < rows_; ++i) {
            std::map<std::size_t, double> acc;  // sorted columns of result row i
            for (std::size_t p = rowPtr_[i]; p < rowPtr_[i + 1]; ++p) {
                std::size_t k = colIdx_[p];
                for (std::size_t q = b.rowPtr_[k]; q < b.rowPtr_[k + 1]; ++q)
                    acc[b.colIdx_[q]] += values_[p] * b.values_[q];
            }
            for (auto [c, v] : acc) out.push_back({i, c, v});
        }
        return fromTriplets(rows_, b.cols_, std::move(out));
    }

    SparseMatrix add(const SparseMatrix& b) const {
        if (rows_ != b.rows_ || cols_ != b.cols_) throw std::invalid_argument("shape mismatch");
        auto ts = triplets(false), tb = b.triplets(false);
        ts.insert(ts.end(), tb.begin(), tb.end());
        return fromTriplets(rows_, cols_, std::move(ts));
    }

    std::vector<std::vector<double>> toDense() const {
        std::vector<std::vector<double>> d(rows_, std::vector<double>(cols_, 0.0));
        for (const auto& t : triplets(false)) d[t.row][t.col] = t.value;
        return d;
    }

private:
    std::vector<Triplet> triplets(bool swapped) const {
        std::vector<Triplet> ts;
        ts.reserve(nnz());
        for (std::size_t r = 0; r < rows_; ++r)
            for (std::size_t k = rowPtr_[r]; k < rowPtr_[r + 1]; ++k)
                ts.push_back(swapped ? Triplet{colIdx_[k], r, values_[k]} : Triplet{r, colIdx_[k], values_[k]});
        return ts;
    }

    std::size_t rows_, cols_;
    std::vector<std::size_t> rowPtr_;  // row r occupies [rowPtr_[r], rowPtr_[r+1])
    std::vector<std::size_t> colIdx_;  // column of each stored value, sorted within a row
    std::vector<double> values_;
};
