#include "solution.hpp"

#include <cassert>
#include <cstdint>
#include <iostream>
#include <random>
#include <set>
#include <stdexcept>
#include <string>

struct Particle {
    static int alive;
    double x, y;
    std::string tag;
    Particle(double x_, double y_, std::string t) : x(x_), y(y_), tag(std::move(t)) { ++alive; }
    ~Particle() { --alive; }
};
int Particle::alive = 0;

struct Throws {
    explicit Throws(bool fail) {
        if (fail) throw std::runtime_error("boom");
    }
};

struct alignas(32) Aligned {
    char c;
};

static void test_create_and_destroy() {
    ObjectPool<Particle, 4> pool;
    Particle* p = pool.create(1.0, 2.0, "a");
    assert(p->x == 1.0 && p->tag == "a" && Particle::alive == 1 && pool.live() == 1);
    pool.destroy(p);
    assert(Particle::alive == 0 && pool.live() == 0);
    pool.destroy(nullptr);  // no-op
}

static void test_slot_reuse_lifo() {
    ObjectPool<int, 8> pool;
    int* a = pool.create(1);
    pool.destroy(a);
    int* b = pool.create(2);
    assert(a == b && *b == 2);  // most recently freed slot is reused first
    pool.destroy(b);
}

static void test_grows_in_chunks() {
    ObjectPool<int, 4> pool;
    assert(pool.capacity() == 0);
    std::vector<int*> v;
    for (int i = 0; i < 9; ++i) v.push_back(pool.create(i));
    assert(pool.capacity() == 12 && pool.live() == 9);
    for (int i = 0; i < 9; ++i) assert(*v[i] == i);
    for (int* p : v) pool.destroy(p);
    for (int i = 0; i < 12; ++i) v.push_back(pool.create(i));
    assert(pool.capacity() == 12);  // freed slots are reused, no new chunk
    for (std::size_t i = 9; i < v.size(); ++i) pool.destroy(v[i]);
}

static void test_distinct_addresses() {
    ObjectPool<long long, 16> pool;
    std::set<long long*> seen;
    std::vector<long long*> v;
    for (int i = 0; i < 100; ++i) {
        long long* p = pool.create(i);
        assert(seen.insert(p).second);
        v.push_back(p);
    }
    for (auto* p : v) pool.destroy(p);
}

static void test_alignment() {
    ObjectPool<Aligned, 5> pool;
    std::vector<Aligned*> v;
    for (int i = 0; i < 12; ++i) {
        Aligned* p = pool.create();
        assert(reinterpret_cast<std::uintptr_t>(p) % alignof(Aligned) == 0);
        v.push_back(p);
    }
    for (auto* p : v) pool.destroy(p);
}

static void test_constructor_exception_returns_slot() {
    ObjectPool<Throws, 2> pool;
    bool threw = false;
    try { pool.create(true); } catch (const std::runtime_error&) { threw = true; }
    assert(threw && pool.live() == 0);
    Throws* a = pool.create(false);
    Throws* b = pool.create(false);
    assert(pool.capacity() == 2);  // the failed slot was recycled
    pool.destroy(a);
    pool.destroy(b);
}

static void test_raii_handle() {
    ObjectPool<Particle, 4> pool;
    {
        auto p = makePooled(pool, 3.0, 4.0, "raii");
        assert(Particle::alive == 1 && p->y == 4.0);
    }
    assert(Particle::alive == 0 && pool.live() == 0);
}

static void test_random_churn() {
    std::mt19937 rng(115);
    ObjectPool<Particle, 7> pool;
    std::vector<Particle*> live;
    for (int step = 0; step < 20000; ++step) {
        if (!live.empty() && rng() % 2) {
            std::size_t i = rng() % live.size();
            pool.destroy(live[i]);
            live[i] = live.back();
            live.pop_back();
        } else {
            live.push_back(pool.create(step, -step, std::to_string(step)));
        }
        assert(pool.live() == live.size() && Particle::alive == static_cast<int>(live.size()));
    }
    for (auto* p : live) {
        assert(p->tag == std::to_string(static_cast<int>(p->x)));  // no slot was shared
        pool.destroy(p);
    }
    assert(Particle::alive == 0);
}

int main() {
    void (*tests[])() = {test_create_and_destroy, test_slot_reuse_lifo, test_grows_in_chunks,
                         test_distinct_addresses, test_alignment,
                         test_constructor_exception_returns_slot, test_raii_handle,
                         test_random_churn};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
