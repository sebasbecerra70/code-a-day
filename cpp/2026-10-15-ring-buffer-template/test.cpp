#include "solution.hpp"

#include <cassert>
#include <deque>
#include <iostream>
#include <random>
#include <string>

static void test_empty() {
    RingBuffer<int, 3> rb;
    assert(rb.empty() && !rb.full() && rb.size() == 0);
    assert(!rb.pop().has_value());
    bool threw = false;
    try { rb.front(); } catch (const std::out_of_range&) { threw = true; }
    assert(threw);
}

static void test_fifo_order() {
    RingBuffer<int, 4> rb;
    for (int i = 1; i <= 4; ++i) assert(rb.push(i));
    for (int i = 1; i <= 4; ++i) assert(*rb.pop() == i);
    assert(rb.empty());
}

static void test_push_fails_when_full() {
    RingBuffer<int, 2> rb;
    assert(rb.push(1) && rb.push(2));
    assert(rb.full() && !rb.push(3));
    assert(rb.front() == 1 && rb.back() == 2);
}

static void test_wraparound() {
    RingBuffer<int, 3> rb;
    rb.push(1); rb.push(2); rb.push(3);
    rb.pop(); rb.pop();
    rb.push(4); rb.push(5);  // these wrap to the start of storage
    assert(rb.size() == 3);
    assert(rb[0] == 3 && rb[1] == 4 && rb[2] == 5);
    assert(rb.back() == 5);
}

static void test_overwrite_drops_oldest() {
    RingBuffer<int, 3> rb;
    for (int i = 1; i <= 5; ++i) rb.push_overwrite(i);
    assert(rb.size() == 3 && rb.front() == 3 && rb.back() == 5);
}

static void test_capacity_one() {
    RingBuffer<std::string, 1> rb;
    assert(rb.push("a") && !rb.push("b"));
    rb.push_overwrite("c");
    assert(rb.front() == "c" && *rb.pop() == "c" && rb.empty());
}

static void test_clear() {
    RingBuffer<int, 2> rb;
    rb.push(1); rb.push(2);
    rb.clear();
    assert(rb.empty() && rb.push(9) && rb.front() == 9);
}

static void test_random_against_deque() {
    std::mt19937 rng(7);
    RingBuffer<int, 5> rb;
    std::deque<int> ref;
    for (int step = 0; step < 10000; ++step) {
        int op = static_cast<int>(rng() % 3);
        int v = static_cast<int>(rng() % 100);
        if (op == 0) {
            bool ok = rb.push(v);
            assert(ok == (ref.size() < 5));
            if (ok) ref.push_back(v);
        } else if (op == 1) {
            rb.push_overwrite(v);
            if (ref.size() == 5) ref.pop_front();
            ref.push_back(v);
        } else {
            auto got = rb.pop();
            assert(got.has_value() == !ref.empty());
            if (got) { assert(*got == ref.front()); ref.pop_front(); }
        }
        assert(rb.size() == ref.size());
        for (std::size_t i = 0; i < ref.size(); ++i) assert(rb[i] == ref[i]);
    }
}

int main() {
    void (*tests[])() = {test_empty, test_fifo_order, test_push_fails_when_full, test_wraparound,
                         test_overwrite_drops_oldest, test_capacity_one, test_clear,
                         test_random_against_deque};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
