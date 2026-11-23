#pragma once
// 2D prefix sums for O(1) rectangle-sum queries, the inverse "difference array"
// for O(1) rectangle updates, and a classic application (max-sum k x k square).

#include <algorithm>
#include <cstddef>
#include <limits>
#include <stdexcept>
#include <vector>

using Grid = std::vector<std::vector<long long>>;

class PrefixSum2D {
public:
    explicit PrefixSum2D(const Grid& g)
        : rows_(g.size()), cols_(g.empty() ? 0 : g[0].size()),
          // One row/col of zero padding removes all boundary checks.
          p_(rows_ + 1, std::vector<long long>(cols_ + 1, 0)) {
        for (std::size_t r = 0; r < rows_; ++r) {
            if (g[r].size() != cols_) throw std::invalid_argument("ragged grid");
            for (std::size_t c = 0; c < cols_; ++c)
                p_[r + 1][c + 1] = g[r][c] + p_[r][c + 1] + p_[r + 1][c] - p_[r][c];
        }
    }

    // Sum of the inclusive rectangle (r1, c1)..(r2, c2) by inclusion-exclusion.
    long long sum(std::size_t r1, std::size_t c1, std::size_t r2, std::size_t c2) const {
        if (r1 > r2 || c1 > c2 || r2 >= rows_ || c2 >= cols_) throw std::out_of_range("bad rectangle");
        return p_[r2 + 1][c2 + 1] - p_[r1][c2 + 1] - p_[r2 + 1][c1] + p_[r1][c1];
    }

private:
    std::size_t rows_, cols_;
    Grid p_;  // p_[r][c] = sum of g[0..r-1][0..c-1]
};

// Batch of "add v to every cell in a rectangle" updates in O(1) each, materialized at the end.
class RectAdder {
public:
    RectAdder(std::size_t rows, std::size_t cols)
        : rows_(rows), cols_(cols), d_(rows + 1, std::vector<long long>(cols + 1, 0)) {}

    void add(std::size_t r1, std::size_t c1, std::size_t r2, std::size_t c2, long long v) {
        if (r1 > r2 || c1 > c2 || r2 >= rows_ || c2 >= cols_) throw std::out_of_range("bad rectangle");
        d_[r1][c1] += v;
        d_[r1][c2 + 1] -= v;
        d_[r2 + 1][c1] -= v;
        d_[r2 + 1][c2 + 1] += v;
    }

    // A 2D prefix sum over the difference array recovers the final grid.
    Grid build() const {
        Grid g(rows_, std::vector<long long>(cols_, 0));
        for (std::size_t r = 0; r < rows_; ++r)
            for (std::size_t c = 0; c < cols_; ++c) {
                g[r][c] = d_[r][c];
                if (r) g[r][c] += g[r - 1][c];
                if (c) g[r][c] += g[r][c - 1];
                if (r && c) g[r][c] -= g[r - 1][c - 1];
            }
        return g;
    }

private:
    std::size_t rows_, cols_;
    Grid d_;
};

// Largest sum of any k x k sub-square.
inline long long maxSquareSum(const Grid& g, std::size_t k) {
    if (g.empty() || k == 0 || k > g.size() || k > g[0].size()) throw std::invalid_argument("bad k");
    PrefixSum2D ps(g);
    long long best = std::numeric_limits<long long>::min();
    for (std::size_t r = 0; r + k <= g.size(); ++r)
        for (std::size_t c = 0; c + k <= g[0].size(); ++c)
            best = std::max(best, ps.sum(r, c, r + k - 1, c + k - 1));
    return best;
}
