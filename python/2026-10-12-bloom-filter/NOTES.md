# Bloom filter (optimal sizing, double hashing)

**Problem:** Answer "have I seen this item?" for a huge set using far less memory than storing the items, accepting a small, tunable false-positive rate but never a false negative.

## Approach
- A bit array of `m` bits and `k` hash functions. `add` sets `k` bits; `contains` checks that all `k` are set.
- Size from the desired capacity `n` and error rate `p`: `m = -n ln p / (ln 2)^2`, `k = (m/n) ln 2`.
- Instead of `k` independent hashes, derive them from one SHA-256 digest with double hashing: `h1 + i*h2 mod m` (Kirsch-Mitzenmacher). Forcing `h2` odd avoids every probe landing on the same bit.
- Bits are packed into a `bytearray` (8 per byte).

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| add       | O(k) | — |
| contains  | O(k) | — |
| total     | — | O(m) bits ≈ 9.6 bits/item at 1% |

## Interview talking points
- Why no false negatives? Bits are only ever set, never cleared, so an added item's bits stay on.
- No deletion: clearing a bit could break other items. A *counting* Bloom filter uses small counters instead; cuckoo filters support deletes too.
- FP rate rises as you overfill; `(1 - e^{-kn/m})^k` estimates it.
- Uses: skip disk lookups in LSM trees (Cassandra, RocksDB), CDN cache admission, "already crawled" URL sets, malicious-URL prechecks.

## Run
From this folder: `python -m pytest -q`
