#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <memory>
#include <random>
#include <string>
#include <vector>

static void test_empty() {
    DynamicArray<int> a;
    assert(a.empty() && a.size() == 0 && a.capacity() == 0);
    bool threw = false;
    try { a.pop_back(); } catch (const std::out_of_range&) { threw = true; }
    assert(threw);
}

static void test_push_and_index() {
    DynamicArray<int> a;
    for (int i = 0; i < 100; ++i) a.push_back(i * i);
    assert(a.size() == 100);
    for (int i = 0; i < 100; ++i) assert(a[i] == i * i);
}

static void test_geometric_growth() {
    DynamicArray<int> a;
    std::size_t reallocations = 0, last = 0;
    for (int i = 0; i < 1000; ++i) {
        a.push_back(i);
        if (a.capacity() != last) { ++reallocations; last = a.capacity(); }
    }
    assert(reallocations <= 11);  // 1,2,4,...,1024
    assert(a.capacity() >= a.size());
}

static void test_at_bounds() {
    DynamicArray<int> a;
    a.push_back(1);
    assert(a.at(0) == 1);
    bool threw = false;
    try { a.at(1); } catch (const std::out_of_range&) { threw = true; }
    assert(threw);
}

static void test_pop_back() {
    DynamicArray<std::string> a;
    a.push_back("x");
    a.push_back("y");
    a.pop_back();
    assert(a.size() == 1 && a[0] == "x");
}

static void test_copy_is_deep() {
    DynamicArray<std::string> a;
    a.push_back("hello");
    DynamicArray<std::string> b = a;
    b[0] = "changed";
    assert(a[0] == "hello" && b[0] == "changed");
    a = b;
    assert(a[0] == "changed");
}

static void test_move_steals_buffer() {
    DynamicArray<int> a;
    a.push_back(7);
    DynamicArray<int> b = std::move(a);
    assert(b.size() == 1 && b[0] == 7);
    assert(a.size() == 0 && a.capacity() == 0);  // NOLINT: checking moved-from state
}

static void test_move_only_type() {
    DynamicArray<std::unique_ptr<int>> a;
    for (int i = 0; i < 10; ++i) a.emplace_back(std::make_unique<int>(i));
    int sum = 0;
    for (auto& p : a) sum += *p;
    assert(sum == 45);
}

static void test_destructors_run() {
    auto counter = std::make_shared<int>(0);
    {
        DynamicArray<std::shared_ptr<int>> a;
        for (int i = 0; i < 20; ++i) a.push_back(counter);
        assert(counter.use_count() == 21);
        a.pop_back();
        assert(counter.use_count() == 20);
    }
    assert(counter.use_count() == 1);
}

static void test_random_against_vector() {
    std::mt19937 rng(42);
    DynamicArray<int> a;
    std::vector<int> ref;
    for (int step = 0; step < 5000; ++step) {
        if (!ref.empty() && rng() % 3 == 0) {
            a.pop_back();
            ref.pop_back();
        } else {
            int v = static_cast<int>(rng() % 1000);
            a.push_back(v);
            ref.push_back(v);
        }
        assert(a.size() == ref.size());
    }
    for (std::size_t i = 0; i < ref.size(); ++i) assert(a[i] == ref[i]);
}

int main() {
    void (*tests[])() = {test_empty, test_push_and_index, test_geometric_growth, test_at_bounds,
                         test_pop_back, test_copy_is_deep, test_move_steals_buffer,
                         test_move_only_type, test_destructors_run, test_random_against_vector};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
