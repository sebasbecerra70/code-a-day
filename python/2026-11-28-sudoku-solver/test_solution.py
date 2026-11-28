import pytest

from solution import is_valid_solution, parse, solve

EASY = """
53..7....
6..195...
.98....6.
8...6...3
4..8.3..1
7...2...6
.6....28.
...419..5
....8..79
"""

# "World's hardest sudoku" (Arto Inkala, 2012)
HARD = "8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4.."


def consistent_with_givens(puzzle, solution):
    return all(p == 0 or p == s for prow, srow in zip(puzzle, solution) for p, s in zip(prow, srow))


def test_easy_puzzle():
    p = parse(EASY)
    s = solve(p)
    assert s is not None and is_valid_solution(s) and consistent_with_givens(p, s)
    assert s[0] == [5, 3, 4, 6, 7, 8, 9, 1, 2]


def test_hard_puzzle():
    p = parse(HARD)
    s = solve(p)
    assert s is not None and is_valid_solution(s) and consistent_with_givens(p, s)


def test_input_not_mutated():
    p = parse(EASY)
    before = [row[:] for row in p]
    solve(p)
    assert p == before


def test_empty_grid_has_a_solution():
    s = solve([[0] * 9 for _ in range(9)])
    assert s is not None and is_valid_solution(s)


def test_already_solved():
    s = solve(parse(EASY))
    assert solve(s) == s


def test_conflicting_givens():
    p = parse(EASY)
    p[0][2] = 5  # duplicate 5 in row 0
    assert solve(p) is None


def test_unsolvable_without_direct_conflict():
    # Row 0 needs a 9 in its last cell, but column 8 already has a 9.
    p = [[0] * 9 for _ in range(9)]
    p[0] = [1, 2, 3, 4, 5, 6, 7, 8, 0]
    p[5][8] = 9
    assert solve(p) is None


def test_parse_validation():
    with pytest.raises(ValueError):
        parse("123")
    with pytest.raises(ValueError):
        parse("x" * 81)
    assert parse("." * 81) == [[0] * 9 for _ in range(9)]


def test_is_valid_solution_rejects_bad_grid():
    s = solve(parse(EASY))
    s[0][0], s[0][1] = s[0][1], s[0][0]  # rows still fine, columns break
    assert not is_valid_solution(s)
