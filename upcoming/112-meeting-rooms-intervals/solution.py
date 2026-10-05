"""Meeting-room interval problems. Intervals are half-open [start, end):
a meeting ending at 10 does not conflict with one starting at 10."""

from __future__ import annotations

import heapq

Interval = tuple[int, int]


def can_attend_all(meetings: list[Interval]) -> bool:
    """Meeting Rooms I: True if no two meetings overlap."""
    ordered = sorted(meetings)
    return all(prev[1] <= cur[0] for prev, cur in zip(ordered, ordered[1:]))


def min_rooms(meetings: list[Interval]) -> int:
    """Meeting Rooms II: minimum rooms needed, via a min-heap of end times."""
    ends: list[int] = []
    for start, end in sorted(meetings):
        if ends and ends[0] <= start:
            heapq.heapreplace(ends, end)  # reuse the room that frees up earliest
        else:
            heapq.heappush(ends, end)
    return len(ends)


def min_rooms_sweep(meetings: list[Interval]) -> int:
    """Same answer via a sweep line over +1/-1 events. Ends sort before starts at equal times."""
    events = sorted([(s, 1) for s, _ in meetings] + [(e, -1) for _, e in meetings])
    best = cur = 0
    for _, delta in events:
        cur += delta
        best = max(best, cur)
    return best


def assign_rooms(meetings: list[Interval]) -> list[int]:
    """Assigns each meeting (by input index) a room number using the minimum number of rooms.

    Freed rooms are reused lowest-number-first.
    """
    order = sorted(range(len(meetings)), key=lambda i: meetings[i])
    busy: list[tuple[int, int]] = []  # (end, room)
    free: list[int] = []  # free room numbers
    rooms = [0] * len(meetings)
    next_room = 0
    for i in order:
        start, end = meetings[i]
        while busy and busy[0][0] <= start:
            heapq.heappush(free, heapq.heappop(busy)[1])
        if free:
            room = heapq.heappop(free)
        else:
            room, next_room = next_room, next_room + 1
        rooms[i] = room
        heapq.heappush(busy, (end, room))
    return rooms


def max_non_overlapping(meetings: list[Interval]) -> list[Interval]:
    """Activity selection: largest set of non-overlapping meetings (greedy by earliest end)."""
    chosen: list[Interval] = []
    last_end = float("-inf")
    for s, e in sorted(meetings, key=lambda m: m[1]):
        if s >= last_end:
            chosen.append((s, e))
            last_end = e
    return chosen


def free_slots(schedules: list[list[Interval]], day: Interval) -> list[Interval]:
    """Common free time across people within `day` (merge all busy intervals, take the gaps)."""
    busy = sorted(iv for person in schedules for iv in person)
    out: list[Interval] = []
    cursor, day_end = day
    for s, e in busy:
        if s > cursor:
            out.append((cursor, min(s, day_end)))
        cursor = max(cursor, e)
        if cursor >= day_end:
            break
    if cursor < day_end:
        out.append((cursor, day_end))
    return [(s, e) for s, e in out if s < e]
