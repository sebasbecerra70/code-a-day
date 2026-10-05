# String template engine (Mustache-style)

**Problem:** Render templates like `Hello {{ user.name | upper }}` with HTML escaping, raw output, filters, conditionals, loops, and comments. Compile once to an AST, then render many times fast.

## Approach
- **Tokenize** with one global regex that finds `{{{ raw }}}` or `{{ tag }}`. The text between matches becomes text nodes.
- **Parse** with a stack of open blocks. `{{#if}}` / `{{#each}}` push a node, `{{else}}` switches the insertion target to the `else` branch, and `{{/x}}` pops after checking it matches. Leftover stack entries mean an unclosed block. All syntax errors surface at compile time.
- **Variables:** `path | filter | filter`. Filters are resolved and validated at compile time, so typos fail early.
- **Scopes:** rendering carries a stack of context objects. `{{#each}}` pushes a meta scope (`@index`, `@first`, `@last`) and then the item. Lookup searches inward to outward, so loop bodies can still read outer fields. `this` / `.` is the innermost scope.
- **Escaping:** `{{x}}` HTML-escapes `& < > " '`, and `{{{x}}}` emits raw text. `null`/`undefined` render as empty, while `0` and `false` render as text.
- Truthiness follows Handlebars: empty arrays are falsy.

## Complexity
| Aspect | Cost |
|--------|------|
| Compile | O(template length) |
| Render | O(output size + nodes visited · path depth · scope depth) |
| Space | O(AST) + O(nesting depth) |

## Interview talking points
- Why compile to an AST (or to a JS function, as Handlebars does)? Parsing happens once, and rendering is just a tree walk.
- Safe by default: escaping is opt-out, not opt-in. That's the main XSS defense in server-side templates.
- Scope-chain lookup is a design choice. Mustache does it too, but it can surprise users when a missing inner field silently resolves to an outer one.
- Logic-less vs logic-ful templates: Mustache intentionally avoids expressions. Jinja/EJS allow arbitrary code (more power, more risk).
- Extensions: partials (`{{> name}}`), whitespace control (`{{~ }}`), helpers with arguments, source positions in errors, streaming output.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
