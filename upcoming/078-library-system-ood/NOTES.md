# Library management system (object-oriented design)

**Problem:** Design a library: a catalog of titles with multiple physical copies, members with loan limits, 14-day loans with capped late fees, FIFO holds (a returned copy is set aside for the next person waiting) and search.

## Approach
- **Separate the title from the copy:** `Book` (ISBN, title, author) is an immutable record, while `BookCopy` is a physical item with a status (`AVAILABLE`, `ON_LOAN`, `ON_HOLD_SHELF`), a holder and a due date. Holds are placed on a title, but loans are of a specific copy.
- `Member` tracks current loans, the loan limit and outstanding fines.
- `Library` is the façade. It keeps maps by ISBN and copy id for O(1) lookup, plus a FIFO `ArrayDeque` of member ids per ISBN for holds.
- **Checkout:** check the limit and fines, then prefer a copy on the hold shelf *for this member*, otherwise any available copy.
- **Return:** compute the late fee (days late × 25¢, capped at $10). If someone is waiting, move the copy to the hold shelf for the head of the queue; otherwise make it available.
- Time comes from an injected `Clock`, so due-date tests are deterministic.

## Complexity
| Operation | Time | Notes |
|-----------|------|-------|
| checkout / return | O(copies of that title) | fine for a typical library |
| placeHold | O(copies + queue length) | the duplicate-hold check scans the queue |
| search | O(catalog) | real systems use an inverted index |
| overdue | O(all copies) | a priority queue by due date would make it O(k log n) |

## Interview talking points
- The key modeling insight is **title vs copy** (also **product vs inventory item** and **flight vs seat**). Interviewers look for it.
- Business rules belong in one place (the façade or domain services), not scattered across entity setters.
- Holds: a FIFO queue per title. Real systems also add hold expiry (the copy returns to the shelf if not picked up within N days, which needs a scheduled job) and notifications (Observer).
- For scale or persistence, this maps onto tables `books`, `copies`, `members`, `loans` and `holds`, with transactions around checkout and return so two clerks can't lend the same copy.
- Using `Clock` instead of `LocalDate.now()` is the standard way to make time-dependent logic testable.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
