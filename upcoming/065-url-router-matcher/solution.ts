// A path router backed by a segment trie. Supports static segments,
// named params (":id"), and a trailing catch-all ("*rest").
// Priority at each level: static > param > wildcard, with backtracking.

export interface Match<H> {
  handler: H;
  params: Record<string, string>;
}

class Node<H> {
  static = new Map<string, Node<H>>();
  param?: { name: string; node: Node<H> };
  wildcard?: { name: string; handler: H };
  handler?: H;
}

const split = (path: string) => path.split("/").filter((s) => s.length > 0);

export class Router<H> {
  private roots = new Map<string, Node<H>>(); // one trie per HTTP method

  add(method: string, pattern: string, handler: H): this {
    let root = this.roots.get(method);
    if (!root) this.roots.set(method, (root = new Node()));
    let node = root;
    const segs = split(pattern);
    segs.forEach((seg, i) => {
      if (seg.startsWith("*")) {
        if (i !== segs.length - 1) throw new Error("wildcard must be the last segment");
        if (node.wildcard) throw new Error(`duplicate route ${method} ${pattern}`);
        node.wildcard = { name: seg.slice(1) || "*", handler };
        return;
      }
      if (seg.startsWith(":")) {
        const name = seg.slice(1);
        if (!name) throw new Error("param needs a name");
        if (node.param && node.param.name !== name)
          throw new Error(`conflicting param names :${node.param.name} and :${name}`);
        node.param ??= { name, node: new Node() };
        node = node.param.node;
      } else {
        let next = node.static.get(seg);
        if (!next) node.static.set(seg, (next = new Node()));
        node = next;
      }
      if (i === segs.length - 1) {
        if (node.handler !== undefined) throw new Error(`duplicate route ${method} ${pattern}`);
        node.handler = handler;
      }
    });
    if (segs.length === 0) {
      if (node.handler !== undefined) throw new Error(`duplicate route ${method} ${pattern}`);
      node.handler = handler;
    }
    return this;
  }

  match(method: string, path: string): Match<H> | undefined {
    const root = this.roots.get(method);
    if (!root) return undefined;
    const segs = split(path.split("?")[0]).map((s) => {
      try {
        return decodeURIComponent(s);
      } catch {
        return s; // leave malformed escapes as-is
      }
    });
    const params: Record<string, string> = {};

    const walk = (node: Node<H>, i: number): H | undefined => {
      if (i === segs.length) {
        if (node.handler !== undefined) return node.handler;
        if (node.wildcard) {
          params[node.wildcard.name] = ""; // a catch-all also matches zero segments
          return node.wildcard.handler;
        }
        return undefined;
      }
      const s = node.static.get(segs[i]);
      if (s) {
        const h = walk(s, i + 1);
        if (h !== undefined) return h;
      }
      if (node.param) {
        params[node.param.name] = segs[i];
        const h = walk(node.param.node, i + 1);
        if (h !== undefined) return h;
        delete params[node.param.name]; // backtrack
      }
      if (node.wildcard) {
        params[node.wildcard.name] = segs.slice(i).join("/");
        return node.wildcard.handler;
      }
      return undefined;
    };

    const handler = walk(root, 0);
    return handler === undefined ? undefined : { handler, params };
  }

  /** Methods that have a route for this path (for 405 responses / Allow header). */
  allowedMethods(path: string): string[] {
    return [...this.roots.keys()].filter((m) => this.match(m, path) !== undefined);
  }
}
