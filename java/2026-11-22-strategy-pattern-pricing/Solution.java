import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A cart line. Money is in integer cents to avoid floating-point rounding. */
record LineItem(String sku, long unitCents, int quantity) {
    LineItem {
        Objects.requireNonNull(sku);
        if (unitCents < 0 || quantity < 0) throw new IllegalArgumentException("negative price or quantity");
    }

    long subtotalCents() {
        return Math.multiplyExact(unitCents, quantity);
    }
}

/**
 * The strategy: given the cart, how much discount applies? Implementations
 * are interchangeable at runtime, and the checkout code never branches on which
 * promotion is active.
 */
@FunctionalInterface
interface PricingStrategy {
    long discountCents(List<LineItem> items);

    static long subtotal(List<LineItem> items) {
        long sum = 0;
        for (LineItem i : items) sum = Math.addExact(sum, i.subtotalCents());
        return sum;
    }

    PricingStrategy NONE = items -> 0;

    /** Percentage off the whole cart, rounded half-up to the cent. */
    static PricingStrategy percentOff(int percent) {
        if (percent < 0 || percent > 100) throw new IllegalArgumentException("percent must be 0..100");
        return items -> (subtotal(items) * percent + 50) / 100;
    }

    /** "Buy X get Y free" for one SKU: every group of X+Y units has Y free. */
    static PricingStrategy buyXGetYFree(String sku, int x, int y) {
        if (x <= 0 || y <= 0) throw new IllegalArgumentException("x and y must be positive");
        return items -> {
            long discount = 0;
            for (LineItem i : items) {
                if (i.sku().equals(sku)) discount += (long) (i.quantity() / (x + y)) * y * i.unitCents();
            }
            return discount;
        };
    }

    /** Flat amount off once the subtotal reaches a threshold, never more than the subtotal. */
    static PricingStrategy amountOffOver(long thresholdCents, long offCents) {
        return items -> {
            long sub = subtotal(items);
            return sub >= thresholdCents ? Math.min(offCents, sub) : 0;
        };
    }

    /** Composite: apply whichever strategy saves the customer the most (promotions don't stack). */
    static PricingStrategy bestOf(PricingStrategy... strategies) {
        List<PricingStrategy> list = List.of(strategies);
        return items -> {
            long best = 0;
            for (PricingStrategy s : list) best = Math.max(best, s.discountCents(items));
            return best;
        };
    }

    /** Composite: stack all strategies, capping the total at the subtotal. */
    static PricingStrategy stacked(PricingStrategy... strategies) {
        List<PricingStrategy> list = List.of(strategies);
        return items -> {
            long total = 0;
            for (PricingStrategy s : list) total += s.discountCents(items);
            return Math.min(total, subtotal(items));
        };
    }
}

enum CustomerTier { GUEST, MEMBER, VIP }

/** The context: holds a strategy per customer tier and computes totals. */
class Checkout {
    private final Map<CustomerTier, PricingStrategy> strategies = new EnumMap<>(CustomerTier.class);

    Checkout() {
        for (CustomerTier t : CustomerTier.values()) strategies.put(t, PricingStrategy.NONE);
    }

    /** Swapping a strategy at runtime needs no change to Checkout itself. */
    Checkout setStrategy(CustomerTier tier, PricingStrategy s) {
        strategies.put(tier, Objects.requireNonNull(s));
        return this;
    }

    long totalCents(CustomerTier tier, List<LineItem> items) {
        long sub = PricingStrategy.subtotal(items);
        long discount = strategies.get(tier).discountCents(items);
        if (discount < 0 || discount > sub) throw new IllegalStateException("invalid discount " + discount);
        return sub - discount;
    }
}
