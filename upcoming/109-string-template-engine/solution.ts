// A small Mustache-style template engine, compiled to an AST once and
// rendered many times:
//   {{ name }}  {{ user.name }}  escaped interpolation (dotted paths)
//   {{{ html }}}                 raw interpolation
//   {{ name | upper }}           filters (pipe chain)
//   {{#if cond}}...{{else}}...{{/if}}
//   {{#each items}}...{{/each}}  with {{this}}, {{@index}}, and item fields
//   {{! comment }}

type Node =
  | { kind: "text"; value: string }
  | { kind: "var"; path: string; filters: string[]; raw: boolean }
  | { kind: "if"; path: string; then: Node[]; else: Node[] }
  | { kind: "each"; path: string; body: Node[] };

export type Filter = (v: unknown) => unknown;

export class TemplateError extends Error {}

const TAG = /\{\{\{\s*(.*?)\s*\}\}\}|\{\{\s*(.*?)\s*\}\}/g;

export function escapeHtml(v: unknown): string {
  return String(v ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]!);
}

export function parse(src: string): Node[] {
  const root: Node[] = [];
  // Stack of open blocks; `target` is where new nodes go (then/else/body).
  const stack: { node: Node & { kind: "if" | "each" }; target: Node[] }[] = [];
  const target = () => (stack.length ? stack[stack.length - 1].target : root);
  let last = 0;
  for (const m of src.matchAll(TAG)) {
    if (m.index! > last) target().push({ kind: "text", value: src.slice(last, m.index) });
    last = m.index! + m[0].length;
    if (m[1] !== undefined) {
      target().push(parseVar(m[1], true));
      continue;
    }
    const tag = m[2];
    if (tag.startsWith("!")) continue;
    let mm: RegExpMatchArray | null;
    if ((mm = tag.match(/^#(if|each)\s+(\S+)$/))) {
      const node: Node & { kind: "if" | "each" } =
        mm[1] === "if" ? { kind: "if", path: mm[2], then: [], else: [] } : { kind: "each", path: mm[2], body: [] };
      target().push(node);
      stack.push({ node, target: node.kind === "if" ? node.then : node.body });
    } else if (tag === "else") {
      const top = stack[stack.length - 1];
      if (!top || top.node.kind !== "if" || top.target === top.node.else) throw new TemplateError("unexpected {{else}}");
      top.target = top.node.else;
    } else if ((mm = tag.match(/^\/(if|each)$/))) {
      const top = stack.pop();
      if (!top || top.node.kind !== mm[1]) throw new TemplateError(`unexpected {{/${mm[1]}}}`);
    } else if (/^[#/]/.test(tag)) {
      throw new TemplateError(`unknown block tag {{${tag}}}`);
    } else {
      target().push(parseVar(tag, false));
    }
  }
  if (stack.length) throw new TemplateError(`unclosed {{#${stack[stack.length - 1].node.kind}}}`);
  if (last < src.length) root.push({ kind: "text", value: src.slice(last) });
  return root;
}

function parseVar(expr: string, raw: boolean): Node {
  const [path, ...filters] = expr.split("|").map((s) => s.trim());
  if (!path) throw new TemplateError("empty tag");
  return { kind: "var", path, filters, raw };
}

export const defaultFilters: Record<string, Filter> = {
  upper: (v) => String(v ?? "").toUpperCase(),
  lower: (v) => String(v ?? "").toLowerCase(),
  trim: (v) => String(v ?? "").trim(),
  json: (v) => JSON.stringify(v),
  length: (v) => (v as { length?: number } | null)?.length ?? 0,
};

/** Lookup walks the scope chain from innermost outward, so loop bodies can still see outer fields. */
function lookup(scopes: unknown[], path: string): unknown {
  if (path === "this" || path === ".") return scopes[scopes.length - 1];
  const [head, ...rest] = path.split(".");
  if (head === "this") return walk(scopes[scopes.length - 1], rest); // explicit: innermost scope only
  for (let i = scopes.length - 1; i >= 0; i--) {
    const s = scopes[i];
    if (s !== null && typeof s === "object" && head in s) {
      return walk((s as Record<string, unknown>)[head], rest);
    }
  }
  return undefined;
}

function walk(v: unknown, keys: string[]): unknown {
  for (const key of keys) v = v == null ? undefined : (v as Record<string, unknown>)[key];
  return v;
}

const truthy = (v: unknown) => (Array.isArray(v) ? v.length > 0 : Boolean(v));

export function compile(src: string, filters: Record<string, Filter> = {}): (data: unknown) => string {
  const ast = parse(src);
  const fs = { ...defaultFilters, ...filters };
  for (const name of collectFilters(ast)) if (!fs[name]) throw new TemplateError(`unknown filter "${name}"`);

  const render = (nodes: Node[], scopes: unknown[]): string => {
    let out = "";
    for (const n of nodes) {
      switch (n.kind) {
        case "text":
          out += n.value;
          break;
        case "var": {
          let v = lookup(scopes, n.path);
          for (const f of n.filters) v = fs[f](v);
          out += n.raw ? String(v ?? "") : escapeHtml(v);
          break;
        }
        case "if":
          out += render(truthy(lookup(scopes, n.path)) ? n.then : n.else, scopes);
          break;
        case "each": {
          const list = lookup(scopes, n.path);
          if (!Array.isArray(list)) break;
          list.forEach((item, i) => {
            out += render(n.body, [...scopes, { "@index": i, "@first": i === 0, "@last": i === list.length - 1 }, item]);
          });
          break;
        }
      }
    }
    return out;
  };
  return (data) => render(ast, [data]);
}

function collectFilters(nodes: Node[], acc = new Set<string>()): Set<string> {
  for (const n of nodes) {
    if (n.kind === "var") n.filters.forEach((f) => acc.add(f));
    else if (n.kind === "if") collectFilters([...n.then, ...n.else], acc);
    else if (n.kind === "each") collectFilters(n.body, acc);
  }
  return acc;
}

export function render(src: string, data: unknown, filters?: Record<string, Filter>): string {
  return compile(src, filters)(data);
}
