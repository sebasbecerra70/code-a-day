"""0/1 knapsack: maximize value within a weight capacity, each item used at most once."""


def knapsack_value(weights: list[int], values: list[int], capacity: int) -> int:
    """Space-optimized DP: dp[c] = best value with capacity c. O(n*W) time, O(W) space."""
    _validate(weights, values, capacity)
    dp = [0] * (capacity + 1)
    for w, v in zip(weights, values):
        # Iterate capacity downward so each item is counted at most once.
        for c in range(capacity, w - 1, -1):
            dp[c] = max(dp[c], dp[c - w] + v)
    return dp[capacity]


def knapsack_items(weights: list[int], values: list[int], capacity: int) -> tuple[int, list[int]]:
    """Full 2-D table so we can walk back and recover which items were chosen."""
    _validate(weights, values, capacity)
    n = len(weights)
    # dp[i][c] = best value using the first i items with capacity c
    dp = [[0] * (capacity + 1) for _ in range(n + 1)]
    for i in range(1, n + 1):
        w, v = weights[i - 1], values[i - 1]
        for c in range(capacity + 1):
            dp[i][c] = dp[i - 1][c]
            if w <= c and dp[i - 1][c - w] + v > dp[i][c]:
                dp[i][c] = dp[i - 1][c - w] + v
    chosen, c = [], capacity
    for i in range(n, 0, -1):
        if dp[i][c] != dp[i - 1][c]:  # item i-1 was taken
            chosen.append(i - 1)
            c -= weights[i - 1]
    return dp[n][capacity], chosen[::-1]


def _validate(weights, values, capacity):
    if len(weights) != len(values):
        raise ValueError("weights and values must have the same length")
    if capacity < 0 or any(w < 0 for w in weights):
        raise ValueError("capacity and weights must be non-negative")
