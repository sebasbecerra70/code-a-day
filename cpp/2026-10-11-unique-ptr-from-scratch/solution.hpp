#pragma once
// A simplified std::unique_ptr: single ownership, move-only, custom deleter support.

#include <cstddef>
#include <type_traits>
#include <utility>

template <typename T>
struct DefaultDelete {
    DefaultDelete() noexcept = default;
    // Allows DefaultDelete<Derived> -> DefaultDelete<Base>.
    template <typename U, typename = std::enable_if_t<std::is_convertible_v<U*, T*>>>
    DefaultDelete(const DefaultDelete<U>&) noexcept {}
    void operator()(T* p) const noexcept { delete p; }
};

template <typename T, typename Deleter = DefaultDelete<T>>
class UniquePtr {
public:
    UniquePtr() noexcept = default;
    UniquePtr(std::nullptr_t) noexcept {}
    explicit UniquePtr(T* p, Deleter d = Deleter()) noexcept : ptr_(p), del_(std::move(d)) {}

    // Ownership is unique, so copying is forbidden.
    UniquePtr(const UniquePtr&) = delete;
    UniquePtr& operator=(const UniquePtr&) = delete;

    UniquePtr(UniquePtr&& other) noexcept
        : ptr_(other.release()), del_(std::move(other.del_)) {}

    // Converting move, e.g. UniquePtr<Derived> -> UniquePtr<Base>.
    template <typename U, typename E,
              typename = std::enable_if_t<std::is_convertible_v<U*, T*> &&
                                          std::is_convertible_v<E, Deleter>>>
    UniquePtr(UniquePtr<U, E>&& other) noexcept
        : ptr_(other.release()), del_(std::move(other.get_deleter())) {}

    UniquePtr& operator=(UniquePtr&& other) noexcept {
        if (this != &other) {
            reset(other.release());
            del_ = std::move(other.del_);
        }
        return *this;
    }

    UniquePtr& operator=(std::nullptr_t) noexcept {
        reset();
        return *this;
    }

    ~UniquePtr() { reset(); }

    // Gives up ownership without deleting.
    T* release() noexcept { return std::exchange(ptr_, nullptr); }

    // Swap in the new pointer first, then delete the old one, so self-reset is safe.
    void reset(T* p = nullptr) noexcept {
        T* old = std::exchange(ptr_, p);
        if (old) del_(old);
    }

    void swap(UniquePtr& other) noexcept {
        std::swap(ptr_, other.ptr_);
        std::swap(del_, other.del_);
    }

    T* get() const noexcept { return ptr_; }
    Deleter& get_deleter() noexcept { return del_; }
    T& operator*() const { return *ptr_; }
    T* operator->() const noexcept { return ptr_; }
    explicit operator bool() const noexcept { return ptr_ != nullptr; }

private:
    T* ptr_ = nullptr;
    Deleter del_{};
};

template <typename T, typename... Args>
UniquePtr<T> makeUnique(Args&&... args) {
    return UniquePtr<T>(new T(std::forward<Args>(args)...));
}
