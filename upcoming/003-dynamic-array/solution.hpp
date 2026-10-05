#pragma once
// A minimal std::vector-like growable array with geometric (2x) growth.
// Uses raw storage + placement new so elements are only constructed when pushed.

#include <cstddef>
#include <new>
#include <stdexcept>
#include <utility>

template <typename T>
class DynamicArray {
public:
    DynamicArray() = default;

    DynamicArray(const DynamicArray& other) {
        reserve(other.size_);
        for (std::size_t i = 0; i < other.size_; ++i) new (data_ + i) T(other.data_[i]);
        size_ = other.size_;
    }

    DynamicArray(DynamicArray&& other) noexcept
        : data_(std::exchange(other.data_, nullptr)),
          size_(std::exchange(other.size_, 0)),
          cap_(std::exchange(other.cap_, 0)) {}

    // Copy-and-swap gives strong exception safety for both copy and move assignment.
    DynamicArray& operator=(DynamicArray other) noexcept {
        swap(other);
        return *this;
    }

    ~DynamicArray() {
        clear();
        ::operator delete(data_);
    }

    void swap(DynamicArray& other) noexcept {
        std::swap(data_, other.data_);
        std::swap(size_, other.size_);
        std::swap(cap_, other.cap_);
    }

    template <typename... Args>
    T& emplace_back(Args&&... args) {
        if (size_ == cap_) reserve(cap_ == 0 ? 1 : cap_ * 2);
        T* p = new (data_ + size_) T(std::forward<Args>(args)...);
        ++size_;
        return *p;
    }
    void push_back(const T& v) { emplace_back(v); }
    void push_back(T&& v) { emplace_back(std::move(v)); }

    void pop_back() {
        if (size_ == 0) throw std::out_of_range("pop_back on empty array");
        data_[--size_].~T();
    }

    void reserve(std::size_t new_cap) {
        if (new_cap <= cap_) return;
        T* fresh = static_cast<T*>(::operator new(new_cap * sizeof(T)));
        for (std::size_t i = 0; i < size_; ++i) {
            new (fresh + i) T(std::move_if_noexcept(data_[i]));
            data_[i].~T();
        }
        ::operator delete(data_);
        data_ = fresh;
        cap_ = new_cap;
    }

    void clear() noexcept {
        while (size_ > 0) data_[--size_].~T();
    }

    T& operator[](std::size_t i) { return data_[i]; }
    const T& operator[](std::size_t i) const { return data_[i]; }

    T& at(std::size_t i) {
        if (i >= size_) throw std::out_of_range("index out of range");
        return data_[i];
    }
    const T& at(std::size_t i) const {
        if (i >= size_) throw std::out_of_range("index out of range");
        return data_[i];
    }

    std::size_t size() const { return size_; }
    std::size_t capacity() const { return cap_; }
    bool empty() const { return size_ == 0; }

    T* begin() { return data_; }
    T* end() { return data_ + size_; }
    const T* begin() const { return data_; }
    const T* end() const { return data_ + size_; }

private:
    T* data_ = nullptr;
    std::size_t size_ = 0;
    std::size_t cap_ = 0;
};
