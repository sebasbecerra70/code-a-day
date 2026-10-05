import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class SolutionTest {
    private static int passed = 0;

    interface TestBody {
        void run() throws Exception;
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void expectThrows(Class<? extends Throwable> type, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + t, t);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " to be thrown");
    }

    static void run(String name, TestBody body) {
        try {
            body.run();
        } catch (Throwable t) {
            throw new AssertionError(name + " failed: " + t.getMessage(), t);
        }
        passed++;
    }

    /** A clock the test can advance. */
    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2025-01-01T08:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    static final SpotSize S = SpotSize.SMALL, C = SpotSize.COMPACT, L = SpotSize.LARGE;

    static ParkingLot lot(MutableClock clock, List<List<SpotSize>> layouts) {
        return new ParkingLot(layouts, clock, FeePolicy.hourly(Duration.ofMinutes(15)));
    }

    static Vehicle car(String p) {
        return new Vehicle(p, VehicleType.CAR);
    }

    public static void main(String[] args) {
        run("parkAndLeave", () -> {
            MutableClock clock = new MutableClock();
            ParkingLot lot = lot(clock, List.of(List.of(S, C, L)));
            Ticket t = lot.park(car("A1")).orElseThrow();
            check(t.spots().size() == 1 && t.spots().get(0).size == C, "car takes compact");
            check(lot.freeSpots().equals(Map.of(S, 1, C, 0, L, 1)), lot.freeSpots().toString());
            clock.advance(Duration.ofMinutes(90));
            check(lot.leave(t.id()) == 600, "2 started hours * 300");
            check(lot.freeSpots().get(C) == 1 && lot.parkedCount() == 0, "freed");
        });

        run("smallestAdequateSpotPreferred", () -> {
            ParkingLot lot = lot(new MutableClock(), List.of(List.of(L, C, S)));
            Ticket m = lot.park(new Vehicle("M", VehicleType.MOTORCYCLE)).orElseThrow();
            check(m.spots().get(0).size == S, "motorcycle takes small");
            Ticket m2 = lot.park(new Vehicle("M2", VehicleType.MOTORCYCLE)).orElseThrow();
            check(m2.spots().get(0).size == C, "then compact");
            check(lot.park(car("C")).orElseThrow().spots().get(0).size == L, "car falls back to large");
        });

        run("vehicleTooBigOrLotFull", () -> {
            ParkingLot lot = lot(new MutableClock(), List.of(List.of(S, S)));
            check(lot.park(car("C")).isEmpty(), "car can't use small spots");
            lot.park(new Vehicle("M1", VehicleType.MOTORCYCLE));
            lot.park(new Vehicle("M2", VehicleType.MOTORCYCLE));
            check(lot.park(new Vehicle("M3", VehicleType.MOTORCYCLE)).isEmpty(), "full");
        });

        run("busNeedsThreeAdjacentLarge", () -> {
            ParkingLot lot = lot(new MutableClock(), List.of(List.of(L, L, C, L, L), List.of(C, L, L, L)));
            Ticket bus = lot.park(new Vehicle("BUS", VehicleType.BUS)).orElseThrow();
            check(bus.spots().size() == 3, "three spots");
            check(bus.spots().stream().allMatch(s -> s.level == 1), "level 0 has no run of 3");
            check(bus.spots().get(0).index == 1 && bus.spots().get(2).index == 3, "adjacent");
            check(lot.park(new Vehicle("BUS2", VehicleType.BUS)).isEmpty(), "no more room");
        });

        run("lowerLevelFirst", () -> {
            ParkingLot lot = lot(new MutableClock(), List.of(List.of(C), List.of(C)));
            check(lot.park(car("A")).orElseThrow().spots().get(0).level == 0, "level 0");
            check(lot.park(car("B")).orElseThrow().spots().get(0).level == 1, "level 1");
        });

        run("feePolicy", () -> {
            FeePolicy f = FeePolicy.hourly(Duration.ofMinutes(15));
            check(f.feeCents(VehicleType.CAR, Duration.ofMinutes(15)) == 0, "grace period");
            check(f.feeCents(VehicleType.CAR, Duration.ofMinutes(16)) == 300, "first hour");
            check(f.feeCents(VehicleType.CAR, Duration.ofMinutes(60)) == 300, "exactly one hour");
            check(f.feeCents(VehicleType.CAR, Duration.ofMinutes(61)) == 600, "started second hour");
            check(f.feeCents(VehicleType.BUS, Duration.ofHours(3)) == 3000, "bus rate");
        });

        run("doubleParkAndBadTicketRejected", () -> {
            ParkingLot lot = lot(new MutableClock(), List.of(List.of(C, C)));
            Ticket t = lot.park(car("X")).orElseThrow();
            expectThrows(IllegalStateException.class, () -> lot.park(car("X")));
            lot.leave(t.id());
            expectThrows(IllegalArgumentException.class, () -> lot.leave(t.id()));
            expectThrows(IllegalArgumentException.class, () -> lot.leave(999));
            check(lot.park(car("X")).isPresent(), "can re-park after leaving");
        });

        run("randomizedSpotAccounting", () -> {
            Random rnd = new Random(66);
            List<SpotSize> row = new ArrayList<>();
            for (int i = 0; i < 30; i++) row.add(SpotSize.values()[rnd.nextInt(3)]);
            ParkingLot lot = lot(new MutableClock(), List.of(row, row));
            int total = 60;
            List<Ticket> parked = new ArrayList<>();
            for (int op = 0; op < 3000; op++) {
                if (!parked.isEmpty() && rnd.nextInt(3) == 0) {
                    lot.leave(parked.remove(rnd.nextInt(parked.size())).id());
                } else {
                    VehicleType type = VehicleType.values()[rnd.nextInt(3)];
                    Optional<Ticket> t = lot.park(new Vehicle("P" + op, type));
                    t.ifPresent(parked::add);
                }
                int used = parked.stream().mapToInt(t -> t.spots().size()).sum();
                int free = lot.freeSpots().values().stream().mapToInt(Integer::intValue).sum();
                check(used + free == total, "spot accounting");
                Set<ParkingSpot> seen = new HashSet<>();
                for (Ticket t : parked) for (ParkingSpot s : t.spots()) check(seen.add(s), "spot double-booked");
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
