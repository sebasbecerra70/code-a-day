#pragma once
// Fixed-size thread pool. submit() returns a std::future for the task's result;
// exceptions thrown by a task are delivered through that future.
// The destructor finishes all queued tasks, then joins the workers.

#include <condition_variable>
#include <functional>
#include <future>
#include <memory>
#include <mutex>
#include <queue>
#include <stdexcept>
#include <thread>
#include <tuple>
#include <type_traits>
#include <utility>
#include <vector>

class ThreadPool {
public:
    explicit ThreadPool(std::size_t threads = std::thread::hardware_concurrency()) {
        if (threads == 0) threads = 1;
        workers_.reserve(threads);
        for (std::size_t i = 0; i < threads; ++i) workers_.emplace_back([this] { workerLoop(); });
    }

    ThreadPool(const ThreadPool&) = delete;
    ThreadPool& operator=(const ThreadPool&) = delete;

    ~ThreadPool() {
        {
            std::lock_guard<std::mutex> lock(mu_);
            stopping_ = true;
        }
        cv_.notify_all();
        for (auto& w : workers_) w.join();
    }

    template <typename F, typename... Args>
    auto submit(F&& f, Args&&... args) -> std::future<std::invoke_result_t<F, Args...>> {
        using R = std::invoke_result_t<F, Args...>;
        // packaged_task is move-only but std::function needs copyable callables,
        // so hold it through a shared_ptr.
        auto task = std::make_shared<std::packaged_task<R()>>(
            [fn = std::forward<F>(f), tup = std::make_tuple(std::forward<Args>(args)...)]() mutable {
                return std::apply(std::move(fn), std::move(tup));
            });
        std::future<R> result = task->get_future();
        {
            std::lock_guard<std::mutex> lock(mu_);
            if (stopping_) throw std::runtime_error("submit on stopped pool");
            tasks_.emplace([task] { (*task)(); });
        }
        cv_.notify_one();
        return result;
    }

    std::size_t size() const { return workers_.size(); }

private:
    void workerLoop() {
        while (true) {
            std::function<void()> job;
            {
                std::unique_lock<std::mutex> lock(mu_);
                // The predicate guards against spurious wakeups.
                cv_.wait(lock, [this] { return stopping_ || !tasks_.empty(); });
                if (stopping_ && tasks_.empty()) return;  // drain the queue before exiting
                job = std::move(tasks_.front());
                tasks_.pop();
            }
            job();  // run outside the lock so other workers can dequeue
        }
    }

    std::vector<std::thread> workers_;
    std::queue<std::function<void()>> tasks_;
    std::mutex mu_;
    std::condition_variable cv_;
    bool stopping_ = false;
};
