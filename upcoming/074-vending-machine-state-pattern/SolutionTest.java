import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    static final Product COLA = new Product("Cola", 125), CHIPS = new Product("Chips", 65);

    static VendingMachine stocked() {
        VendingMachine m = new VendingMachine();
        m.enterMaintenance();
        m.stockSlot("A1", COLA, 2);
        m.stockSlot("B1", CHIPS, 1);
        m.loadCoins(Coin.QUARTER, 4);
        m.loadCoins(Coin.DIME, 4);
        m.loadCoins(Coin.NICKEL, 2);
        m.exitMaintenance();
        return m;
    }

    static int sum(List<Coin> coins) {
        return coins.stream().mapToInt(c -> c.cents).sum();
    }

    // Exhaustive reference: minimum number of coins, or -1.
    static int bruteMinCoins(int amount, int[] counts, int idx) {
        if (amount == 0) return 0;
        if (idx == counts.length) return -1;
        int best = -1, value = Coin.values()[idx].cents;
        for (int k = 0; k <= counts[idx] && k * value <= amount; k++) {
            int rest = bruteMinCoins(amount - k * value, counts, idx + 1);
            if (rest >= 0 && (best < 0 || rest + k < best)) best = rest + k;
        }
        return best;
    }

    public static void main(String[] args) {
        run("newMachineIsSoldOut", () -> {
            VendingMachine m = new VendingMachine();
            check(m.stateName().equals("SoldOut"), m.stateName());
            expectThrows(IllegalStateException.class, () -> m.insertCoin(Coin.DOLLAR));
            expectThrows(IllegalStateException.class, () -> m.stockSlot("A1", COLA, 1));
        });

        run("happyPathWithChange", () -> {
            VendingMachine m = stocked();
            check(m.stateName().equals("Idle"), "idle");
            m.insertCoin(Coin.DOLLAR);
            check(m.stateName().equals("HasMoney"), "has money");
            m.insertCoin(Coin.DOLLAR);
            Purchase p = m.select("A1");
            check(p.product().equals(COLA) && sum(p.change()) == 75, "change 75");
            check(p.change().size() == 3, "3 quarters is the fewest coins");
            check(m.stateName().equals("Idle") && m.stock("A1") == 1, "back to idle");
        });

        run("selectWithoutMoneyOrBadSlot", () -> {
            VendingMachine m = stocked();
            expectThrows(IllegalStateException.class, () -> m.select("A1"));
            m.insertCoin(Coin.QUARTER);
            expectThrows(IllegalArgumentException.class, () -> m.select("Z9"));
            expectThrows(IllegalStateException.class, () -> m.select("A1")); // not enough
            check(m.stateName().equals("HasMoney") && m.balance() == 25, "transaction still open");
        });

        run("cancelRefundsExactCoins", () -> {
            VendingMachine m = stocked();
            check(m.cancel().isEmpty(), "nothing to refund when idle");
            m.insertCoin(Coin.DIME);
            m.insertCoin(Coin.DOLLAR);
            check(m.cancel().equals(List.of(Coin.DIME, Coin.DOLLAR)), "refund");
            check(m.stateName().equals("Idle") && m.coinCount(Coin.DOLLAR) == 0, "coins removed");
        });

        run("cannotMakeChangeKeepsTransactionOpen", () -> {
            VendingMachine m = new VendingMachine();
            m.enterMaintenance();
            m.stockSlot("B1", CHIPS, 1); // no coins loaded at all
            m.exitMaintenance();
            m.insertCoin(Coin.DOLLAR);
            expectThrows(IllegalStateException.class, () -> m.select("B1"));
            check(m.stock("B1") == 1 && m.stateName().equals("HasMoney"), "nothing dispensed");
            check(m.cancel().equals(List.of(Coin.DOLLAR)), "refund works");
        });

        run("lastItemMovesToSoldOut", () -> {
            VendingMachine m = new VendingMachine();
            m.enterMaintenance();
            m.stockSlot("B1", CHIPS, 1);
            m.exitMaintenance();
            for (Coin c : List.of(Coin.QUARTER, Coin.QUARTER, Coin.DIME, Coin.NICKEL)) m.insertCoin(c);
            Purchase p = m.select("B1");
            check(p.change().isEmpty(), "exact amount");
            check(m.stateName().equals("SoldOut"), m.stateName());
            expectThrows(IllegalStateException.class, () -> m.insertCoin(Coin.DIME));
        });

        run("maintenanceRules", () -> {
            VendingMachine m = stocked();
            m.insertCoin(Coin.DIME);
            expectThrows(IllegalStateException.class, m::enterMaintenance);
            m.cancel();
            m.enterMaintenance();
            expectThrows(IllegalStateException.class, () -> m.insertCoin(Coin.DIME));
            m.exitMaintenance();
            expectThrows(IllegalStateException.class, m::exitMaintenance);
        });

        run("changeWhereGreedyFails", () -> {
            Map<Coin, Integer> avail = new EnumMap<>(Coin.class);
            avail.put(Coin.QUARTER, 1);
            avail.put(Coin.DIME, 3);
            Optional<List<Coin>> change = VendingMachine.makeChange(30, avail);
            check(change.isPresent() && change.get().equals(List.of(Coin.DIME, Coin.DIME, Coin.DIME)),
                    String.valueOf(change));
            check(VendingMachine.makeChange(15, avail).isEmpty(), "impossible");
            check(VendingMachine.makeChange(0, avail).get().isEmpty(), "zero");
        });

        run("randomizedChangeMatchesBruteForce", () -> {
            Random rnd = new Random(74);
            for (int t = 0; t < 400; t++) {
                Map<Coin, Integer> avail = new EnumMap<>(Coin.class);
                int[] counts = new int[4];
                for (Coin c : Coin.values()) {
                    counts[c.ordinal()] = rnd.nextInt(4);
                    avail.put(c, counts[c.ordinal()]);
                }
                int amount = 5 * rnd.nextInt(60);
                Optional<List<Coin>> got = VendingMachine.makeChange(amount, avail);
                int expected = bruteMinCoins(amount, counts, 0);
                if (expected < 0) {
                    check(got.isEmpty(), "should be impossible: " + amount);
                } else {
                    check(got.isPresent() && got.get().size() == expected && sum(got.get()) == amount,
                            amount + " -> " + got);
                    for (Coin c : Coin.values()) {
                        check(got.get().stream().filter(x -> x == c).count() <= counts[c.ordinal()], "supply");
                    }
                }
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
