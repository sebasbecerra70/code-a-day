import itertools

import pytest

from solution import count_n_queens, solve_n_queens

KNOWN = [1, 1, 0, 0, 2, 10, 4, 40, 92, 352, 724]  # OEIS A000170, n = 0..10


def is_valid(board):
    n = len(board)
    queens = [(r, row.index("Q")) for r, row in enumerate(board)]
    if any(row.count("Q") != 1 or len(row) != n for row in board):
        return False
    return all(
        c1 != c2 and abs(r1 - r2) != abs(c1 - c2)
        for (r1, c1), (r2, c2) in itertools.combinations(queens, 2)
    )


def test_four_queens():
    assert solve_n_queens(4) == [
        [".Q..", "...Q", "Q...", "..Q."],
        ["..Q.", "Q...", "...Q", ".Q.."],
    ]


def test_trivial_sizes():
    assert solve_n_queens(1) == [["Q"]]
    assert solve_n_queens(2) == []
    assert solve_n_queens(3) == []
    assert solve_n_queens(0) == [[]]


def test_solution_counts_match_known_values():
    for n in range(9):
        assert len(solve_n_queens(n)) == KNOWN[n]
        assert count_n_queens(n) == KNOWN[n]


def test_all_solutions_valid_and_unique():
    sols = solve_n_queens(8)
    assert all(is_valid(b) for b in sols)
    assert len({tuple(b) for b in sols}) == len(sols)


def test_bitmask_counts_larger_n():
    assert count_n_queens(9) == KNOWN[9]
    assert count_n_queens(10) == KNOWN[10]


def test_brute_force_permutations_small_n():
    for n in range(1, 8):
        brute = sum(
            all(abs(p[i] - p[j]) != j - i for i in range(n) for j in range(i + 1, n))
            for p in itertools.permutations(range(n))
        )
        assert count_n_queens(n) == brute


def test_negative_n():
    with pytest.raises(ValueError):
        solve_n_queens(-1)
    with pytest.raises(ValueError):
        count_n_queens(-1)
