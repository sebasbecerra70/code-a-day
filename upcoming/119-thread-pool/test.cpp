#include "solution.hpp"

#include <atomic>
#include <cassert>
#include <chrono>
#include <iostream>
#include <numeric>
#include <set>
#include <string>

static void test_returns_values() {
    ThreadPool pool(4);
    auto a = pool.submit([] { return 6 * 7; });
    auto b = pool.submit([](int x, int y) { return x + y; }, 2, 3);
    auto c = pool.submit([](std::string s) { return s + "!"; }, std::string("hi"));
    assert(a.get() == 42 && b.get() == 5 && c.get() == "hi!");
}

static void test_void_tasks() {
    std::atomic<int> counter{0};
    {
        ThreadPool pool(3);
        for (int i = 0; i < 100; ++i) pool.submit([&counter] { ++counter; });
    }  // destructor drains the queue
    assert(counter == 100);
}

static void test_exception_propagates() {
    ThreadPool pool(2);
    auto f = pool.submit([]() -> int { throw std::logic_error("bad"); });
    bool threw = false;
    try { f.get(); } catch (const std::logic_error&) { threw = true; }
    assert(threw);
    assert(pool.submit([] { return 1; }).get() == 1);  // pool still works
}

static void test_parallel_sum() {
    ThreadPool pool(4);
    std::vector<long long> data(1'000'000);
    std::iota(data.begin(), data.end(), 1);
    const std::size_t chunks = 8, step = data.size() / chunks;
    std::vector<std::future<long long>> parts;
    for (std::size_t c = 0; c < chunks; ++c)
        parts.push_back(pool.submit([&data, c, step] {
            return std::accumulate(data.begin() + c * step, data.begin() + (c + 1) * step, 0LL);
        }));
    long long total = 0;
    for (auto& p : parts) total += p.get();
    assert(total == 1'000'000LL * 1'000'001 / 2);
}

static void test_uses_multiple_threads() {
    ThreadPool pool(4);
    std::mutex m;
    std::set<std::thread::id> ids;
    std::atomic<int> arrived{0};
    std::vector<std::future<void>> fs;
    for (int i = 0; i < 4; ++i)
        fs.push_back(pool.submit([&] {
            {
                std::lock_guard<std::mutex> lock(m);
                ids.insert(std::this_thread::get_id());
            }
            ++arrived;
            // Wait (bounded) until all four tasks run at once, proving they're concurrent.
            auto deadline = std::chrono::steady_clock::now() + std::chrono::seconds(5);
            while (arrived < 4 && std::chrono::steady_clock::now() < deadline) std::this_thread::yield();
        }));
    for (auto& f : fs) f.get();
    assert(ids.size() == 4);
}

static void test_move_only_arguments() {
    ThreadPool pool(1);
    auto f = pool.submit([](std::unique_ptr<int> p) { return *p * 2; }, std::make_unique<int>(21));
    assert(f.get() == 42);
}

static void test_single_thread_is_fifo() {
    ThreadPool pool(1);
    std::vector<int> order;
    std::vector<std::future<void>> fs;
    for (int i = 0; i < 20; ++i) fs.push_back(pool.submit([&order, i] { order.push_back(i); }));
    for (auto& f : fs) f.get();
    for (int i = 0; i < 20; ++i) assert(order[i] == i);
}

static void test_zero_threads_becomes_one() {
    ThreadPool pool(0);
    assert(pool.size() == 1 && pool.submit([] { return 7; }).get() == 7);
}

int main() {
    void (*tests[])() = {test_returns_values, test_void_tasks, test_exception_propagates,
                         test_parallel_sum, test_uses_multiple_threads, test_move_only_arguments,
                         test_single_thread_is_fifo, test_zero_threads_becomes_one};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
