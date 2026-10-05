import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

enum Direction { UP, DOWN, IDLE }

/**
 * One car running the LOOK algorithm: keep moving in the current direction
 * while there are stops ahead, then reverse. Unlike SCAN, it doesn't travel to
 * the end of the shaft when nothing is waiting there.
 */
class Elevator {
    final int id;
    private final int minFloor, maxFloor;
    private int floor;
    private Direction direction = Direction.IDLE;
    private final TreeSet<Integer> stops = new TreeSet<>();
    private final List<Integer> visited = new ArrayList<>();

    Elevator(int id, int minFloor, int maxFloor, int startFloor) {
        this.id = id;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.floor = startFloor;
    }

    void addStop(int f) {
        if (f < minFloor || f > maxFloor) throw new IllegalArgumentException("floor out of range: " + f);
        stops.add(f);
    }

    /** Advances one tick: either opens doors at a stop or moves one floor. */
    void step() {
        if (stops.remove(floor)) {
            visited.add(floor); // doors open this tick
            if (stops.isEmpty()) direction = Direction.IDLE;
            return;
        }
        if (stops.isEmpty()) {
            direction = Direction.IDLE;
            return;
        }
        if (direction == Direction.IDLE) {
            // Head toward the nearest stop; ties go up.
            Integer up = stops.higher(floor), down = stops.lower(floor);
            direction = down == null || (up != null && up - floor <= floor - down) ? Direction.UP : Direction.DOWN;
        }
        // LOOK: reverse only when nothing is left ahead.
        if (direction == Direction.UP && stops.higher(floor) == null) direction = Direction.DOWN;
        else if (direction == Direction.DOWN && stops.lower(floor) == null) direction = Direction.UP;
        floor += direction == Direction.UP ? 1 : -1;
    }

    /**
     * Estimated ticks of travel before this car could reach {@code f}: direct if
     * idle or if f is ahead in the current direction, otherwise finish the
     * current sweep and come back.
     */
    int costTo(int f) {
        if (direction == Direction.IDLE || stops.isEmpty()) return Math.abs(f - floor);
        if (direction == Direction.UP) {
            if (f >= floor) return f - floor;
            int top = Math.max(stops.last(), floor);
            return (top - floor) + (top - f);
        } else {
            if (f <= floor) return floor - f;
            int bottom = Math.min(stops.first(), floor);
            return (floor - bottom) + (f - bottom);
        }
    }

    int floor() { return floor; }
    Direction direction() { return direction; }
    boolean isIdle() { return stops.isEmpty(); }
    List<Integer> visited() { return Collections.unmodifiableList(visited); }
    int pendingStops() { return stops.size(); }
    boolean hasStop(int f) { return stops.contains(f); }
}

/** Assigns hall calls to the car with the lowest estimated cost, then drives all cars. */
class ElevatorSystem {
    private final List<Elevator> cars = new ArrayList<>();
    private final int minFloor, maxFloor;

    ElevatorSystem(int cars, int minFloor, int maxFloor) {
        if (cars <= 0 || minFloor >= maxFloor) throw new IllegalArgumentException();
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        for (int i = 0; i < cars; i++) this.cars.add(new Elevator(i, minFloor, maxFloor, minFloor));
    }

    /** Hall call: someone presses a button on floor f. Returns the id of the car assigned. */
    int requestPickup(int f) {
        if (f < minFloor || f > maxFloor) throw new IllegalArgumentException("floor out of range: " + f);
        // A car already stopping there absorbs the call for free.
        for (Elevator e : cars) if (e.hasStop(f)) return e.id;
        Elevator best = cars.get(0);
        for (Elevator e : cars) {
            // Prefer lower cost, then fewer pending stops (spreads load), then lower id.
            int c = e.costTo(f), bc = best.costTo(f);
            if (c < bc || (c == bc && e.pendingStops() < best.pendingStops())) best = e;
        }
        best.addStop(f);
        return best.id;
    }

    /** Car call: a passenger inside car {@code id} presses floor f. */
    void requestDropoff(int id, int f) {
        cars.get(id).addStop(f);
    }

    void step() {
        for (Elevator e : cars) e.step();
    }

    /** Runs until every car is idle or the tick limit is hit; returns ticks used. */
    int runUntilIdle(int maxTicks) {
        int t = 0;
        while (t < maxTicks && cars.stream().anyMatch(e -> !e.isIdle())) {
            step();
            t++;
        }
        return t;
    }

    Elevator car(int id) {
        return cars.get(id);
    }
}
