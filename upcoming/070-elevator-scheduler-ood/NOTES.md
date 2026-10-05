# Elevator scheduler (LOOK algorithm + dispatcher)

**Problem:** Design an elevator system: each car decides which floor to serve next, and a dispatcher assigns hall calls (a button pressed on a floor) to one of several cars. Simulate it in discrete ticks.

## Approach
- **Elevator state:** current floor, direction (`UP`/`DOWN`/`IDLE`) and a `TreeSet<Integer>` of pending stops. The sorted set gives O(log n) `higher`/`lower` lookups.
- **LOOK algorithm (per tick):**
  1. If the current floor is a stop, open the doors (that takes the tick).
  2. If idle with work pending, head toward the nearest stop.
  3. Keep going in the current direction while any stop lies ahead; otherwise reverse.
- **Dispatcher:** if a car already has the floor queued, it takes the call. Otherwise it uses a **cost function**: an idle car costs its distance. A moving car costs its distance if the floor is *ahead* in its direction, and otherwise the distance to finish its sweep and come back. Ties go to the car with fewer pending stops, then the lowest id.
- Tests compare a single car's visit order with an independent LOOK formula on random stop sets, and check that the multi-car simulation eventually serves every request.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| addStop / step | O(log s) for s pending stops | O(s) |
| costTo | O(1) (TreeSet first/last) | — |
| requestPickup | O(cars) | — |

## Interview talking points
- Disk-scheduling analogy: FCFS is fair but zig-zags; SSTF (nearest first) can starve far floors; SCAN sweeps end to end; LOOK reverses at the last request, which is what real elevators approximate.
- A fuller design splits stops into *up* and *down* hall calls: someone on floor 7 going down shouldn't be picked up by a car passing upward.
- Dispatch goals conflict: minimize average wait, minimize the worst-case wait, or minimize energy. Real systems (destination dispatch) group passengers by destination at the lobby.
- Model it as a state machine (Idle → MovingUp/MovingDown → DoorsOpen). Use the State pattern if behaviors per state grow.
- Tick-based simulation with no real threads makes it deterministic and testable. In production, the controller is event-driven and talks to sensors.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
