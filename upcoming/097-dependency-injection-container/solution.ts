// A small, type-safe dependency injection container.
// - Typed tokens tie a key to the type it resolves to, so no casts are needed.
// - Lifetimes: singleton (one per root), scoped (one per child scope), transient (new each time).
// - Detects circular dependencies and reports the full resolution chain.

export class Token<T> {
  // Phantom field so Token<A> and Token<B> are not interchangeable.
  declare readonly __type: T;
  constructor(readonly name: string) {}
  toString(): string {
    return `Token(${this.name})`;
  }
}

export type Lifetime = "singleton" | "scoped" | "transient";
export type Factory<T> = (c: Container) => T;

interface Provider<T> {
  factory: Factory<T>;
  lifetime: Lifetime;
}

export class CircularDependencyError extends Error {
  constructor(readonly chain: string[]) {
    super(`circular dependency: ${chain.join(" -> ")}`);
  }
}

export class Container {
  private constructor(
    private readonly root: Container | null,
    private readonly providers: Map<Token<unknown>, Provider<unknown>>,
    private readonly resolving: Token<unknown>[] = [],
    private readonly instances = new Map<Token<unknown>, unknown>(),
    private readonly disposers: (() => void)[] = [],
  ) {}

  static create(): Container {
    return new Container(null, new Map());
  }

  register<T>(token: Token<T>, factory: Factory<T>, lifetime: Lifetime = "transient"): this {
    if (this.root) throw new Error("register on the root container");
    this.providers.set(token, { factory, lifetime } as Provider<unknown>);
    return this;
  }

  singleton<T>(token: Token<T>, factory: Factory<T>): this {
    return this.register(token, factory, "singleton");
  }

  scoped<T>(token: Token<T>, factory: Factory<T>): this {
    return this.register(token, factory, "scoped");
  }

  value<T>(token: Token<T>, value: T): this {
    return this.register(token, () => value, "singleton");
  }

  has(token: Token<unknown>): boolean {
    return this.providers.has(token);
  }

  /** Child scope: shares registrations and singletons, owns its scoped instances. */
  createScope(): Container {
    return new Container(this.root ?? this, this.providers);
  }

  resolve<T>(token: Token<T>): T {
    const provider = this.providers.get(token) as Provider<T> | undefined;
    if (!provider) throw new Error(`no provider for ${token.name}`);

    if (provider.lifetime === "scoped" && !this.root) {
      throw new Error(`scoped ${token.name} resolved from root; use createScope()`);
    }
    // Where a cached instance lives depends on its lifetime.
    const owner = provider.lifetime === "singleton" ? (this.root ?? this) : this;
    if (provider.lifetime !== "transient" && owner.instances.has(token)) {
      return owner.instances.get(token) as T;
    }

    if (this.resolving.includes(token)) {
      const chain = [...this.resolving, token].map((t) => t.name);
      throw new CircularDependencyError(chain.slice(chain.indexOf(token.name)));
    }
    this.resolving.push(token);
    let instance: T;
    try {
      // Singletons resolve their deps through the root so they never capture scoped instances.
      const ctx = provider.lifetime === "singleton" && this.root ? this.root.withResolving(this.resolving) : this;
      instance = provider.factory(ctx);
    } finally {
      this.resolving.pop();
    }
    if (provider.lifetime !== "transient") {
      owner.instances.set(token, instance);
      const d = (instance as { dispose?: () => void } | null)?.dispose;
      if (typeof d === "function") owner.disposers.push(() => d.call(instance));
    }
    return instance;
  }

  /** A root-level view that shares the root's cache but continues the caller's cycle-detection stack. */
  private withResolving(resolving: Token<unknown>[]): Container {
    return new Container(null, this.providers, resolving, this.instances, this.disposers);
  }

  /** Disposes cached instances owned by this container, in reverse creation order. */
  dispose(): void {
    while (this.disposers.length) this.disposers.pop()!();
    this.instances.clear();
  }
}
