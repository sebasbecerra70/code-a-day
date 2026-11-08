#include "solution.hpp"

#include <algorithm>
#include <cassert>
#include <iostream>
#include <random>

using namespace bits;

static int naivePopcount(std::uint64_t x) {
    int n = 0;
    for (int i = 0; i < 64; ++i) n += (x >> i) & 1;
    return n;
}

static void test_power_of_two() {
    assert(!isPowerOfTwo(0) && isPowerOfTwo(1) && isPowerOfTwo(1ULL << 63));
    assert(!isPowerOfTwo(6) && !isPowerOfTwo(~0ULL));
    assert(nextPowerOfTwo(0) == 1 && nextPowerOfTwo(1) == 1 && nextPowerOfTwo(5) == 8);
    assert(nextPowerOfTwo(64) == 64 && nextPowerOfTwo(65) == 128);
}

static void test_popcount_ctz_lowbit_random() {
    std::mt19937_64 rng(1);
    for (int t = 0; t < 10000; ++t) {
        std::uint64_t x = rng() >> (rng() % 64);
        assert(popcount(x) == naivePopcount(x));
        if (x) {
            int tz = countTrailingZeros(x);
            assert(((x >> tz) & 1) && (x & ((1ULL << tz) - 1)) == 0);
            assert(lowestSetBit(x) == (1ULL << tz));
        }
    }
    assert(countTrailingZeros(0) == 64 && lowestSetBit(0) == 0);
}

static void test_reverse_bits() {
    assert(reverseBits(1) == 0x80000000u);
    assert(reverseBits(0b1011) == 0xD0000000u);
    std::mt19937 rng(2);
    for (int t = 0; t < 1000; ++t) {
        std::uint32_t x = rng(), naive = 0;
        for (int i = 0; i < 32; ++i) naive |= ((x >> i) & 1u) << (31 - i);
        assert(reverseBits(x) == naive);
    }
}

static void test_single_numbers() {
    assert(singleNumber({4, 1, 2, 1, 2}) == 4);
    assert(singleNumber({-7}) == -7);
    assert((twoSingleNumbers({1, 2, 1, 3, 2, 5}) == std::pair{3, 5}));
    assert((twoSingleNumbers({-1, 0}) == std::pair{-1, 0}));
}

static void test_gray_code() {
    for (std::uint32_t i = 0; i < 1024; ++i) {
        assert(fromGray(toGray(i)) == i);
        assert(popcount(toGray(i) ^ toGray(i + 1)) == 1);
    }
}

static void test_gosper() {
    // All 5-bit numbers with exactly two bits set, in order.
    std::vector<std::uint64_t> got;
    for (std::uint64_t x = 0b11; x < 32; x = nextSamePopcount(x)) got.push_back(x);
    std::vector<std::uint64_t> expected;
    for (std::uint64_t x = 0; x < 32; ++x)
        if (naivePopcount(x) == 2) expected.push_back(x);
    assert(got == expected);
}

static void test_submasks() {
    auto s = submasks(0b1010);
    assert((s == std::vector<std::uint32_t>{0b1010, 0b1000, 0b0010, 0}));
    assert(submasks(0).size() == 1);
    assert(submasks(0xFF).size() == 256);
}

static void test_add_without_plus() {
    std::mt19937 rng(3);
    for (int t = 0; t < 1000; ++t) {
        std::uint32_t a = rng(), b = rng();
        assert(addWithoutPlus(a, b) == a + b);  // wraps mod 2^32 like unsigned +
    }
}

int main() {
    void (*tests[])() = {test_power_of_two, test_popcount_ctz_lowbit_random, test_reverse_bits,
                         test_single_numbers, test_gray_code, test_gosper, test_submasks,
                         test_add_without_plus};
    for (auto t : tests) t();
    std::cout << "All " << std::size(tests) << " tests passed\n";
}
