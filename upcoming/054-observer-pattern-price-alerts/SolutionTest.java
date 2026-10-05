import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    public static void main(String[] args) {
        run("notifiesOnlyMatchingSymbol", () -> {
            PriceFeed feed = new PriceFeed();
            List<PriceUpdate> got = new ArrayList<>();
            feed.subscribe("AAPL", got::add);
            feed.publish("AAPL", 100);
            feed.publish("MSFT", 200);
            feed.publish("AAPL", 110);
            check(got.size() == 2, "two AAPL ticks");
            check(got.get(0).equals(new PriceUpdate("AAPL", 100, 100)), "first tick old == new");
            check(got.get(1).equals(new PriceUpdate("AAPL", 100, 110)), "second tick");
        });

        run("unchangedPriceNotBroadcast", () -> {
            PriceFeed feed = new PriceFeed();
            List<PriceUpdate> got = new ArrayList<>();
            feed.subscribe("X", got::add);
            feed.publish("X", 5);
            check(feed.publish("X", 5) == 0, "no-op tick");
            check(got.size() == 1, "one notification");
        });

        run("wildcardSubscriber", () -> {
            PriceFeed feed = new PriceFeed();
            List<String> seen = new ArrayList<>();
            feed.subscribeAll(u -> seen.add(u.symbol()));
            feed.publish("A", 1);
            feed.publish("B", 1);
            check(seen.equals(List.of("A", "B")), seen.toString());
        });

        run("unsubscribeIsIdempotent", () -> {
            PriceFeed feed = new PriceFeed();
            List<PriceUpdate> got = new ArrayList<>();
            Subscription s = feed.subscribe("A", got::add);
            feed.publish("A", 1);
            s.close();
            s.close();
            feed.publish("A", 2);
            check(got.size() == 1 && feed.listenerCount("A") == 0, "removed");
        });

        run("unsubscribeDuringNotification", () -> {
            PriceFeed feed = new PriceFeed();
            List<String> log = new ArrayList<>();
            Subscription[] self = new Subscription[1];
            self[0] = feed.subscribe("A", u -> {
                log.add("once");
                self[0].close(); // removing itself mid-iteration must be safe
            });
            feed.subscribe("A", u -> log.add("other"));
            feed.publish("A", 1);
            feed.publish("A", 2);
            check(log.equals(List.of("once", "other", "other")), log.toString());
        });

        run("failingListenerIsolated", () -> {
            List<Throwable> errors = new ArrayList<>();
            PriceFeed feed = new PriceFeed(errors::add);
            List<PriceUpdate> got = new ArrayList<>();
            feed.subscribe("A", u -> {
                throw new IllegalStateException("bad observer");
            });
            feed.subscribe("A", got::add);
            check(feed.publish("A", 1) == 2, "both attempted");
            check(got.size() == 1 && errors.size() == 1, "second still notified, error reported");
        });

        run("thresholdAlertFiresOnCrossingAndRearms", () -> {
            PriceFeed feed = new PriceFeed();
            List<String> alerts = new ArrayList<>();
            feed.subscribe("TSLA", new ThresholdAlert(200, ThresholdAlert.Direction.ABOVE, alerts::add));
            for (long p : new long[] {150, 199, 200, 210, 250, 190, 205}) feed.publish("TSLA", p);
            check(alerts.equals(List.of("TSLA above 200 at 200", "TSLA above 200 at 205")), alerts.toString());
        });

        run("belowAlert", () -> {
            PriceFeed feed = new PriceFeed();
            List<String> alerts = new ArrayList<>();
            feed.subscribe("X", new ThresholdAlert(50, ThresholdAlert.Direction.BELOW, alerts::add));
            for (long p : new long[] {40, 45, 60, 50}) feed.publish("X", p);
            check(alerts.equals(List.of("X below 50 at 40", "X below 50 at 50")), alerts.toString());
        });

        run("percentMoveAlert", () -> {
            PriceFeed feed = new PriceFeed();
            PercentMoveAlert alert = new PercentMoveAlert(5);
            feed.subscribeAll(alert);
            for (long p : new long[] {1000, 1040, 1100, 990}) feed.publish("Y", p);
            check(alert.fired().equals(List.of("Y +6%", "Y -10%")), alert.fired().toString());
        });

        run("randomizedThresholdMatchesBruteForce", () -> {
            Random rnd = new Random(8);
            for (int t = 0; t < 200; t++) {
                PriceFeed feed = new PriceFeed();
                List<String> alerts = new ArrayList<>();
                long threshold = 50;
                feed.subscribe("S", new ThresholdAlert(threshold, ThresholdAlert.Direction.ABOVE, alerts::add));
                int expected = 0;
                boolean wasAbove = false;
                Long last = null;
                for (int i = 0; i < 30; i++) {
                    long p = rnd.nextInt(100);
                    feed.publish("S", p);
                    if (last != null && last == p) continue; // suppressed tick
                    last = p;
                    boolean above = p >= threshold;
                    if (above && !wasAbove) expected++;
                    wasAbove = above;
                }
                check(alerts.size() == expected, "alerts " + alerts.size() + " vs " + expected);
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
