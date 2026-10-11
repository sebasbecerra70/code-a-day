#include "solution.hpp"

#include <cassert>
#include <iostream>
#include <string>
#include <type_traits>

// Counts live instances so we can prove destructors run exactly once.
struct Tracked {
    static int alive;
    int value;
    explicit Tracked(int v) : value(v) { ++alive; }
    virtual ~Tracked() { --alive; }
};
int Tracked::alive = 0;

struct Derived : Tracked {
    explicit Derived(int v) : Tracked(v) {}
};

static void test_default_is_null() {
    UniquePtr<int> p;
    assert(!p && p.get() == nullptr);
    UniquePtr<int> q = nullptr;
    assert(!q);
}

static void test_destructor_frees() {
    {
        auto p = makeUnique<Tracked>(5);
        assert(Tracked::alive == 1 && p->value == 5 && (*p).value == 5);
    }
    assert(Tracked::alive == 0);
}

static void test_not_copyable() {
    static_assert(!std::is_copy_constructible_v<UniquePtr<int>>);
    static_assert(!std::is_copy_assignable_v<UniquePtr<int>>);
    static_assert(std::is_nothrow_move_constructible_v<UniquePtr<int>>);
}

static void test_move_transfers_ownership() {
    auto a = makeUnique<Tracked>(1);
    Tracked* raw = a.get();
    UniquePtr<Tracked> b = std::move(a);
    assert(!a && b.get() == raw);  // NOLINT: moved-from is null by contract
    UniquePtr<Tracked> c;
    c = std::move(b);
    assert(!b && c.get() == raw);  // NOLINT
    assert(Tracked::alive == 1);
    c = nullptr;
    assert(Tracked::alive == 0);
}

static void test_move_assign_deletes_old() {
    auto a = makeUnique<Tracked>(1);
    auto b = makeUnique<Tracked>(2);
    assert(Tracked::alive == 2);
    a = std::move(b);
    assert(Tracked::alive == 1 && a->value == 2);
    a.reset();
    assert(Tracked::alive == 0);
}

static void test_release_and_reset() {
    auto p = makeUnique<Tracked>(3);
    Tracked* raw = p.release();
    assert(!p && Tracked::alive == 1);
    p.reset(raw);
    assert(p.get() == raw);
    p.reset(new Tracked(4));
    assert(Tracked::alive == 1 && p->value == 4);
    p.reset();
    assert(Tracked::alive == 0);
}

static void test_custom_deleter() {
    int calls = 0;
    auto deleter = [&calls](int* q) { ++calls; delete q; };
    {
        UniquePtr<int, decltype(deleter)> p(new int(9), deleter);
        assert(*p == 9);
    }
    assert(calls == 1);
}

static void test_derived_to_base() {
    UniquePtr<Tracked> base = makeUnique<Derived>(7);
    assert(base->value == 7 && Tracked::alive == 1);
    base.reset();
    assert(Tracked::alive == 0);  // virtual destructor ran
}

static void test_swap() {
    auto a = makeUnique<std::string>("a");
    auto b = makeUnique<std::string>("b");
    a.swap(b);
    assert(*a == "b" && *b == "a");
}

static void test_size_matches_raw_pointer() {
    static_assert(sizeof(UniquePtr<int>) <= 2 * sizeof(int*));
}

int main() {
    void (*tests[])() = {test_default_is_null, test_destructor_frees, test_not_copyable,
                         test_move_transfers_ownership, test_move_assign_deletes_old,
                         test_release_and_reset, test_custom_deleter, test_derived_to_base,
                         test_swap, test_size_matches_raw_pointer};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
