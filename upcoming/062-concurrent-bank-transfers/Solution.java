import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

class InsufficientFundsException extends Exception {
    private static final long serialVersionUID = 1L;

    InsufficientFundsException(String msg) {
        super(msg);
    }
}

/** An account with its own lock, so transfers between unrelated accounts run in parallel. */
class Account {
    final int id;
    private long balanceCents;
    final ReentrantLock lock = new ReentrantLock();

    Account(int id, long balanceCents) {
        if (balanceCents < 0) throw new IllegalArgumentException("negative opening balance");
        this.id = id;
        this.balanceCents = balanceCents;
    }

    long balance() {
        lock.lock();
        try {
            return balanceCents;
        } finally {
            lock.unlock();
        }
    }

    // Callers hold the lock.
    void debit(long cents) throws InsufficientFundsException {
        if (cents > balanceCents) {
            throw new InsufficientFundsException("account " + id + " has " + balanceCents + ", needs " + cents);
        }
        balanceCents -= cents;
    }

    void credit(long cents) {
        balanceCents = Math.addExact(balanceCents, cents);
    }
}

class Bank {
    private final List<Account> accounts;

    Bank(List<Account> accounts) {
        this.accounts = accounts;
    }

    /**
     * Moves money atomically. Both locks are always acquired in ascending id
     * order, so two opposite transfers (A->B and B->A) can never each hold one
     * lock while waiting for the other. That rules out the circular wait.
     */
    static void transfer(Account from, Account to, long cents) throws InsufficientFundsException {
        if (cents <= 0) throw new IllegalArgumentException("amount must be positive");
        if (from == to) return; // also avoids locking the same lock "twice" in a different order
        Account first = from.id < to.id ? from : to;
        Account second = first == from ? to : from;
        first.lock.lock();
        try {
            second.lock.lock();
            try {
                from.debit(cents); // throws before any mutation if funds are short
                to.credit(cents);
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }

    /**
     * Alternative that needs no global ordering: tryLock both, and if the second
     * is unavailable, release everything, back off for a random time, and retry.
     * Returns false if it didn't succeed before the deadline.
     */
    static boolean tryTransfer(Account from, Account to, long cents, long timeout, TimeUnit unit)
            throws InsufficientFundsException, InterruptedException {
        if (cents <= 0) throw new IllegalArgumentException("amount must be positive");
        if (from == to) return true;
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            if (from.lock.tryLock()) {
                try {
                    if (to.lock.tryLock()) {
                        try {
                            from.debit(cents);
                            to.credit(cents);
                            return true;
                        } finally {
                            to.lock.unlock();
                        }
                    }
                } finally {
                    from.lock.unlock();
                }
            }
            // Random backoff breaks the symmetry that would otherwise cause livelock.
            TimeUnit.MICROSECONDS.sleep(ThreadLocalRandom.current().nextInt(50, 500));
        }
        return false;
    }

    /**
     * Consistent snapshot of the total: lock every account (in id order), sum, unlock.
     * Reading balances one by one without locks could double-count money in flight.
     */
    long totalBalance() {
        List<Account> sorted = accounts.stream().sorted((a, b) -> Integer.compare(a.id, b.id)).toList();
        for (Account a : sorted) a.lock.lock();
        try {
            long sum = 0;
            for (Account a : sorted) sum += a.balance();
            return sum;
        } finally {
            for (int i = sorted.size() - 1; i >= 0; i--) sorted.get(i).lock.unlock();
        }
    }
}
