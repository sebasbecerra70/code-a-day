import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

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

    static void runIdle(Elevator e) {
        for (int i = 0; i < 10_000 && !e.isIdle(); i++) e.step();
    }

    public static void main(String[] args) {
        run("idleElevatorStaysPut", () -> {
            Elevator e = new Elevator(0, 0, 10, 3);
            e.step();
            check(e.floor() == 3 && e.direction() == Direction.IDLE, "idle");
        });

        run("stopAtCurrentFloorOpensImmediately", () -> {
            Elevator e = new Elevator(0, 0, 10, 4);
            e.addStop(4);
            e.step();
            check(e.visited().equals(List.of(4)) && e.isIdle(), "served in place");
        });

        run("lookOrderGoingUp", () -> {
            Elevator e = new Elevator(0, 0, 10, 5);
            e.addStop(6); // makes it head up first
            e.step();
            e.addStop(9);
            e.addStop(2);
            e.addStop(8);
            runIdle(e);
            check(e.visited().equals(List.of(6, 8, 9, 2)), e.visited().toString());
        });

        run("doesNotOvershootLikeScan", () -> {
            Elevator e = new Elevator(0, 0, 100, 0);
            e.addStop(3);
            e.addStop(1);
            int ticks = 0;
            while (!e.isIdle()) {
                e.step();
                ticks++;
                check(e.floor() <= 3, "never travels past the last stop");
            }
            check(ticks == 3 + 2, "3 moves + 2 door openings");
        });

        run("outOfRangeRejected", () -> {
            Elevator e = new Elevator(0, 0, 10, 0);
            expectThrows(IllegalArgumentException.class, () -> e.addStop(11));
            expectThrows(IllegalArgumentException.class, () -> new ElevatorSystem(2, 0, 5).requestPickup(-1));
            expectThrows(IllegalArgumentException.class, () -> new ElevatorSystem(0, 0, 5));
        });

        run("costFunction", () -> {
            Elevator e = new Elevator(0, 0, 20, 5);
            check(e.costTo(9) == 4, "idle: distance");
            e.addStop(10);
            e.step(); // floor 6, going up
            check(e.costTo(8) == 2, "ahead on the way");
            check(e.costTo(2) == (10 - 6) + (10 - 2), "behind: finish sweep, come back");
        });

        run("dispatcherPicksNearestAndOnTheWay", () -> {
            ElevatorSystem sys = new ElevatorSystem(2, 0, 20);
            sys.requestDropoff(1, 15);
            for (int i = 0; i < 5; i++) sys.step(); // car 1 at floor 5 going up; car 0 idle at 0
            check(sys.car(1).floor() == 5, "car 1 position");
            check(sys.requestPickup(8) == 1, "car 1 passes floor 8 on the way up");
            check(sys.requestPickup(1) == 0, "car 0 is closer to floor 1");
            sys.runUntilIdle(1000);
            check(sys.car(1).visited().equals(List.of(8, 15)), sys.car(1).visited().toString());
            check(sys.car(0).visited().equals(List.of(1)), "car 0 served 1");
        });

        run("tieBreaksByLoad", () -> {
            ElevatorSystem sys = new ElevatorSystem(3, 0, 10);
            check(sys.requestPickup(0) == 0, "first car");
            check(sys.requestPickup(0) == 0, "already stopping there: same car");
            check(sys.requestPickup(5) == 1, "equal cost goes to the less loaded car");
        });

        run("randomizedSingleCarMatchesLookOrder", () -> {
            Random rnd = new Random(70);
            for (int t = 0; t < 300; t++) {
                int start = rnd.nextInt(30);
                Elevator e = new Elevator(0, 0, 29, start);
                TreeSet<Integer> stops = new TreeSet<>();
                for (int i = rnd.nextInt(10); i > 0; i--) stops.add(rnd.nextInt(30));
                stops.forEach(e::addStop);
                // Expected: serve current floor, go toward the nearest stop first, sweep, reverse.
                List<Integer> expected = new ArrayList<>();
                TreeSet<Integer> rest = new TreeSet<>(stops);
                if (rest.remove(start)) expected.add(start);
                Integer up = rest.higher(start), down = rest.lower(start);
                boolean goUp = down == null || (up != null && up - start <= start - down);
                List<Integer> ups = new ArrayList<>(rest.tailSet(start, false));
                List<Integer> downs = new ArrayList<>(rest.headSet(start, false).descendingSet());
                if (goUp) {
                    expected.addAll(ups);
                    expected.addAll(downs);
                } else {
                    expected.addAll(downs);
                    expected.addAll(ups);
                }
                runIdle(e);
                check(e.visited().equals(expected), e.visited() + " vs " + expected);
            }
        });

        run("randomizedSystemServesEveryRequest", () -> {
            Random rnd = new Random(71);
            ElevatorSystem sys = new ElevatorSystem(3, 0, 40);
            List<TreeSet<Integer>> pending = List.of(new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
            for (int tick = 0; tick < 2000; tick++) {
                if (rnd.nextInt(3) == 0) {
                    int f = rnd.nextInt(41);
                    pending.get(sys.requestPickup(f)).add(f);
                }
                sys.step();
                for (int c = 0; c < 3; c++) {
                    int fl = sys.car(c).floor();
                    check(fl >= 0 && fl <= 40, "in range");
                }
            }
            check(sys.runUntilIdle(10_000) < 10_000, "drains");
            for (int c = 0; c < 3; c++) {
                check(new TreeSet<>(sys.car(c).visited()).containsAll(pending.get(c)), "car " + c + " served all");
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
