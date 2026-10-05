import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2025-03-01T10:00:00Z");

        void advanceDays(int d) {
            now = now.plus(Duration.ofDays(d));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    static final Book DUNE = new Book("111", "Dune", "Frank Herbert");
    static final Book SICP = new Book("222", "Structure and Interpretation of Computer Programs", "Abelson & Sussman");

    static Library setup(MutableClock clock) {
        Library lib = new Library(clock);
        lib.addCopy(DUNE, "D1");
        lib.addCopy(DUNE, "D2");
        lib.addCopy(SICP, "S1");
        lib.addMember("ann", 2);
        lib.addMember("bob", 3);
        lib.addMember("cat", 3);
        return lib;
    }

    static void expectLibraryError(Runnable r) {
        expectThrows(LibraryException.class, r);
    }

    public static void main(String[] args) {
        run("searchCaseInsensitive", () -> {
            Library lib = setup(new MutableClock());
            check(lib.search("dune").equals(List.of(DUNE)), "title");
            check(lib.search("SUSSMAN").equals(List.of(SICP)), "author");
            check(lib.search("r").size() == 2 && lib.search("zzz").isEmpty(), "multi/none");
        });

        run("checkoutAndReturnOnTime", () -> {
            MutableClock clock = new MutableClock();
            Library lib = setup(clock);
            BookCopy c = lib.checkout("ann", "111");
            check(lib.availableCopies("111") == 1 && lib.loanCount("ann") == 1, "one out");
            clock.advanceDays(14);
            check(lib.returnCopy(c.copyId) == 0, "due date itself is not late");
            check(lib.availableCopies("111") == 2, "back");
        });

        run("lateFeesAndCap", () -> {
            MutableClock clock = new MutableClock();
            Library lib = setup(clock);
            BookCopy c = lib.checkout("bob", "111");
            clock.advanceDays(17);
            check(lib.overdue().size() == 1, "overdue listed");
            check(lib.returnCopy(c.copyId) == 75, "3 days * 25");
            BookCopy c2 = lib.checkout("bob", "222");
            clock.advanceDays(200);
            check(lib.returnCopy(c2.copyId) == 1000, "capped");
            check(lib.fines("bob") == 1075, "accumulated");
        });

        run("finesBlockBorrowingUntilPaid", () -> {
            MutableClock clock = new MutableClock();
            Library lib = setup(clock);
            BookCopy c = lib.checkout("bob", "111");
            clock.advanceDays(14 + 30);
            lib.returnCopy(c.copyId);
            expectLibraryError(() -> lib.checkout("bob", "111"));
            expectLibraryError(() -> lib.payFine("bob", 10_000));
            lib.payFine("bob", 500);
            check(lib.fines("bob") == 250 && lib.checkout("bob", "111") != null, "can borrow again");
        });

        run("loanLimitAndUnknowns", () -> {
            Library lib = setup(new MutableClock());
            lib.checkout("ann", "111");
            lib.checkout("ann", "222");
            expectLibraryError(() -> lib.checkout("ann", "111"));
            expectLibraryError(() -> lib.checkout("nobody", "111"));
            expectLibraryError(() -> lib.checkout("bob", "999"));
            expectLibraryError(() -> lib.returnCopy("D2")); // not on loan
            expectLibraryError(() -> lib.addMember("ann", 1));
            expectLibraryError(() -> lib.addCopy(DUNE, "D1"));
        });

        run("holdsAreFifoAndReserveTheCopy", () -> {
            Library lib = setup(new MutableClock());
            BookCopy s = lib.checkout("ann", "222");
            expectLibraryError(() -> lib.checkout("bob", "222"));
            check(lib.placeHold("bob", "222") == 1 && lib.placeHold("cat", "222") == 2, "queue positions");
            expectLibraryError(() -> lib.placeHold("bob", "222"));
            lib.returnCopy(s.copyId);
            check(lib.reservedFor("S1").orElseThrow().equals("bob"), "held for bob");
            check(lib.availableCopies("222") == 0, "not generally available");
            expectLibraryError(() -> lib.checkout("cat", "222"));
            lib.checkout("bob", "222");
            lib.returnCopy("S1");
            check(lib.reservedFor("S1").orElseThrow().equals("cat"), "next in line");
            lib.checkout("cat", "222");
            lib.returnCopy("S1");
            check(lib.reservedFor("S1").isEmpty() && lib.availableCopies("222") == 1, "queue drained");
        });

        run("cannotHoldWhenCopyAvailable", () -> {
            Library lib = setup(new MutableClock());
            expectLibraryError(() -> lib.placeHold("ann", "111"));
        });

        run("holdShelfCopyPreferredForHolder", () -> {
            Library lib = setup(new MutableClock());
            BookCopy d1 = lib.checkout("ann", "111");
            BookCopy d2 = lib.checkout("bob", "111");
            lib.placeHold("cat", "111");
            lib.returnCopy(d1.copyId); // goes to cat's hold shelf
            lib.returnCopy(d2.copyId); // queue empty now, so available
            check(lib.availableCopies("111") == 1, "one free copy");
            check(lib.checkout("cat", "111").copyId.equals("D1"), "cat gets the copy held for them");
            check(lib.checkout("ann", "111").copyId.equals("D2"), "ann gets the free one");
        });

        run("randomizedInvariants", () -> {
            Random rnd = new Random(78);
            MutableClock clock = new MutableClock();
            Library lib = new Library(clock);
            String[] isbns = {"a", "b", "c"};
            int copiesPerTitle = 2;
            for (String isbn : isbns) {
                for (int i = 0; i < copiesPerTitle; i++) lib.addCopy(new Book(isbn, "T" + isbn, "A"), isbn + i);
            }
            String[] people = {"m0", "m1", "m2", "m3", "m4"};
            for (String p : people) lib.addMember(p, 2);
            List<String> onLoan = new ArrayList<>();
            for (int op = 0; op < 5000; op++) {
                String who = people[rnd.nextInt(people.length)], isbn = isbns[rnd.nextInt(isbns.length)];
                try {
                    switch (rnd.nextInt(4)) {
                        case 0 -> onLoan.add(lib.checkout(who, isbn).copyId);
                        case 1 -> {
                            if (!onLoan.isEmpty()) lib.returnCopy(onLoan.remove(rnd.nextInt(onLoan.size())));
                        }
                        case 2 -> lib.placeHold(who, isbn);
                        default -> {
                            clock.advanceDays(rnd.nextInt(5));
                            if (lib.fines(who) > 0) lib.payFine(who, lib.fines(who));
                        }
                    }
                } catch (LibraryException expected) {
                    // rule violations are fine; state must stay consistent
                }
                int loans = 0;
                for (String p : people) {
                    loans += lib.loanCount(p);
                    check(lib.loanCount(p) <= 2, "limit respected");
                }
                check(loans == onLoan.size(), "loan bookkeeping");
                for (String isbn2 : isbns) {
                    long held = 0, out = onLoan.stream().filter(id -> id.startsWith(isbn2)).count();
                    for (int i = 0; i < copiesPerTitle; i++) if (lib.reservedFor(isbn2 + i).isPresent()) held++;
                    check(lib.availableCopies(isbn2) + held + out == copiesPerTitle, "copies conserved");
                }
            }
        });

        System.out.println("All " + passed + " tests passed");
    }
}
