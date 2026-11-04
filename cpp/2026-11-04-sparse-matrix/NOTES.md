# Sparse matrix (CSR format)

**Problem:** Store a mostly-zero matrix in memory proportional to its nonzeros, and support element lookup, matrix-vector product, transpose, sparse-sparse multiplication and addition.

## Approach
Use **Compressed Sparse Row (CSR)** with three arrays:
- `values`: the nonzero values, row by row.
- `colIdx`: the column of each value, sorted within each row.
- `rowPtr` (length rows + 1): row `r` lives in `[rowPtr[r], rowPtr[r+1])`.

Build it from COO triplets: sort by (row, col), merge duplicates by summing, drop zeros, count entries per row, then prefix-sum the counts into `rowPtr`.
- `get(r, c)`: binary search `c` inside row `r`'s slice.
- `A·x`: one pass over all nonzeros.
- `A·B`: for each nonzero `A[i][k]`, scale row `k` of `B` and accumulate into result row `i` (Gustavson's algorithm).
- Transpose and add go through triplets and reuse the builder.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| build from t triplets | O(t log t + rows) | O(nnz + rows) |
| get | O(log nnz_row) | O(1) |
| matrix-vector | O(nnz + rows) | O(rows) |
| transpose / add | O(nnz log nnz) | O(nnz) |
| A·B | O(Σ over A[i][k] of nnz(B row k) · log) | O(nnz of result) |

## Interview talking points
- COO is easy to build; CSR is fast for row operations and mat-vec; CSC (the transpose layout) is fast for column operations.
- Explicitly storing zeros wastes space and breaks `nnz`, so the builder drops them.
- Gustavson's row-wise product is what SciPy and most libraries use. A dense accumulator array plus a "touched" list replaces the `std::map` for speed.
- Use cases: graphs as adjacency matrices (PageRank is repeated mat-vec), finite-element systems, recommender systems, NLP bag-of-words.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
