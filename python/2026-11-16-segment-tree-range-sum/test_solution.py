import random

import pytest

from solution import LazySegmentTree, SegmentTree


def test_basic_queries():
    st = SegmentTree([1, 3, 5, 7, 9, 11])
    assert st.query(0, 6) == 36
    assert st.query(1, 4) == 15
    assert st.query(2, 3) == 5
    assert st.query(3, 3) == 0


def test_point_update():
    st = SegmentTree([1, 3, 5])
    st.update(1, 2)
    assert st.query(0, 3) == 8
    assert st.query(1, 2) == 2


def test_non_power_of_two_and_single():
    st = SegmentTree([4])
    assert st.query(0, 1) == 4
    vals = list(range(7))
    st = SegmentTree(vals)
    for lo in range(8):
        for hi in range(lo, 8):
            assert st.query(lo, hi) == sum(vals[lo:hi])


def test_empty():
    assert SegmentTree([]).query(0, 0) == 0
    assert LazySegmentTree([]).query(0, 0) == 0


def test_bounds():
    st = SegmentTree([1, 2])
    with pytest.raises(IndexError):
        st.query(1, 3)
    with pytest.raises(IndexError):
        st.update(2, 0)
    with pytest.raises(IndexError):
        LazySegmentTree([1]).range_add(0, 2, 1)


def test_lazy_range_add():
    lt = LazySegmentTree([0] * 5)
    lt.range_add(1, 4, 2)  # [0,2,2,2,0]
    lt.range_add(0, 2, 1)  # [1,3,2,2,0]
    assert lt.query(0, 5) == 8
    assert lt.query(1, 2) == 3
    assert lt.query(3, 5) == 2
    lt.range_add(2, 2, 100)  # empty range: no-op
    assert lt.query(0, 5) == 8


def test_randomized_point_updates():
    rng = random.Random(0)
    vals = [rng.randint(-50, 50) for _ in range(37)]
    st = SegmentTree(vals)
    for _ in range(2000):
        if rng.random() < 0.4:
            i, v = rng.randrange(len(vals)), rng.randint(-50, 50)
            vals[i] = v
            st.update(i, v)
        else:
            lo = rng.randint(0, len(vals))
            hi = rng.randint(lo, len(vals))
            assert st.query(lo, hi) == sum(vals[lo:hi])


def test_randomized_lazy():
    rng = random.Random(1)
    for n in (1, 2, 13, 64):
        vals = [rng.randint(-9, 9) for _ in range(n)]
        lt = LazySegmentTree(vals)
        for _ in range(500):
            lo = rng.randint(0, n)
            hi = rng.randint(lo, n)
            if rng.random() < 0.5:
                d = rng.randint(-5, 5)
                lt.range_add(lo, hi, d)
                for i in range(lo, hi):
                    vals[i] += d
            else:
                assert lt.query(lo, hi) == sum(vals[lo:hi])
