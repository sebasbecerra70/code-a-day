# Dynamic array (growable vector from scratch)

**Problem:** Implement a `std::vector`-like container: amortized O(1) `push_back`, O(1) indexing, `pop_back`, and correct copy/move semantics, without using any standard container.

## Approach
- Keep three fields: a raw buffer pointer, `size`, and `capacity`.
- Allocate **raw memory** with `::operator new` and construct elements with **placement new**, so unused capacity holds no live objects.
- When full, **double** the capacity, move (or copy, via `std::move_if_noexcept`) elements into the new buffer, destroy the old ones and free the old buffer.
- Rule of five: deep copy constructor, stealing move constructor, and a single **copy-and-swap** assignment operator that handles both copy and move.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| push_back / emplace_back | O(1) amortized, O(n) worst | O(n) total |
| pop_back | O(1) | — |
| operator[] / at | O(1) | — |
| reserve | O(n) | O(new capacity) |

## Interview talking points
- Why doubling? With a growth factor *k > 1* the total copying is a geometric series, so n pushes cost O(n). Adding a constant each time would make it O(n²). Some implementations use 1.5x so freed blocks can be reused.
- `std::move_if_noexcept` keeps the strong exception guarantee: if a move constructor might throw, copying keeps the old buffer intact.
- Why `operator new` + placement new instead of `new T[cap]`? `new T[]` default-constructs every slot, which is wasteful and impossible for types with no default constructor.
- Iterators and references are invalidated on reallocation, which is the classic `std::vector` pitfall.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
