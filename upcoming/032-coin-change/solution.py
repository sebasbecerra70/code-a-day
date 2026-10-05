"""Coin change: fewest coins to make an amount, and number of ways to make it."""

from __future__ import annotations


def min_coins(coins: list[int], amount: int) -> int:
    """Fewest coins summing to `amount` (unlimited supply), or -1 if impossible."""
    _validate(coins, amount)
    INF = amount + 1  # more coins than could ever be needed
    dp = [0] + [INF] * amount
    for a in range(1, amount + 1):
        for c in coins:
            if c <= a and dp[a - c] + 1 < dp[a]:
                dp[a] = dp[a - c] + 1
    return -1 if dp[amount] == INF else dp[amount]


def min_coins_combo(coins: list[int], amount: int) -> list[int] | None:
    """One optimal multiset of coins (sorted), or None if impossible."""
    _validate(coins, amount)
    INF = amount + 1
    dp = [0] + [INF] * amount
    last = [0] * (amount + 1)  # last coin used to reach each amount optimally
    for a in range(1, amount + 1):
        for c in coins:
            if c <= a and dp[a - c] + 1 < dp[a]:
                dp[a], last[a] = dp[a - c] + 1, c
    if dp[amount] == INF:
        return None
    out = []
    while amount:
        out.append(last[amount])
        amount -= last[amount]
    return sorted(out)


def count_ways(coins: list[int], amount: int) -> int:
    """Number of distinct combinations (order doesn't matter)."""
    _validate(coins, amount)
    ways = [1] + [0] * amount
    # Coins in the outer loop: each combination is counted once, in coin order.
    for c in set(coins):
        for a in range(c, amount + 1):
            ways[a] += ways[a - c]
    return ways[amount]


def _validate(coins, amount):
    if amount < 0:
        raise ValueError("amount must be non-negative")
    if any(c <= 0 for c in coins):
        raise ValueError("coins must be positive")
