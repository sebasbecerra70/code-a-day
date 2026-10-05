import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

record Book(String isbn, String title, String author) {}

class LibraryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    LibraryException(String msg) {
        super(msg);
    }
}

/** One physical copy. Its status says who has it or who it's set aside for. */
class BookCopy {
    enum Status { AVAILABLE, ON_LOAN, ON_HOLD_SHELF }

    final String copyId;
    final Book book;
    Status status = Status.AVAILABLE;
    String holder; // borrower (ON_LOAN) or the member it's reserved for (ON_HOLD_SHELF)
    LocalDate due;

    BookCopy(String copyId, Book book) {
        this.copyId = copyId;
        this.book = book;
    }
}

class Member {
    final String id;
    final int loanLimit;
    final List<BookCopy> loans = new ArrayList<>();
    long finesCents;

    Member(String id, int loanLimit) {
        this.id = id;
        this.loanLimit = loanLimit;
    }
}

/** Façade over the catalog, members, loans and reservation queues. */
class Library {
    static final int LOAN_DAYS = 14;
    static final long FINE_PER_DAY_CENTS = 25;
    static final long FINE_CAP_CENTS = 1000;
    static final long MAX_FINES_TO_BORROW_CENTS = 500;

    private final Clock clock;
    private final Map<String, Book> catalog = new LinkedHashMap<>();
    private final Map<String, List<BookCopy>> copiesByIsbn = new HashMap<>();
    private final Map<String, BookCopy> copiesById = new HashMap<>();
    private final Map<String, Member> members = new HashMap<>();
    private final Map<String, Deque<String>> holdQueues = new HashMap<>(); // isbn -> member ids, FIFO

    Library(Clock clock) {
        this.clock = clock;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    void addCopy(Book book, String copyId) {
        if (copiesById.containsKey(copyId)) throw new LibraryException("duplicate copy id " + copyId);
        catalog.putIfAbsent(book.isbn(), book);
        BookCopy c = new BookCopy(copyId, book);
        copiesByIsbn.computeIfAbsent(book.isbn(), k -> new ArrayList<>()).add(c);
        copiesById.put(copyId, c);
    }

    void addMember(String id, int loanLimit) {
        if (members.putIfAbsent(id, new Member(id, loanLimit)) != null) throw new LibraryException("duplicate member " + id);
    }

    /** Case-insensitive substring search over title and author. */
    List<Book> search(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        return catalog.values().stream()
                .filter(b -> b.title().toLowerCase(Locale.ROOT).contains(q) || b.author().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    /**
     * Lends a copy of the book. A copy on the hold shelf is reserved for this
     * member and takes priority; otherwise any available copy works.
     */
    BookCopy checkout(String memberId, String isbn) {
        Member m = member(memberId);
        if (m.loans.size() >= m.loanLimit) throw new LibraryException("loan limit reached");
        if (m.finesCents > MAX_FINES_TO_BORROW_CENTS) throw new LibraryException("unpaid fines");
        List<BookCopy> copies = copiesByIsbn.getOrDefault(isbn, List.of());
        if (copies.isEmpty()) throw new LibraryException("unknown isbn " + isbn);
        BookCopy copy = copies.stream()
                .filter(c -> c.status == BookCopy.Status.ON_HOLD_SHELF && c.holder.equals(memberId))
                .findFirst()
                .or(() -> copies.stream().filter(c -> c.status == BookCopy.Status.AVAILABLE).findFirst())
                .orElseThrow(() -> new LibraryException("no copy available; place a hold"));
        copy.status = BookCopy.Status.ON_LOAN;
        copy.holder = memberId;
        copy.due = today().plusDays(LOAN_DAYS);
        m.loans.add(copy);
        return copy;
    }

    /**
     * Returns a copy, charging a late fee if overdue. If anyone is waiting for
     * this title, the copy goes to the hold shelf for the first person in line.
     * Returns the fee charged.
     */
    long returnCopy(String copyId) {
        BookCopy c = copiesById.get(copyId);
        if (c == null || c.status != BookCopy.Status.ON_LOAN) throw new LibraryException("copy not on loan: " + copyId);
        Member m = member(c.holder);
        m.loans.remove(c);
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(c.due, today()));
        long fee = Math.min(FINE_CAP_CENTS, daysLate * FINE_PER_DAY_CENTS);
        m.finesCents += fee;
        c.due = null;
        Deque<String> queue = holdQueues.get(c.book.isbn());
        if (queue != null && !queue.isEmpty()) {
            c.status = BookCopy.Status.ON_HOLD_SHELF;
            c.holder = queue.poll();
        } else {
            c.status = BookCopy.Status.AVAILABLE;
            c.holder = null;
        }
        return fee;
    }

    /** Joins the FIFO wait list. Only allowed when no copy is free; returns the position in line (1-based). */
    int placeHold(String memberId, String isbn) {
        member(memberId);
        List<BookCopy> copies = copiesByIsbn.get(isbn);
        if (copies == null) throw new LibraryException("unknown isbn " + isbn);
        if (copies.stream().anyMatch(c -> c.status == BookCopy.Status.AVAILABLE)) {
            throw new LibraryException("a copy is available; check it out instead");
        }
        Deque<String> q = holdQueues.computeIfAbsent(isbn, k -> new ArrayDeque<>());
        if (q.contains(memberId)) throw new LibraryException("already on the wait list");
        q.add(memberId);
        return q.size();
    }

    void payFine(String memberId, long cents) {
        Member m = member(memberId);
        if (cents <= 0 || cents > m.finesCents) throw new LibraryException("invalid payment");
        m.finesCents -= cents;
    }

    List<BookCopy> overdue() {
        LocalDate t = today();
        return copiesById.values().stream()
                .filter(c -> c.status == BookCopy.Status.ON_LOAN && c.due.isBefore(t))
                .toList();
    }

    long availableCopies(String isbn) {
        return copiesByIsbn.getOrDefault(isbn, List.of()).stream()
                .filter(c -> c.status == BookCopy.Status.AVAILABLE).count();
    }

    Optional<String> reservedFor(String copyId) {
        BookCopy c = copiesById.get(copyId);
        return c != null && c.status == BookCopy.Status.ON_HOLD_SHELF ? Optional.of(c.holder) : Optional.empty();
    }

    long fines(String memberId) {
        return member(memberId).finesCents;
    }

    int loanCount(String memberId) {
        return member(memberId).loans.size();
    }

    private Member member(String id) {
        Member m = members.get(id);
        if (m == null) throw new LibraryException("unknown member " + id);
        return m;
    }
}
