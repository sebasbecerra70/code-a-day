import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    static List<Account> makeAccounts(int n, long each) {
        List<Account> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(new Account(i, each));
        return list;
    }

    public static void main(String[] args) {
        run("simpleTransfer", () -> {
            Account a = new Account(1, 1000), b = new Account(2, 500);
            Bank.transfer(a, b, 300);
            check(a.balance() == 700 && b.balance() == 800, "moved");
        });

        run("insufficientFundsLeavesBalancesUnchanged", () -> {
            Account a = new Account(1, 100), b = new Account(2, 0);
            try {
                Bank.transfer(a, b, 101);
                check(false, "should throw");
            } catch (InsufficientFundsException expected) {
            }
            check(a.balance() == 100 && b.balance() == 0, "unchanged");
            check(!a.lock.isLocked() && !b.lock.isLocked(), "locks released after exception");
        });

        run("invalidAmountsAndSelfTransfer", () -> {
            Account a = new Account(1, 100);
            expectThrows(IllegalArgumentException.class, () -> {
                try {
                    Bank.transfer(a, new Account(2, 0), 0);
                } catch (InsufficientFundsException e) {
                    throw new RuntimeException(e);
                }
            });
            expectThrows(IllegalArgumentException.class, () -> new Account(3, -1));
            Bank.transfer(a, a, 50);
            check(a.balance() == 100, "self transfer is a no-op");
        });

        run("oppositeTransfersDoNotDeadlock", () -> {
            Account a = new Account(1, 1_000_000), b = new Account(2, 1_000_000);
            CountDownLatch go = new CountDownLatch(1);
            Thread t1 = new Thread(() -> {
                try {
                    go.await();
                    for (int i = 0; i < 20_000; i++) Bank.transfer(a, b, 1);
                } catch (Exception ignored) {
                }
            });
            Thread t2 = new Thread(() -> {
                try {
                    go.await();
                    for (int i = 0; i < 20_000; i++) Bank.transfer(b, a, 1);
                } catch (Exception ignored) {
                }
            });
            t1.start();
            t2.start();
            go.countDown();
            t1.join(10_000);
            t2.join(10_000);
            check(!t1.isAlive() && !t2.isAlive(), "finished (no deadlock)");
            check(a.balance() == 1_000_000 && b.balance() == 1_000_000, "balanced");
        });

        run("manyThreadsConserveMoney", () -> {
            List<Account> accts = makeAccounts(10, 10_000);
            Bank bank = new Bank(accts);
            AtomicInteger rejected = new AtomicInteger();
            AtomicBoolean badSnapshot = new AtomicBoolean();
            List<Thread> ts = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                int seed = t;
                ts.add(new Thread(() -> {
                    Random rnd = new Random(seed);
                    for (int i = 0; i < 20_000; i++) {
                        Account from = accts.get(rnd.nextInt(10)), to = accts.get(rnd.nextInt(10));
                        try {
                            Bank.transfer(from, to, 1 + rnd.nextInt(500));
                        } catch (InsufficientFundsException e) {
                            rejected.incrementAndGet();
                        }
                    }
                }));
            }
            ts.add(new Thread(() -> {
                for (int i = 0; i < 200; i++) if (bank.totalBalance() != 100_000) badSnapshot.set(true);
            }));
            ts.forEach(Thread::start);
            for (Thread t : ts) t.join(20_000);
            check(bank.totalBalance() == 100_000, "money conserved");
            check(!badSnapshot.get(), "snapshots were consistent mid-flight");
            for (Account a : accts) check(a.balance() >= 0, "no overdraft");
        });

        run("tryTransferWithBackoff", () -> {
            List<Account> accts = makeAccounts(4, 5_000);
            Bank bank = new Bank(accts);
            AtomicInteger timeouts = new AtomicInteger();
            List<Thread> ts = new ArrayList<>();
            for (int t = 0; t < 6; t++) {
                int seed = t;
                ts.add(new Thread(() -> {
                    Random rnd = new Random(seed);
                    for (int i = 0; i < 2_000; i++) {
                        Account from = accts.get(rnd.nextInt(4)), to = accts.get(rnd.nextInt(4));
                        try {
                            if (!Bank.tryTransfer(from, to, 1 + rnd.nextInt(100), 1, TimeUnit.SECONDS)) {
                                timeouts.incrementAndGet();
                            }
                        } catch (InsufficientFundsException ignored) {
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }));
            }
            ts.forEach(Thread::start);
            for (Thread t : ts) t.join(30_000);
            check(bank.totalBalance() == 20_000, "conserved");
            check(timeouts.get() == 0, "no timeouts under light contention");
        });

        run("tryTransferTimesOutWhenLockHeld", () -> {
            Account a = new Account(1, 100), b = new Account(2, 0);
            CountDownLatch held = new CountDownLatch(1), release = new CountDownLatch(1);
            Thread holder = new Thread(() -> {
                b.lock.lock();
                held.countDown();
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                } finally {
                    b.lock.unlock();
                }
            });
            holder.start();
            held.await();
            check(!Bank.tryTransfer(a, b, 10, 30, TimeUnit.MILLISECONDS), "timed out");
            check(a.balance() == 100 && !a.lock.isLocked(), "nothing changed, lock released");
            release.countDown();
            holder.join();
            check(Bank.tryTransfer(a, b, 10, 1, TimeUnit.SECONDS) && b.balance() == 10, "succeeds after release");
        });

        System.out.println("All " + passed + " tests passed");
    }
}
