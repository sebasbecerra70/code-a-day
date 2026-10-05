# Parking lot (object-oriented design)

**Problem:** Design a multi-level parking lot: motorcycles, cars and buses; spots of three sizes; a bus needs several adjacent large spots. Issue tickets on entry, compute fees on exit and report availability.

## Approach
- **Entities:** `Vehicle` (plate + type), `ParkingSpot` (level, index, size, occupant), `Level` (an ordered row of spots), `Ticket` (id, vehicle, spots, entry time) and `ParkingLot` (the façade).
- **Encode rules in data, not `if`s:** `VehicleType` carries its minimum spot size, the number of spots it needs and its hourly rate. `SpotSize` ordinals are ordered, so "fits" is a single comparison.
- **Allocation:** try levels from lowest to highest. Within a level, try spot sizes from smallest adequate to largest, and look for a run of `spotsNeeded` adjacent free spots. Using the smallest adequate size keeps large spots free for buses.
- **Fees:** a `FeePolicy` strategy (a grace period, then per *started* hour). Time comes from an injected `java.time.Clock`, so tests can advance it deterministically.
- **Integrity:** a plate can't be parked twice, tickets can't be reused, and the public methods are `synchronized` because multiple entrance gates call them concurrently.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| park | O(levels × spots × sizes) scan | O(1) |
| leave | O(spots in ticket) | — |
| freeSpots | O(total spots) | O(1) |

## Interview talking points
- Start by clarifying requirements: vehicle and spot types, multi-spot vehicles, pricing, multiple entrances, reservations, EV charging, display boards.
- **Scaling up the scan:** keep a free-list per (level, size), e.g. a `TreeSet` of free indices. For adjacency, store free runs in an interval map. That makes parking O(log n).
- Injecting `Clock` and `FeePolicy` keeps the core testable and open for extension (weekend rates, monthly passes) without modifying `ParkingLot`.
- Concurrency: a single lot-wide lock is fine for a handful of gates. For more throughput, lock per level, or claim spots with compare-and-set.
- Persisting tickets (so a crash doesn't lose who's parked where) and payment are separate services in a real system.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
