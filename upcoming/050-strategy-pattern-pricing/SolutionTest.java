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

    static final List<LineItem> CART = List.of(
            new LineItem("apple", 50, 7),   // 350
            new LineItem("bread", 300, 1),  // 300
            new LineItem("cheese", 1250, 2) // 2500
    ); // subtotal 3150

    public static void main(String[] args) {
        run("noDiscountAndEmptyCart", () -> {
            Checkout c = new Checkout();
            check(c.totalCents(CustomerTier.GUEST, CART) == 3150, "subtotal");
            check(c.totalCents(CustomerTier.VIP, List.of()) == 0, "empty cart");
        });

        run("lineItemValidation", () -> {
            expectThrows(IllegalArgumentException.class, () -> new LineItem("x", -1, 1));
            expectThrows(IllegalArgumentException.class, () -> new LineItem("x", 1, -1));
            expectThrows(ArithmeticException.class, () -> new LineItem("x", Long.MAX_VALUE, 2).subtotalCents());
        });

        run("percentOffRounding", () -> {
            check(PricingStrategy.percentOff(10).discountCents(CART) == 315, "10%");
            check(PricingStrategy.percentOff(15).discountCents(List.of(new LineItem("a", 333, 1))) == 50, "49.95 -> 50");
            check(PricingStrategy.percentOff(100).discountCents(CART) == 3150, "100%");
            expectThrows(IllegalArgumentException.class, () -> PricingStrategy.percentOff(101));
        });

        run("buyXGetYFree", () -> {
            PricingStrategy b2g1 = PricingStrategy.buyXGetYFree("apple", 2, 1);
            check(b2g1.discountCents(CART) == 100, "7 apples -> 2 free"); // groups of 3: 2 groups
            check(b2g1.discountCents(List.of(new LineItem("apple", 50, 2))) == 0, "not enough");
            check(b2g1.discountCents(List.of(new LineItem("pear", 50, 9))) == 0, "other sku");
        });

        run("amountOffOverThreshold", () -> {
            PricingStrategy s = PricingStrategy.amountOffOver(3000, 500);
            check(s.discountCents(CART) == 500, "over threshold");
            check(s.discountCents(List.of(new LineItem("a", 2999, 1))) == 0, "just under");
            check(PricingStrategy.amountOffOver(0, 500).discountCents(List.of(new LineItem("a", 100, 1))) == 100,
                    "capped at subtotal");
        });

        run("bestOfPicksLargest", () -> {
            PricingStrategy best = PricingStrategy.bestOf(
                    PricingStrategy.percentOff(10), PricingStrategy.amountOffOver(3000, 500),
                    PricingStrategy.buyXGetYFree("apple", 2, 1));
            check(best.discountCents(CART) == 500, "flat 500 wins");
            check(PricingStrategy.bestOf().discountCents(CART) == 0, "empty composite");
        });

        run("stackedIsCapped", () -> {
            PricingStrategy s = PricingStrategy.stacked(PricingStrategy.percentOff(10),
                    PricingStrategy.buyXGetYFree("apple", 2, 1));
            check(s.discountCents(CART) == 415, "315 + 100");
            PricingStrategy huge = PricingStrategy.stacked(PricingStrategy.percentOff(80), PricingStrategy.percentOff(80));
            check(huge.discountCents(CART) == 3150, "capped");
        });

        run("swapStrategyPerTierAtRuntime", () -> {
            Checkout c = new Checkout()
                    .setStrategy(CustomerTier.MEMBER, PricingStrategy.percentOff(10))
                    .setStrategy(CustomerTier.VIP, PricingStrategy.percentOff(20));
            check(c.totalCents(CustomerTier.GUEST, CART) == 3150, "guest");
            check(c.totalCents(CustomerTier.MEMBER, CART) == 2835, "member");
            check(c.totalCents(CustomerTier.VIP, CART) == 2520, "vip");
            c.setStrategy(CustomerTier.GUEST, items -> 1); // a lambda is a strategy too
            check(c.totalCents(CustomerTier.GUEST, CART) == 3149, "swapped");
        });

        run("checkoutRejectsBadStrategy", () -> {
            Checkout c = new Checkout().setStrategy(CustomerTier.GUEST, items -> -5);
            expectThrows(IllegalStateException.class, () -> c.totalCents(CustomerTier.GUEST, CART));
            expectThrows(NullPointerException.class, () -> c.setStrategy(CustomerTier.VIP, null));
        });

        run("randomizedInvariants", () -> {
            Random rnd = new Random(5);
            for (int t = 0; t < 500; t++) {
                List<LineItem> cart = new ArrayList<>();
                for (int i = rnd.nextInt(5); i > 0; i--) {
                    cart.add(new LineItem(rnd.nextBoolean() ? "apple" : "kiwi", rnd.nextInt(1000), rnd.nextInt(10)));
                }
                PricingStrategy[] parts = {
                    PricingStrategy.percentOff(rnd.nextInt(101)),
                    PricingStrategy.buyXGetYFree("apple", 1 + rnd.nextInt(3), 1 + rnd.nextInt(2)),
                    PricingStrategy.amountOffOver(rnd.nextInt(3000), rnd.nextInt(800))
                };
                long sub = PricingStrategy.subtotal(cart);
                long best = PricingStrategy.bestOf(parts).discountCents(cart);
                long stacked = PricingStrategy.stacked(parts).discountCents(cart);
                for (PricingStrategy p : parts) {
                    long d = p.discountCents(cart);
                    check(d >= 0 && d <= sub && d <= best, "each within bounds");
                }
                check(best <= stacked && stacked <= sub, "best <= stacked <= subtotal");
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
