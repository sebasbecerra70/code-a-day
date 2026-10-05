#pragma once
// Fixed-capacity circular buffer. Capacity is a template parameter so storage lives inline.
// When full, push() fails; push_overwrite() drops the oldest element instead.

#include <array>
#include <cstddef>
#include <optional>
#include <stdexcept>

template <typename T, std::size_t N>
class RingBuffer {
    static_assert(N > 0, "capacity must be positive");

public:
    // Returns false if the buffer is full.
    bool push(const T& v) {
        if (full()) return false;
        buf_[(head_ + size_) % N] = v;
        ++size_;
        return true;
    }

    // Always succeeds; overwrites the oldest element when full.
    void push_overwrite(const T& v) {
        buf_[(head_ + size_) % N] = v;
        if (full()) head_ = (head_ + 1) % N;
        else ++size_;
    }

    std::optional<T> pop() {
        if (empty()) return std::nullopt;
        T v = std::move(buf_[head_]);
        head_ = (head_ + 1) % N;
        --size_;
        return v;
    }

    const T& front() const {
        if (empty()) throw std::out_of_range("front on empty buffer");
        return buf_[head_];
    }
    const T& back() const {
        if (empty()) throw std::out_of_range("back on empty buffer");
        return buf_[(head_ + size_ - 1) % N];
    }
    // i = 0 is the oldest element.
    const T& operator[](std::size_t i) const { return buf_[(head_ + i) % N]; }

    std::size_t size() const { return size_; }
    static constexpr std::size_t capacity() { return N; }
    bool empty() const { return size_ == 0; }
    bool full() const { return size_ == N; }
    void clear() { head_ = size_ = 0; }

private:
    std::array<T, N> buf_{};
    std::size_t head_ = 0;  // index of oldest element
    std::size_t size_ = 0;  // tracking size avoids the "one wasted slot" trick
};
