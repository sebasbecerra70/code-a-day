import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Spot sizes, ordered so that a larger ordinal can hold anything a smaller one can. */
enum SpotSize { SMALL, COMPACT, LARGE }

enum VehicleType {
    MOTORCYCLE(SpotSize.SMALL, 1, 100),
    CAR(SpotSize.COMPACT, 1, 300),
    BUS(SpotSize.LARGE, 3, 1000); // a bus takes 3 adjacent large spots

    final SpotSize minSize;
    final int spotsNeeded;
    final long centsPerHour;

    VehicleType(SpotSize minSize, int spotsNeeded, long centsPerHour) {
        this.minSize = minSize;
        this.spotsNeeded = spotsNeeded;
        this.centsPerHour = centsPerHour;
    }
}

record Vehicle(String plate, VehicleType type) {}

class ParkingSpot {
    final int level, index;
    final SpotSize size;
    private Vehicle occupant;

    ParkingSpot(int level, int index, SpotSize size) {
        this.level = level;
        this.index = index;
        this.size = size;
    }

    boolean isFree() {
        return occupant == null;
    }

    boolean canFit(VehicleType t) {
        return isFree() && size.compareTo(t.minSize) >= 0;
    }

    void occupy(Vehicle v) {
        occupant = v;
    }

    void release() {
        occupant = null;
    }
}

/** A row of spots. Adjacency matters for multi-spot vehicles. */
class Level {
    final int number;
    final List<ParkingSpot> spots = new ArrayList<>();

    Level(int number, List<SpotSize> layout) {
        this.number = number;
        for (int i = 0; i < layout.size(); i++) spots.add(new ParkingSpot(number, i, layout.get(i)));
    }

    /**
     * Finds the first run of {@code spotsNeeded} adjacent spots that fit, preferring
     * the smallest adequate spot size so big spots stay free for big vehicles.
     */
    Optional<List<ParkingSpot>> findSpots(VehicleType t) {
        for (SpotSize size : SpotSize.values()) {
            if (size.compareTo(t.minSize) < 0) continue;
            int run = 0;
            for (int i = 0; i < spots.size(); i++) {
                ParkingSpot s = spots.get(i);
                run = s.canFit(t) && s.size == size ? run + 1 : 0;
                if (run == t.spotsNeeded) return Optional.of(List.copyOf(spots.subList(i - run + 1, i + 1)));
            }
        }
        return Optional.empty();
    }
}

record Ticket(long id, Vehicle vehicle, List<ParkingSpot> spots, Instant entry) {}

/** Fee policy: billed per started hour, with a grace period. */
interface FeePolicy {
    long feeCents(VehicleType type, Duration stay);

    static FeePolicy hourly(Duration grace) {
        return (type, stay) -> {
            if (stay.compareTo(grace) <= 0) return 0;
            long minutes = stay.toMinutes();
            long hours = (minutes + 59) / 60; // round up to whole hours
            return Math.max(1, hours) * type.centsPerHour;
        };
    }
}

class ParkingLot {
    private final List<Level> levels = new ArrayList<>();
    private final Map<Long, Ticket> active = new HashMap<>();
    private final Map<String, Long> ticketByPlate = new HashMap<>();
    private final Clock clock;
    private final FeePolicy fees;
    private long nextTicketId = 1;

    ParkingLot(List<List<SpotSize>> layouts, Clock clock, FeePolicy fees) {
        for (int i = 0; i < layouts.size(); i++) levels.add(new Level(i, layouts.get(i)));
        this.clock = clock;
        this.fees = fees;
    }

    /** Parks on the lowest level with room. Empty if the lot is full for this vehicle type. */
    synchronized Optional<Ticket> park(Vehicle v) {
        if (ticketByPlate.containsKey(v.plate())) throw new IllegalStateException(v.plate() + " is already parked");
        for (Level level : levels) {
            Optional<List<ParkingSpot>> spots = level.findSpots(v.type());
            if (spots.isPresent()) {
                spots.get().forEach(s -> s.occupy(v));
                Ticket t = new Ticket(nextTicketId++, v, spots.get(), clock.instant());
                active.put(t.id(), t);
                ticketByPlate.put(v.plate(), t.id());
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    /** Frees the spots and returns the fee owed. */
    synchronized long leave(long ticketId) {
        Ticket t = active.remove(ticketId);
        if (t == null) throw new IllegalArgumentException("unknown or already used ticket " + ticketId);
        ticketByPlate.remove(t.vehicle().plate());
        t.spots().forEach(ParkingSpot::release);
        return fees.feeCents(t.vehicle().type(), Duration.between(t.entry(), clock.instant()));
    }

    synchronized Map<SpotSize, Integer> freeSpots() {
        Map<SpotSize, Integer> free = new EnumMap<>(SpotSize.class);
        for (SpotSize s : SpotSize.values()) free.put(s, 0);
        for (Level l : levels) for (ParkingSpot s : l.spots) if (s.isFree()) free.merge(s.size, 1, Integer::sum);
        return free;
    }

    synchronized int parkedCount() {
        return active.size();
    }
}
