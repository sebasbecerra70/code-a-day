#pragma once
// Fixed-size object pool. Memory is allocated in chunks of BlockCount slots; free slots
// form an intrusive singly linked list threaded through the unused slots themselves,
// so allocate and deallocate are O(1) with no per-object bookkeeping.

#include <cstddef>
#include <memory>
#include <new>
#include <utility>
#include <vector>

template <typename T, std::size_t BlockCount = 64>
class ObjectPool {
    static_assert(BlockCount > 0, "chunks need at least one slot");

    // A slot holds either a live T or, when free, the pointer to the next free slot.
    union Slot {
        Slot* next;
        alignas(T) unsigned char storage[sizeof(T)];
    };

public:
    ObjectPool() = default;
    ObjectPool(const ObjectPool&) = delete;
    ObjectPool& operator=(const ObjectPool&) = delete;

    // Objects still alive at destruction are not destroyed: the owner must release them.
    ~ObjectPool() = default;

    template <typename... Args>
    T* create(Args&&... args) {
        if (!freeList_) grow();
        Slot* slot = freeList_;
        freeList_ = slot->next;
        try {
            T* obj = new (slot->storage) T(std::forward<Args>(args)...);
            ++live_;
            return obj;
        } catch (...) {
            // Constructor threw: return the slot to the free list.
            slot->next = freeList_;
            freeList_ = slot;
            throw;
        }
    }

    void destroy(T* obj) {
        if (!obj) return;
        obj->~T();
        // storage is the union's first byte, so the object's address is the slot's address.
        Slot* slot = reinterpret_cast<Slot*>(obj);
        slot->next = freeList_;
        freeList_ = slot;
        --live_;
    }

    std::size_t live() const { return live_; }
    std::size_t capacity() const { return chunks_.size() * BlockCount; }

private:
    void grow() {
        auto chunk = std::make_unique<Slot[]>(BlockCount);
        // Thread the new slots onto the free list in address order.
        for (std::size_t i = 0; i + 1 < BlockCount; ++i) chunk[i].next = &chunk[i + 1];
        chunk[BlockCount - 1].next = freeList_;
        freeList_ = &chunk[0];
        chunks_.push_back(std::move(chunk));
    }

    std::vector<std::unique_ptr<Slot[]>> chunks_;
    Slot* freeList_ = nullptr;
    std::size_t live_ = 0;
};

// RAII handle: returns the object to its pool on scope exit.
template <typename T, std::size_t N>
struct PoolDeleter {
    ObjectPool<T, N>* pool;
    void operator()(T* p) const { pool->destroy(p); }
};

template <typename T, std::size_t N>
using PoolPtr = std::unique_ptr<T, PoolDeleter<T, N>>;

template <typename T, std::size_t N, typename... Args>
PoolPtr<T, N> makePooled(ObjectPool<T, N>& pool, Args&&... args) {
    return PoolPtr<T, N>(pool.create(std::forward<Args>(args)...), PoolDeleter<T, N>{&pool});
}
