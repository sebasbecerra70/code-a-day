import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
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

    static long[] minors(Money[] ms) {
        return Arrays.stream(ms).mapToLong(Money::minorUnits).toArray();
    }

    public static void main(String[] args) {
        run("parseAndFormat", () -> {
            check(Money.of("12.34", "USD").minorUnits() == 1234, "dollars");
            check(Money.of("12.3", "USD").toString().equals("USD 12.30"), "pads");
            check(Money.of("500", "JPY").toString().equals("JPY 500"), "zero-decimal currency");
            check(Money.of("1.234", "KWD").minorUnits() == 1234, "three-decimal currency");
            check(Money.ofMinor(-5, "USD").toString().equals("USD -0.05"), "negative");
        });

        run("parseRejectsBadInput", () -> {
            expectThrows(IllegalArgumentException.class, () -> Money.of("1.001", "USD"));
            expectThrows(IllegalArgumentException.class, () -> Money.of("0.5", "JPY"));
            expectThrows(NumberFormatException.class, () -> Money.of("abc", "USD"));
            expectThrows(IllegalArgumentException.class, () -> Money.of("1", "XYZ"));
        });

        run("arithmeticIsImmutable", () -> {
            Money a = Money.of("10.00", "USD"), b = Money.of("2.50", "USD");
            Money c = a.plus(b);
            check(c.equals(Money.of("12.50", "USD")) && a.minorUnits() == 1000, "plus returns new");
            check(a.minus(b).minorUnits() == 750 && b.minus(a).isNegative(), "minus");
            check(b.times(3).minorUnits() == 750 && a.negate().minorUnits() == -1000, "times/negate");
        });

        run("currencyMismatch", () -> {
            Money usd = Money.of("1", "USD"), eur = Money.of("1", "EUR");
            expectThrows(IllegalArgumentException.class, () -> usd.plus(eur));
            expectThrows(IllegalArgumentException.class, () -> usd.compareTo(eur));
            check(!usd.equals(eur), "different currencies are not equal");
        });

        run("overflowDetected", () -> {
            Money max = Money.ofMinor(Long.MAX_VALUE, "USD");
            expectThrows(ArithmeticException.class, () -> max.plus(Money.ofMinor(1, "USD")));
            expectThrows(ArithmeticException.class, () -> max.times(2));
            expectThrows(ArithmeticException.class, () -> Money.ofMinor(Long.MIN_VALUE, "USD").negate());
        });

        run("decimalMultiplyRounding", () -> {
            Money price = Money.of("19.99", "USD");
            BigDecimal tax = new BigDecimal("0.0825");
            check(price.times(tax, RoundingMode.HALF_EVEN).minorUnits() == 165, "164.9175 -> 165");
            check(Money.ofMinor(5, "USD").times(new BigDecimal("0.5"), RoundingMode.HALF_EVEN).minorUnits() == 2,
                    "banker's rounding 2.5 -> 2");
            check(Money.ofMinor(5, "USD").times(new BigDecimal("0.5"), RoundingMode.HALF_UP).minorUnits() == 3, "half up");
        });

        run("allocateWithoutLosingCents", () -> {
            check(Arrays.equals(minors(Money.of("1.00", "USD").split(3)), new long[] {34, 33, 33}), "thirds");
            check(Arrays.equals(minors(Money.ofMinor(5, "USD").allocate(3, 7)), new long[] {2, 3}), "30/70 of 5c");
            check(Arrays.equals(minors(Money.ofMinor(-100, "USD").split(3)), new long[] {-34, -33, -33}), "negative");
            check(Arrays.equals(minors(Money.ofMinor(10, "USD").allocate(0, 1, 0)), new long[] {0, 10, 0}), "zero ratio");
            check(Arrays.equals(minors(Money.ofMinor(1, "USD").allocate(0, 1, 1)), new long[] {0, 1, 0}),
                    "leftover skips zero-ratio share");
            expectThrows(IllegalArgumentException.class, () -> Money.ofMinor(1, "USD").allocate(0, 0));
            expectThrows(IllegalArgumentException.class, () -> Money.ofMinor(1, "USD").allocate(-1, 2));
            expectThrows(IllegalArgumentException.class, () -> Money.ofMinor(1, "USD").split(0));
        });

        run("equalsHashCodeCompare", () -> {
            Set<Money> set = new HashSet<>();
            set.add(Money.of("1.50", "USD"));
            set.add(Money.ofMinor(150, "USD"));
            check(set.size() == 1, "value semantics");
            check(Money.of("1", "USD").compareTo(Money.of("2", "USD")) < 0, "ordering");
            check(Money.zero("EUR").isZero(), "zero");
        });

        run("randomizedAllocationInvariants", () -> {
            Random rnd = new Random(118);
            for (int t = 0; t < 2000; t++) {
                long amount = rnd.nextLong() >> rnd.nextInt(64); // wide range of magnitudes, both signs
                long[] ratios = new long[1 + rnd.nextInt(6)];
                for (int i = 0; i < ratios.length; i++) ratios[i] = rnd.nextInt(10);
                if (Arrays.stream(ratios).sum() == 0) ratios[0] = 1;
                Money[] parts = Money.ofMinor(amount, "USD").allocate(ratios);
                long sum = 0;
                for (Money p : parts) sum += p.minorUnits();
                check(sum == amount, "no cent lost: " + amount + " " + Arrays.toString(ratios));
                long ratioSum = Arrays.stream(ratios).sum();
                for (int i = 0; i < ratios.length; i++) {
                    // Each share is within one unit of its exact proportional value.
                    BigDecimal exact = BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(ratios[i]))
                            .divide(BigDecimal.valueOf(ratioSum), 10, RoundingMode.HALF_EVEN);
                    BigDecimal diff = exact.subtract(BigDecimal.valueOf(parts[i].minorUnits())).abs();
                    check(diff.compareTo(BigDecimal.ONE) < 0, "share " + i + " off by " + diff);
                    if (ratios[i] == 0) check(parts[i].isZero(), "zero ratio gets nothing");
                }
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
