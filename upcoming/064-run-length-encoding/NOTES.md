# Run-length encoding (text and bytes)

**Problem:** Compress data by replacing runs of a repeated symbol with a count and the symbol, e.g. `"aaabcc"` → `"3a1b2c"`, and decode it back. Handle multi-digit counts, invalid input, and binary data.

## Approach
- **Text encode:** `itertools.groupby` yields consecutive runs; emit `len(run) + ch`. Digits in the input would make the format ambiguous (`"31"` could be three `1`s or part of a count), so they're rejected.
- **Text decode:** accumulate digits into a count; the next non-digit character closes the run. A character without a count, or a count at the end, is an error.
- **Bytes:** emit fixed-width `(count, value)` pairs. A count fits in one byte, so runs longer than 255 are split. Fixed width means no ambiguity for any byte value. A zero count or odd length is rejected.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| encode / decode (text) | O(n) | O(n) |
| encode_bytes / decode_bytes | O(n) | O(n) |

## Interview talking points
- RLE only helps with long runs; on varied data it can **double** the size (`"abc"` → `"1a1b1c"`). Variants emit literals for non-repeating stretches (PackBits, used in TIFF).
- Ambiguity is the core design issue: escape characters, fixed-width counts, or a separate run-flag bit.
- Real uses: fax (CCITT), BMP/TGA/PCX images, columnar databases (Parquet/ORC RLE for sorted or low-cardinality columns), and the run-length step inside bzip2 and JPEG.
- Follow-ups: in-place string compression (LeetCode 443), "look-and-say" sequence.

## Run
From this folder: `python -m pytest -q`
