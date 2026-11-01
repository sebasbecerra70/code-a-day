import itertools
import random

from solution import lis, lis_length, lis_quadratic


def is_strictly_increasing_subsequence(sub, seq):
    it = iter(seq)
    return all(a < b for a, b in zip(sub, sub[1:])) and all(x in it for x in sub)


def test_classic():
    nums = [10, 9, 2, 5, 3, 7, 101, 18]
    assert lis_length(nums) == 4
    s = lis(nums)
    assert len(s) == 4 and is_strictly_increasing_subsequence(s, nums)


def test_empty_and_single():
    assert lis_length([]) == 0
    assert lis([]) == []
    assert lis([7]) == [7]


def test_duplicates_do_not_count():
    assert lis_length([7, 7, 7, 7]) == 1
    assert lis_length([1, 3, 3, 5]) == 3


def test_sorted_and_reversed():
    assert lis(list(range(10))) == list(range(10))
    assert lis_length(list(range(10, 0, -1))) == 1


def test_negative_numbers():
    assert lis([-5, -1, -3, 0, -2, 4]) in ([-5, -1, 0, 4], [-5, -3, 0, 4], [-5, -3, -2, 4])


def test_reconstruction_is_valid_on_tricky_case():
    # Later small values replace tails but must not corrupt the recovered path.
    nums = [3, 4, 5, 1, 2, 6]
    s = lis(nums)
    assert len(s) == 4 and is_strictly_increasing_subsequence(s, nums)


def test_against_brute_force():
    rng = random.Random(8)
    for _ in range(200):
        nums = [rng.randint(0, 6) for _ in range(rng.randint(0, 10))]
        best = 0
        for r in range(len(nums), 0, -1):
            if any(all(a < b for a, b in zip(c, c[1:])) for c in itertools.combinations(nums, r)):
                best = r
                break
        assert lis_length(nums) == best
        assert lis_quadratic(nums) == best
        s = lis(nums)
        assert len(s) == best and is_strictly_increasing_subsequence(s, nums)


def test_large_random_matches_quadratic():
    rng = random.Random(1)
    nums = [rng.randint(0, 1000) for _ in range(1500)]
    assert lis_length(nums) == lis_quadratic(nums) == len(lis(nums))
