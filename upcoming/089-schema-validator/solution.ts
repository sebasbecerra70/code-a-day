// A tiny zod-style schema validator. Schemas are composable objects that
// validate unknown input at runtime, report every error with its path, and
// carry the inferred static type via `Infer<typeof schema>`.

export interface Issue {
  path: (string | number)[];
  message: string;
}

export type Result<T> = { ok: true; value: T } | { ok: false; issues: Issue[] };

export class ValidationError extends Error {
  constructor(readonly issues: Issue[]) {
    super(issues.map((i) => `${i.path.length ? i.path.join(".") : "(root)"}: ${i.message}`).join("; "));
  }
}

export abstract class Schema<T> {
  /** Validates `input`, pushing problems onto `issues`. Returns the (possibly transformed) value. */
  abstract check(input: unknown, path: (string | number)[], issues: Issue[]): T;

  safeParse(input: unknown): Result<T> {
    const issues: Issue[] = [];
    const value = this.check(input, [], issues);
    return issues.length ? { ok: false, issues } : { ok: true, value };
  }

  parse(input: unknown): T {
    const r = this.safeParse(input);
    if (!r.ok) throw new ValidationError(r.issues);
    return r.value;
  }

  optional(): Schema<T | undefined> {
    return new OptionalSchema(this);
  }

  /** Adds a custom predicate; runs only if the base schema passed. */
  refine(pred: (v: T) => boolean, message: string): Schema<T> {
    return new RefineSchema(this, pred, message);
  }
}

export type Infer<S> = S extends Schema<infer T> ? T : never;

const typeName = (v: unknown): string => (v === null ? "null" : Array.isArray(v) ? "array" : typeof v);

class PrimitiveSchema<T> extends Schema<T> {
  constructor(private readonly name: "string" | "number" | "boolean") {
    super();
  }
  check(input: unknown, path: (string | number)[], issues: Issue[]): T {
    if (typeof input !== this.name || (this.name === "number" && Number.isNaN(input))) {
      issues.push({ path, message: `expected ${this.name}, got ${typeName(input)}` });
    }
    return input as T;
  }
}

class LiteralSchema<T extends string | number | boolean | null> extends Schema<T> {
  constructor(private readonly lit: T) {
    super();
  }
  check(input: unknown, path: (string | number)[], issues: Issue[]): T {
    if (input !== this.lit) issues.push({ path, message: `expected ${JSON.stringify(this.lit)}` });
    return input as T;
  }
}

class OptionalSchema<T> extends Schema<T | undefined> {
  constructor(readonly inner: Schema<T>) {
    super();
  }
  check(input: unknown, path: (string | number)[], issues: Issue[]): T | undefined {
    return input === undefined ? undefined : this.inner.check(input, path, issues);
  }
}

class RefineSchema<T> extends Schema<T> {
  constructor(
    private readonly inner: Schema<T>,
    private readonly pred: (v: T) => boolean,
    private readonly message: string,
  ) {
    super();
  }
  check(input: unknown, path: (string | number)[], issues: Issue[]): T {
    const before = issues.length;
    const v = this.inner.check(input, path, issues);
    if (issues.length === before && !this.pred(v)) issues.push({ path, message: this.message });
    return v;
  }
}

class ArraySchema<T> extends Schema<T[]> {
  constructor(private readonly item: Schema<T>) {
    super();
  }
  check(input: unknown, path: (string | number)[], issues: Issue[]): T[] {
    if (!Array.isArray(input)) {
      issues.push({ path, message: `expected array, got ${typeName(input)}` });
      return input as T[];
    }
    return input.map((x, i) => this.item.check(x, [...path, i], issues));
  }
}

type Shape = Record<string, Schema<unknown>>;

// Keys whose schema accepts undefined become optional properties.
type OptionalKeys<S extends Shape> = { [K in keyof S]: undefined extends Infer<S[K]> ? K : never }[keyof S];
type Flatten<T> = { [K in keyof T]: T[K] };
export type InferShape<S extends Shape> = Flatten<
  { [K in Exclude<keyof S, OptionalKeys<S>>]: Infer<S[K]> } & { [K in OptionalKeys<S>]?: Infer<S[K]> }
>;

class ObjectSchema<S extends Shape> extends Schema<InferShape<S>> {
  constructor(private readonly shape: S, private readonly strictKeys = false) {
    super();
  }
  /** Rejects keys not in the shape (default is to strip them). */
  strict(): ObjectSchema<S> {
    return new ObjectSchema(this.shape, true);
  }
  check(input: unknown, path: (string | number)[], issues: Issue[]): InferShape<S> {
    if (typeof input !== "object" || input === null || Array.isArray(input)) {
      issues.push({ path, message: `expected object, got ${typeName(input)}` });
      return input as InferShape<S>;
    }
    const src = input as Record<string, unknown>;
    const out: Record<string, unknown> = {};
    for (const [key, schema] of Object.entries(this.shape)) {
      const v = schema.check(src[key], [...path, key], issues);
      if (v !== undefined) out[key] = v;
    }
    if (this.strictKeys) {
      for (const key of Object.keys(src)) {
        if (!(key in this.shape)) issues.push({ path: [...path, key], message: "unexpected key" });
      }
    }
    return out as InferShape<S>;
  }
}

class UnionSchema<T> extends Schema<T> {
  constructor(private readonly options: Schema<T>[]) {
    super();
  }
  check(input: unknown, path: (string | number)[], issues: Issue[]): T {
    for (const opt of this.options) {
      const local: Issue[] = [];
      const v = opt.check(input, path, local);
      if (local.length === 0) return v;
    }
    issues.push({ path, message: "no union member matched" });
    return input as T;
  }
}

export const s = {
  string: () => new PrimitiveSchema<string>("string"),
  number: () => new PrimitiveSchema<number>("number"),
  boolean: () => new PrimitiveSchema<boolean>("boolean"),
  literal: <T extends string | number | boolean | null>(v: T) => new LiteralSchema(v),
  array: <T>(item: Schema<T>) => new ArraySchema(item),
  object: <S extends Shape>(shape: S) => new ObjectSchema(shape),
  union: <O extends Schema<unknown>[]>(...options: O) => new UnionSchema<Infer<O[number]>>(options as Schema<Infer<O[number]>>[]),
};
