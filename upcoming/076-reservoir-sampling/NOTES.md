# Reservoir sampling (uniform and weighted)

**Problem:** Choose `k` items uniformly at random from a stream whose length is unknown (or too large to store), in a single pass with O(k) memory. Also support weighted sampling.

## Approach
- **Algorithm R:** keep the first `k` items. For item `i` (0-based, `i ≥ k`), draw `j` in `[0, i]`; if `j < k`, replace `sample[j]`. Each item ends up in the final sample with probability `k/n` (proof by induction: survive each later step with probability `1 - 1/(t+1)`, which telescopes).
- **Algorithm L:** instead of a coin flip per item, compute how many items to *skip* before the next replacement (geometric distribution). Same output distribution, but only O(k·log(n/k)) random draws, which matters when the RNG is expensive.
- **Weighted (A-Res):** give each item the key `u^(1/w)` with `u` uniform in (0, 1), and keep the `k` largest keys in a min-heap. This samples without replacement, proportionally to weight.
- An injectable `random.Random` makes tests reproducible; distribution tests check frequencies over many trials.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| reservoir_sample | O(n) | O(k) |
| reservoir_sample_skip | O(n) iteration, O(k log(n/k)) RNG calls | O(k) |
| weighted_sample | O(n log k) | O(k) |

## Interview talking points
- Classic prompts: random line from a huge file, random node from a linked list (k = 1), sampling log events.
- Distributed version: sample each shard, then merge using the A-Res keys (or weight shards by size).
- Using `rng.random() < k/(i+1)` plus a random slot is equivalent to the single `randrange` trick.
- Shuffling the whole stream would need O(n) memory, which defeats the purpose.

## Run
From this folder: `python -m pytest -q`
