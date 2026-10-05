import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** One price tick. Prices are in integer cents. */
record PriceUpdate(String symbol, long oldCents, long newCents) {}

/** The observer interface. */
@FunctionalInterface
interface PriceListener {
    void onPrice(PriceUpdate update);
}

/** Handle returned by subscribe; closing it unsubscribes. Idempotent. */
interface Subscription extends AutoCloseable {
    @Override
    void close();
}

/**
 * The subject. It keeps the last price per symbol and notifies the listeners
 * subscribed to that symbol (or to all symbols) whenever the price changes.
 */
class PriceFeed {
    private static final String ALL = "*";

    // Copy-on-write lists: iteration is over a snapshot, so listeners may
    // subscribe or unsubscribe while being notified without a CME.
    private final Map<String, CopyOnWriteArrayList<PriceListener>> listeners = new HashMap<>();
    private final Map<String, Long> lastPrice = new HashMap<>();
    private final Consumer<Throwable> errorHandler;

    PriceFeed() {
        this(t -> {});
    }

    PriceFeed(Consumer<Throwable> errorHandler) {
        this.errorHandler = errorHandler;
    }

    Subscription subscribe(String symbol, PriceListener l) {
        CopyOnWriteArrayList<PriceListener> list =
                listeners.computeIfAbsent(symbol, k -> new CopyOnWriteArrayList<>());
        list.add(l);
        return () -> list.remove(l);
    }

    Subscription subscribeAll(PriceListener l) {
        return subscribe(ALL, l);
    }

    /** Publishes a price. Unchanged prices are not broadcast. Returns the number of listeners notified. */
    int publish(String symbol, long cents) {
        Long old = lastPrice.put(symbol, cents);
        if (old != null && old == cents) return 0;
        PriceUpdate u = new PriceUpdate(symbol, old == null ? cents : old, cents);
        int notified = 0;
        for (String key : List.of(symbol, ALL)) {
            List<PriceListener> list = listeners.get(key);
            if (list == null) continue;
            for (PriceListener l : list) {
                // One failing observer must not stop the others from hearing about the tick.
                try {
                    l.onPrice(u);
                } catch (RuntimeException e) {
                    errorHandler.accept(e);
                }
                notified++;
            }
        }
        return notified;
    }

    int listenerCount(String symbol) {
        List<PriceListener> l = listeners.get(symbol);
        return l == null ? 0 : l.size();
    }
}

/**
 * A concrete observer: fires when the price *crosses* a threshold, not on every
 * tick while it stays past it. It re-arms once the price crosses back.
 */
class ThresholdAlert implements PriceListener {
    enum Direction { ABOVE, BELOW }

    private final long thresholdCents;
    private final Direction direction;
    private final Consumer<String> notifier;
    private boolean triggered;

    ThresholdAlert(long thresholdCents, Direction direction, Consumer<String> notifier) {
        this.thresholdCents = thresholdCents;
        this.direction = direction;
        this.notifier = notifier;
    }

    @Override
    public void onPrice(PriceUpdate u) {
        boolean past = direction == Direction.ABOVE ? u.newCents() >= thresholdCents : u.newCents() <= thresholdCents;
        if (past && !triggered) {
            triggered = true;
            notifier.accept(u.symbol() + " " + direction.name().toLowerCase() + " " + thresholdCents + " at " + u.newCents());
        } else if (!past) {
            triggered = false; // re-arm
        }
    }
}

/** Fires when a single tick moves by at least the given percentage. */
class PercentMoveAlert implements PriceListener {
    private final double percent;
    private final List<String> fired = new ArrayList<>();

    PercentMoveAlert(double percent) {
        this.percent = percent;
    }

    @Override
    public void onPrice(PriceUpdate u) {
        if (u.oldCents() == 0) return;
        double change = 100.0 * (u.newCents() - u.oldCents()) / u.oldCents();
        if (Math.abs(change) >= percent) fired.add(u.symbol() + (change > 0 ? " +" : " ") + Math.round(change) + "%");
    }

    List<String> fired() {
        return fired;
    }
}
