import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable money: an exact amount in the currency's minor units (cents, yen,
 * fils) plus the currency. Every operation returns a new instance, and mixing
 * currencies is an error rather than a silent bug.
 */
final class Money implements Comparable<Money> {
    private final long minor;
    private final Currency currency;

    private Money(long minor, Currency currency) {
        this.minor = minor;
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    static Money ofMinor(long minor, String currencyCode) {
        return new Money(minor, Currency.getInstance(currencyCode));
    }

    /** Parses a decimal amount like "12.34". Rejects more precision than the currency allows. */
    static Money of(String amount, String currencyCode) {
        Currency c = Currency.getInstance(currencyCode);
        BigDecimal bd = new BigDecimal(amount);
        try {
            // movePointRight is exact; longValueExact throws if there's a fractional part left or overflow.
            return new Money(bd.movePointRight(c.getDefaultFractionDigits()).longValueExact(), c);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("invalid amount for " + currencyCode + ": " + amount, e);
        }
    }

    static Money zero(String currencyCode) {
        return ofMinor(0, currencyCode);
    }

    long minorUnits() { return minor; }
    Currency currency() { return currency; }
    boolean isZero() { return minor == 0; }
    boolean isNegative() { return minor < 0; }

    Money plus(Money o) {
        requireSameCurrency(o);
        return new Money(Math.addExact(minor, o.minor), currency);
    }

    Money minus(Money o) {
        requireSameCurrency(o);
        return new Money(Math.subtractExact(minor, o.minor), currency);
    }

    Money negate() {
        return new Money(Math.negateExact(minor), currency);
    }

    Money times(long factor) {
        return new Money(Math.multiplyExact(minor, factor), currency);
    }

    /** Multiplies by a decimal factor (tax rate, FX rate) with an explicit rounding mode. */
    Money times(BigDecimal factor, RoundingMode mode) {
        BigDecimal r = BigDecimal.valueOf(minor).multiply(factor).setScale(0, mode);
        return new Money(r.longValueExact(), currency);
    }

    /**
     * Splits the amount by ratios with no cent lost or invented (largest
     * remainder method): each share gets floor(total * ratio / sum), then the
     * leftover units go to the shares with the largest fractional parts, ties
     * to the earliest. allocate(1,1,1) of $1.00 gives 34, 33, 33, and every
     * share is within one unit of its exact value. Negative amounts are split
     * symmetrically.
     */
    Money[] allocate(long... ratios) {
        if (ratios.length == 0) throw new IllegalArgumentException("no ratios");
        long sum = 0;
        for (long r : ratios) {
            if (r < 0) throw new IllegalArgumentException("negative ratio");
            sum = Math.addExact(sum, r);
        }
        if (sum == 0) throw new IllegalArgumentException("ratios sum to zero");
        long sign = minor < 0 ? -1 : 1;
        // BigDecimal avoids overflow in total * ratio (and Math.abs(Long.MIN_VALUE)).
        BigDecimal total = BigDecimal.valueOf(minor).abs(), divisor = BigDecimal.valueOf(sum);
        long[] parts = new long[ratios.length];
        BigDecimal[] fractions = new BigDecimal[ratios.length]; // numerator of the dropped fraction
        BigDecimal remainder = total;
        for (int i = 0; i < ratios.length; i++) {
            BigDecimal[] qr = total.multiply(BigDecimal.valueOf(ratios[i])).divideAndRemainder(divisor);
            parts[i] = qr[0].longValueExact();
            fractions[i] = qr[1];
            remainder = remainder.subtract(qr[0]);
        }
        // The leftover is less than the number of shares with a non-zero fraction,
        // so zero-ratio shares (fraction 0) never receive a unit.
        Integer[] order = new Integer[ratios.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> fractions[b].compareTo(fractions[a])); // stable: ties keep index order
        for (int k = 0; k < remainder.intValueExact(); k++) parts[order[k]]++;
        Money[] out = new Money[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = new Money(sign * parts[i], currency);
        return out;
    }

    /** Equal split into n parts. */
    Money[] split(int n) {
        if (n <= 0) throw new IllegalArgumentException("n must be positive");
        long[] ones = new long[n];
        Arrays.fill(ones, 1);
        return allocate(ones);
    }

    BigDecimal toDecimal() {
        return BigDecimal.valueOf(minor, currency.getDefaultFractionDigits());
    }

    private void requireSameCurrency(Money o) {
        if (!currency.equals(o.currency)) {
            throw new IllegalArgumentException("currency mismatch: " + currency + " vs " + o.currency);
        }
    }

    @Override
    public int compareTo(Money o) {
        requireSameCurrency(o);
        return Long.compare(minor, o.minor);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money m && minor == m.minor && currency.equals(m.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minor, currency);
    }

    /** e.g. "USD 12.34", "JPY 500", "USD -0.05". */
    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + toDecimal().toPlainString();
    }
}
