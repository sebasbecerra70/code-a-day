import copy
import random

import pytest

from solution import rotate_clockwise, rotate_layers, spiral_matrix, spiral_order


def test_spiral_square_and_rectangles():
    assert spiral_order([[1, 2, 3], [4, 5, 6], [7, 8, 9]]) == [1, 2, 3, 6, 9, 8, 7, 4, 5]
    assert spiral_order([[1, 2, 3, 4], [5, 6, 7, 8], [9, 10, 11, 12]]) == [1, 2, 3, 4, 8, 12, 11, 10, 9, 5, 6, 7]
    assert spiral_order([[1, 2], [3, 4], [5, 6]]) == [1, 2, 4, 6, 5, 3]


def test_spiral_degenerate_shapes():
    assert spiral_order([]) == []
    assert spiral_order([[]]) == []
    assert spiral_order([[7]]) == [7]
    assert spiral_order([[1, 2, 3]]) == [1, 2, 3]
    assert spiral_order([[1], [2], [3]]) == [1, 2, 3]


def test_spiral_matrix_generation():
    assert spiral_matrix(3) == [[1, 2, 3], [8, 9, 4], [7, 6, 5]]
    assert spiral_matrix(1) == [[1]]
    assert spiral_matrix(0) == []
    for n in range(1, 8):
        assert spiral_order(spiral_matrix(n)) == list(range(1, n * n + 1))


@pytest.mark.parametrize("rotate", [rotate_clockwise, rotate_layers])
def test_rotate_examples(rotate):
    m = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
    rotate(m)
    assert m == [[7, 4, 1], [8, 5, 2], [9, 6, 3]]
    single, empty = [[1]], []
    rotate(single)
    rotate(empty)
    assert single == [[1]] and empty == []
    with pytest.raises(ValueError):
        rotate([[1, 2]])


def test_rotate_four_times_is_identity_and_methods_agree():
    rng = random.Random(0)
    for n in range(0, 9):
        m = [[rng.randint(0, 99) for _ in range(n)] for _ in range(n)]
        expected = [list(row) for row in zip(*m[::-1])]  # reference rotation
        a, b = copy.deepcopy(m), copy.deepcopy(m)
        rotate_clockwise(a)
        rotate_layers(b)
        assert a == b == expected
        for _ in range(3):
            rotate_clockwise(a)
        assert a == m


def test_randomized_spiral_is_permutation_and_starts_on_border():
    rng = random.Random(1)
    for _ in range(100):
        r, c = rng.randint(1, 7), rng.randint(1, 7)
        m = [[i * c + j for j in range(c)] for i in range(r)]
        s = spiral_order(m)
        assert sorted(s) == list(range(r * c))
        assert s[:c] == m[0]
