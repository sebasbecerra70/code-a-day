# Object pool allocator (fixed-size blocks with a free list)

**Problem:** Many short-lived objects of the same type (particles, network packets, AST nodes) make `new`/`delete` a bottleneck and fragment the heap. Build a pool that hands out slots for `T` in O(1), recycles them in O(1), grows on demand, and supports RAII handles.

## Approach
- Allocate memory in **chunks** of `BlockCount` slots. A chunk is never freed or moved until the pool dies, so pointers stay valid.
- Each slot is a `union` of raw storage for a `T` (correctly aligned) and a `Slot* next`. A free slot stores the link to the next free slot **inside itself**: an *intrusive free list* with zero extra memory.
- `create`: pop the head of the free list (growing if empty), then placement-new `T` there. If the constructor throws, push the slot back.
- `destroy`: call `~T()` explicitly and push the slot onto the free list. LIFO reuse keeps recently used memory hot in cache.
- `makePooled` wraps the result in a `std::unique_ptr` with a deleter that returns the slot to the pool.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| create | O(1) amortized (O(BlockCount) when a chunk is added) | — |
| destroy | O(1) | — |
| overhead | — | max(sizeof(T), sizeof(void*)) per slot, no headers |

## Interview talking points
- General-purpose `malloc` has to handle any size, keep headers, and often take locks; a pool only handles one size, so it's simpler and faster.
- Placement new + explicit destructor call separates *memory lifetime* from *object lifetime*, which is the key idea behind allocators and `std::vector`.
- Not thread-safe as written: options are a mutex, per-thread pools, or a lock-free stack (watch out for the ABA problem).
- Debug builds could poison freed slots and track double-frees; this version trusts the caller.
- Related: arena/bump allocators (free everything at once), slab allocators in the Linux kernel, `std::pmr::unsynchronized_pool_resource`.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
