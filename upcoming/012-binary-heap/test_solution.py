import heapq
import random

import pytest

from solution import MinHeap, heapsort


def test_push_pop_order():
    h = MinHeap()
    for x in [5, 3, 8, 1, 9, 2]:
        h.push(x)
    assert h.peek() == 1
    assert [h.pop() for _ in range(len(h))] == [1, 2, 3, 5, 8, 9]


def test_empty_heap_errors():
    h = MinHeap()
    assert not h
    with pytest.raises(IndexError):
        h.pop()
    with pytest.raises(IndexError):
        h.peek()


def test_heapify_constructor():
    h = MinHeap([7, 2, 9, 4, 4, 0])
    assert len(h) == 6
    assert [h.pop() for _ in range(6)] == [0, 2, 4, 4, 7, 9]


def test_duplicates_and_single_element():
    assert heapsort([3, 3, 3]) == [3, 3, 3]
    assert heapsort([42]) == [42]
    assert heapsort([]) == []


def test_key_function_makes_max_heap():
    h = MinHeap([1, 5, 3], key=lambda x: -x)
    assert [h.pop() for _ in range(3)] == [5, 3, 1]


def test_key_on_tuples_ignores_payload():
    tasks = [(2, "b"), (1, "a"), (3, "c")]
    assert heapsort(tasks, key=lambda t: t[0]) == [(1, "a"), (2, "b"), (3, "c")]


def test_pushpop():
    h = MinHeap([3, 5, 7])
    assert h.pushpop(1) == 1  # smaller than min: returned immediately
    assert h.pushpop(4) == 3
    assert sorted(h._a) == [4, 5, 7]
    assert MinHeap().pushpop(9) == 9


def test_heap_invariant_holds():
    rng = random.Random(3)
    h = MinHeap(rng.randint(0, 100) for _ in range(200))
    a = h._a
    for i in range(1, len(a)):
        assert a[(i - 1) // 2] <= a[i]


def test_randomized_against_heapq():
    rng = random.Random(1)
    mine, ref = MinHeap(), []
    for _ in range(5000):
        op = rng.random()
        if op < 0.5 or not ref:
            x = rng.randint(-50, 50)
            mine.push(x)
            heapq.heappush(ref, x)
        elif op < 0.8:
            assert mine.pop() == heapq.heappop(ref)
        else:
            x = rng.randint(-50, 50)
            assert mine.pushpop(x) == heapq.heappushpop(ref, x)
        assert len(mine) == len(ref)


def test_heapsort_random():
    rng = random.Random(9)
    for _ in range(50):
        data = [rng.random() for _ in range(rng.randint(0, 100))]
        assert heapsort(data) == sorted(data)
