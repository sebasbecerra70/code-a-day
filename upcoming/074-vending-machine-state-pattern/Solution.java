import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

enum Coin {
    NICKEL(5), DIME(10), QUARTER(25), DOLLAR(100);

    final int cents;

    Coin(int cents) {
        this.cents = cents;
    }
}

record Product(String name, int priceCents) {}

record Purchase(Product product, List<Coin> change) {}

/** Each state handles every event; the machine just delegates to the current one. */
interface VendingState {
    default void insertCoin(VendingMachine m, Coin c) {
        throw new IllegalStateException(name() + ": cannot accept coins");
    }

    default Purchase select(VendingMachine m, String slot) {
        throw new IllegalStateException(name() + ": cannot select");
    }

    /** Refunds whatever was inserted. Safe in every state. */
    default List<Coin> cancel(VendingMachine m) {
        return List.of();
    }

    default void enterMaintenance(VendingMachine m) {
        m.setState(new MaintenanceState());
    }

    String name();
}

class IdleState implements VendingState {
    @Override
    public void insertCoin(VendingMachine m, Coin c) {
        m.acceptCoin(c);
        m.setState(new HasMoneyState());
    }

    @Override
    public Purchase select(VendingMachine m, String slot) {
        throw new IllegalStateException("insert money first");
    }

    @Override
    public String name() {
        return "Idle";
    }
}

class HasMoneyState implements VendingState {
    @Override
    public void insertCoin(VendingMachine m, Coin c) {
        m.acceptCoin(c);
    }

    @Override
    public Purchase select(VendingMachine m, String slot) {
        Product p = m.product(slot).orElseThrow(() -> new IllegalArgumentException("no such slot " + slot));
        if (m.stock(slot) == 0) throw new IllegalStateException(p.name() + " is sold out");
        int due = m.balance() - p.priceCents();
        if (due < 0) throw new IllegalStateException("insert " + (-due) + " more cents");
        // Check change *before* dispensing; if we can't make it, the customer keeps their options.
        List<Coin> change = m.makeChange(due)
                .orElseThrow(() -> new IllegalStateException("cannot make change; use exact amount"));
        m.dispense(slot, change);
        m.setState(m.anyStock() ? new IdleState() : new SoldOutState());
        return new Purchase(p, change);
    }

    @Override
    public List<Coin> cancel(VendingMachine m) {
        List<Coin> refund = m.refundInserted();
        m.setState(new IdleState());
        return refund;
    }

    @Override
    public void enterMaintenance(VendingMachine m) {
        throw new IllegalStateException("transaction in progress");
    }

    @Override
    public String name() {
        return "HasMoney";
    }
}

class SoldOutState implements VendingState {
    @Override
    public String name() {
        return "SoldOut";
    }
}

class MaintenanceState implements VendingState {
    @Override
    public void enterMaintenance(VendingMachine m) {
        // already there
    }

    @Override
    public String name() {
        return "Maintenance";
    }
}

/** The context. It owns inventory and money; states decide what each event means. */
class VendingMachine {
    private VendingState state = new SoldOutState();
    private final Map<String, Product> products = new HashMap<>();
    private final Map<String, Integer> stock = new HashMap<>();
    private final Map<Coin, Integer> coins = new EnumMap<>(Coin.class);
    private final List<Coin> inserted = new ArrayList<>();

    VendingMachine() {
        for (Coin c : Coin.values()) coins.put(c, 0);
    }

    // ----- customer events, delegated to the state -----
    void insertCoin(Coin c) { state.insertCoin(this, c); }
    Purchase select(String slot) { return state.select(this, slot); }
    List<Coin> cancel() { return state.cancel(this); }

    // ----- operator events -----
    void enterMaintenance() { state.enterMaintenance(this); }

    void exitMaintenance() {
        if (!(state instanceof MaintenanceState)) throw new IllegalStateException("not in maintenance");
        state = anyStock() ? new IdleState() : new SoldOutState();
    }

    void stockSlot(String slot, Product p, int count) {
        requireMaintenance();
        if (count < 0) throw new IllegalArgumentException("negative count");
        products.put(slot, p);
        stock.merge(slot, count, Integer::sum);
    }

    void loadCoins(Coin c, int count) {
        requireMaintenance();
        coins.merge(c, count, Integer::sum);
    }

    private void requireMaintenance() {
        if (!(state instanceof MaintenanceState)) throw new IllegalStateException("enter maintenance first");
    }

    // ----- helpers used by states -----
    void setState(VendingState s) { state = s; }
    String stateName() { return state.name(); }
    Optional<Product> product(String slot) { return Optional.ofNullable(products.get(slot)); }
    int stock(String slot) { return stock.getOrDefault(slot, 0); }
    boolean anyStock() { return stock.values().stream().anyMatch(n -> n > 0); }
    int coinCount(Coin c) { return coins.get(c); }

    int balance() {
        return inserted.stream().mapToInt(c -> c.cents).sum();
    }

    void acceptCoin(Coin c) {
        inserted.add(c);
        coins.merge(c, 1, Integer::sum); // inserted coins can be used for change
    }

    List<Coin> refundInserted() {
        List<Coin> refund = new ArrayList<>(inserted);
        for (Coin c : refund) coins.merge(c, -1, Integer::sum);
        inserted.clear();
        return refund;
    }

    void dispense(String slot, List<Coin> change) {
        stock.merge(slot, -1, Integer::sum);
        for (Coin c : change) coins.merge(c, -1, Integer::sum);
        inserted.clear();
    }

    /**
     * Fewest coins summing to {@code amount} using only coins in the machine.
     * Greedy fails with limited supply (30 = 3 dimes when there are no nickels,
     * not 25 + ?), so this is a bounded-knapsack DP over amounts.
     */
    Optional<List<Coin>> makeChange(int amount) {
        return makeChange(amount, coins);
    }

    static Optional<List<Coin>> makeChange(int amount, Map<Coin, Integer> available) {
        final int INF = Integer.MAX_VALUE;
        int[] best = new int[amount + 1];
        Arrays.fill(best, INF);
        best[0] = 0;
        // choice[k][a] records that coin k improved amount a, so the answer can be rebuilt backwards.
        List<Coin> items = new ArrayList<>();
        for (Coin c : Coin.values()) for (int i = 0; i < available.getOrDefault(c, 0); i++) items.add(c);
        Coin[][] choice = new Coin[items.size()][];
        for (int k = 0; k < items.size(); k++) {
            Coin c = items.get(k);
            choice[k] = new Coin[amount + 1];
            for (int a = amount; a >= c.cents; a--) { // descending: each physical coin used at most once
                if (best[a - c.cents] != INF && best[a - c.cents] + 1 < best[a]) {
                    best[a] = best[a - c.cents] + 1;
                    choice[k][a] = c;
                }
            }
        }
        if (best[amount] == INF) return Optional.empty();
        List<Coin> out = new ArrayList<>();
        int a = amount;
        for (int k = items.size() - 1; k >= 0 && a > 0; k--) {
            if (choice[k][a] != null) {
                out.add(choice[k][a]);
                a -= choice[k][a].cents;
            }
        }
        return Optional.of(out);
    }
}
