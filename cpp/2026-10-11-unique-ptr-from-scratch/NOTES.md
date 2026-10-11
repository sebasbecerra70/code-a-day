# unique_ptr from scratch (move-only ownership)

**Problem:** Implement a smart pointer that exclusively owns a heap object and deletes it when the owner goes out of scope. It must be movable but not copyable, and support `release`, `reset`, `get`, a custom deleter, and `Derived -> Base` conversion.

## Approach
- Store the raw pointer and a deleter object. The destructor calls `reset()`, which calls the deleter.
- **Delete** the copy constructor and copy assignment, so the compiler stops accidental double ownership.
- The move constructor/assignment `release()` the source, leaving it null.
- `reset(p)` swaps in `p` *before* deleting the old pointer, so the object stays valid even if the deleter somehow reaches back into it.
- A templated converting move constructor (constrained with `enable_if`) allows `UniquePtr<Derived>` to `UniquePtr<Base>`.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| construct / move / release / get | O(1) | O(1) |
| reset / destroy | O(1) + cost of `~T` | — |

## Interview talking points
- RAII: tying resource lifetime to scope makes leaks and double-frees impossible on every path, including exceptions.
- Real `std::unique_ptr` uses the empty base optimization (or `[[no_unique_address]]`) so a stateless deleter adds no size. It's exactly one pointer wide.
- Why `make_unique`? It avoids leaks in expressions like `f(UniquePtr<T>(new T), g())` (before C++17's evaluation-order rules) and avoids writing `new` by hand.
- Deleting through a base pointer requires a **virtual destructor** on the base, otherwise it's undefined behavior.
- `unique_ptr` vs `shared_ptr`: no reference count, no control block, no atomics. Use it by default.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
