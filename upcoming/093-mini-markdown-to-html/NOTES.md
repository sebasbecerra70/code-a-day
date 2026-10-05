# Mini Markdown to HTML converter

**Problem:** Convert a practical subset of Markdown (headings, paragraphs, emphasis, inline code, links, fenced code, blockquotes, lists, horizontal rules) to safe HTML.

## Approach
- **Two passes**, mirroring real parsers like CommonMark: first split the document into blocks, then render inline markup inside each block.
- **Block pass:** walk lines with an index. The first line of each block picks the block type (fence, `hr`, heading, quote, list, or paragraph), and that branch consumes lines until the block ends. Paragraphs end at a blank line or when another block starts.
- Blockquotes strip `> ` and recursively call the block parser, so quotes can contain headings, lists, and code.
- **Inline pass:** split on backtick code spans first so their contents stay literal. Escape HTML, then apply links, then `**strong**`, then `*em*` (strong first, so `**` isn't read as two `*`). Lookarounds `(?=\S)` / `(?<=\S)` stop `2 * 3 * 4` from becoming emphasis.
- **Safety:** all text is HTML-escaped before markup is added, and `javascript:` hrefs are dropped.

## Complexity
| Aspect | Cost |
|--------|------|
| Time | O(n) in document length (regexes here are linear in practice) |
| Space | O(n) |

## Interview talking points
- Why block-then-inline? Block structure decides where inline content lives. A `*` inside a code fence is never emphasis.
- Regex limitations: nested emphasis, `***both***`, and CommonMark's delimiter-run rules really need a delimiter stack (the spec's "process emphasis" algorithm).
- Escape first, then add trusted tags: the safest ordering for XSS. Production renderers still sanitize the output.
- Nested lists need indentation tracking: keep a stack of open lists keyed by indent.
- Extensions: tables, task lists, autolinks, reference-style links (needs a first pass to collect definitions).

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
