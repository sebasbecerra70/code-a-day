# URL router (trie with params and wildcards)

**Problem:** Build the routing core of a web framework: register patterns like `/users/:id/posts/:postId` and `/static/*path` per HTTP method, then match incoming paths to a handler and extract parameters.

## Approach
- One **segment trie** per method. Each node has a map of static children, at most one param child (`:name`), at most one catch-all (`*name`), and an optional handler.
- **Matching** is a DFS over path segments with a fixed priority at each node: static child first, then param, then wildcard. If a branch dead-ends, undo any param it set and try the next option (backtracking). So `/a/b/d` still matches `/a/:x/d` even though `/a/b/c` exists.
- Catch-all captures the remaining segments joined with `/` (possibly empty) and must be the last segment.
- Paths are normalized: empty segments dropped (trailing slashes ignored), query string stripped, segments URL-decoded (malformed escapes left as-is).
- Registration rejects duplicates, conflicting param names at the same position, and misplaced wildcards, so ambiguity is caught at startup.
- `allowedMethods` supports `405 Method Not Allowed` with an `Allow` header.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| add | O(segments) | O(total segments of all routes) |
| match | O(segments) typical; backtracking can revisit in pathological route sets | O(segments) |

## Interview talking points
- Why a trie instead of a list of regexes? Linear regex scanning is O(routes) per request and order-dependent; the trie is proportional to path length.
- Radix trees (httprouter, find-my-way) compress single-child chains and match at the character level.
- Specificity rules (static > param > wildcard) make results independent of registration order.
- Decode *after* splitting, or `%2F` inside a param would be treated as a separator.
- Extensions: param constraints (`:id(\\d+)`), optional segments, reverse routing (building URLs from names).

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
