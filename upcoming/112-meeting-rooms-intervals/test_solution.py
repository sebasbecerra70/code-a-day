import itertools
import random

from solution import assign_rooms, can_attend_all, free_slots, max_non_overlapping, min_rooms, min_rooms_sweep


def overlaps(a, b):
    return a[0] < b[1] and b[0] < a[1]


def test_can_attend_all():
    assert can_attend_all([(7, 10), (2, 4)])
    assert not can_attend_all([(0, 30), (5, 10), (15, 20)])
    assert can_attend_all([(1, 5), (5, 8)])  # touching is fine
    assert can_attend_all([])


def test_min_rooms_examples():
    for f in (min_rooms, min_rooms_sweep):
        assert f([(0, 30), (5, 10), (15, 20)]) == 2
        assert f([(7, 10), (2, 4)]) == 1
        assert f([]) == 0
        assert f([(1, 5), (5, 10), (10, 15)]) == 1
        assert f([(1, 10)] * 4) == 4


def test_assign_rooms_is_valid_and_minimal():
    meetings = [(0, 30), (5, 10), (15, 20), (10, 15), (25, 35)]
    rooms = assign_rooms(meetings)
    assert max(rooms) + 1 == min_rooms(meetings)
    for i, j in itertools.combinations(range(len(meetings)), 2):
        if rooms[i] == rooms[j]:
            assert not overlaps(meetings[i], meetings[j])


def test_assign_rooms_reuses_lowest_free_room():
    # Rooms 0 and 1 both free up before t=10; the next meeting takes room 0.
    assert assign_rooms([(0, 3), (0, 5), (10, 12)]) == [0, 1, 0]


def test_max_non_overlapping():
    assert max_non_overlapping([(1, 3), (2, 4), (3, 5), (0, 7), (5, 9), (8, 10)]) == [(1, 3), (3, 5), (5, 9)]
    assert max_non_overlapping([]) == []


def test_free_slots():
    a = [(9, 10), (12, 13)]
    b = [(9, 11), (14, 15)]
    assert free_slots([a, b], (8, 17)) == [(8, 9), (11, 12), (13, 14), (15, 17)]
    assert free_slots([[(0, 24)]], (8, 17)) == []
    assert free_slots([], (8, 17)) == [(8, 17)]
    assert free_slots([[(5, 9), (16, 20)]], (8, 17)) == [(9, 16)]


def brute_max_overlap(meetings):
    points = {s for s, _ in meetings}
    return max((sum(s <= p < e for s, e in meetings) for p in points), default=0)


def test_randomized():
    rng = random.Random(8)
    for _ in range(300):
        meetings = []
        for _ in range(rng.randint(0, 8)):
            s = rng.randint(0, 20)
            meetings.append((s, s + rng.randint(1, 8)))
        expected = brute_max_overlap(meetings)
        assert min_rooms(meetings) == min_rooms_sweep(meetings) == expected
        assert can_attend_all(meetings) == (expected <= 1)
        rooms = assign_rooms(meetings)
        assert len(set(rooms)) == expected
        for i, j in itertools.combinations(range(len(meetings)), 2):
            if rooms[i] == rooms[j]:
                assert not overlaps(meetings[i], meetings[j])
        if len(meetings) <= 7:
            best = max(
                (len(c) for r in range(len(meetings) + 1) for c in itertools.combinations(meetings, r)
                 if all(not overlaps(x, y) for x, y in itertools.combinations(c, 2))),
                default=0,
            )
            assert len(max_non_overlapping(meetings)) == best
