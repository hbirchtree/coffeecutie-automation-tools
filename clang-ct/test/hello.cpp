// Exercises the pieces of the runtime stack that commonly break when
// cross compiling: iostreams, exceptions/unwinding, threads, 64-bit atomics
// and <format>.
#include <atomic>
#include <cstdint>
#include <format>
#include <iostream>
#include <stdexcept>
#include <thread>
#include <vector>

int main()
{
    std::atomic<std::uint64_t> counter{0};
    std::vector<std::thread> threads;
    for(int i = 0; i < 4; i++)
        threads.emplace_back([&counter] {
            for(int j = 0; j < 1000; j++)
                counter.fetch_add(1);
        });
    for(auto& thread : threads)
        thread.join();

    try
    {
        throw std::runtime_error("exception caught");
    } catch(std::exception const& e)
    {
        std::cout << std::format("{}, counter={}\n", e.what(), counter.load());
    }

    return counter.load() == 4000 ? 0 : 1;
}
