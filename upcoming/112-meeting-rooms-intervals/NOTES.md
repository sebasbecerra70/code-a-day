# Meeting rooms (interval scheduling)

**Problem:** A family of interval questions over half-open meetings `[start, end)`:
1. Can one person attend all meetings?
2. What is the minimum number of rooms needed?
3. Which room does each meeting get?
4. What is the largest set of meetings one room can host?
5. When is everyone free during the day?

## Approach
- **Can attend all:** sort by start, then check `prev.end <= cur.start` for neighbors.
- **Min rooms (heap):** sort by start. Keep a min-heap of end times for rooms in use. If the earliest-ending room is free by this meeting's start, reuse it (`heapreplace`). Otherwise open a new room. The heap size at the end is the answer.
- **Min rooms (sweep line):** turn meetings into `(+1 at start, -1 at end)` events and sort them. Because `-1 < +1`, ends at time t are processed before starts at t, which encodes the half-open rule. The running maximum is the answer.
- **Assign rooms:** the heap approach, but also track `(end, room)` and a heap of free room ids so the lowest-numbered free room is reused.
- **Max non-overlapping (activity selection):** greedy by earliest *end*. An exchange argument shows that finishing earliest never hurts.
- **Common free time:** merge everyone's busy intervals in one sorted sweep and emit the gaps, clipped to the working day.

## Complexity
| Problem | Time | Space |
|--------|------|------|
| Can attend all | O(n log n) | O(n) |
| Min rooms (heap or sweep) | O(n log n) | O(n) |
| Assign rooms | O(n log n) | O(n) |
| Activity selection | O(n log n) | O(n) |
| Free slots | O(N log N), N = total intervals | O(N) |

## Interview talking points
- State the boundary convention up front: does `[1,5]` conflict with `[5,8]`? Half-open intervals avoid off-by-ones.
- Min rooms equals the maximum overlap at any point. That's the interval graph's clique number, and greedy coloring achieves it (interval graphs are perfect).
- Sorting by start is right for room counting, while sorting by end is right for maximizing count. Mixing them up is a common bug.
- Weighted activity selection needs DP + binary search (O(n log n)), not greedy.
- Follow-up: "Meeting Rooms III" (LeetCode 2402), where delayed meetings wait for the lowest free room. It uses the same two-heap pattern as `assign_rooms`.

## Run
From this folder: `python -m pytest -q`
