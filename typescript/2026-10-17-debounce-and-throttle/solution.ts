// debounce: run only after calls stop for `wait` ms.
// throttle: run at most once per `wait` ms (leading call + one trailing call).

type AnyFn = (...args: any[]) => void;

export interface Cancelable<F extends AnyFn> {
  (...args: Parameters<F>): void;
  cancel(): void; // drop any pending call
  flush(): void; // run the pending call now, if any
}

export function debounce<F extends AnyFn>(
  fn: F,
  wait: number,
  { leading = false }: { leading?: boolean } = {},
): Cancelable<F> {
  let timer: ReturnType<typeof setTimeout> | undefined;
  let pendingArgs: Parameters<F> | undefined;

  const run = () => {
    const args = pendingArgs!;
    pendingArgs = undefined;
    fn(...args);
  };

  const debounced = ((...args: Parameters<F>) => {
    const isQuiet = timer === undefined;
    if (timer !== undefined) clearTimeout(timer);
    if (leading && isQuiet) {
      fn(...args); // fire immediately at the start of a burst
    } else {
      pendingArgs = args;
    }
    timer = setTimeout(() => {
      timer = undefined;
      if (pendingArgs) run();
    }, wait);
  }) as Cancelable<F>;

  debounced.cancel = () => {
    if (timer !== undefined) clearTimeout(timer);
    timer = undefined;
    pendingArgs = undefined;
  };
  debounced.flush = () => {
    if (timer !== undefined) clearTimeout(timer);
    timer = undefined;
    if (pendingArgs) run();
  };
  return debounced;
}

export function throttle<F extends AnyFn>(fn: F, wait: number): Cancelable<F> {
  let timer: ReturnType<typeof setTimeout> | undefined;
  let pendingArgs: Parameters<F> | undefined;

  // After each run, open a cool-down window; if calls arrived during it,
  // run the latest one when it closes and start a new window.
  const startCooldown = () => {
    timer = setTimeout(() => {
      timer = undefined;
      if (pendingArgs) {
        const args = pendingArgs;
        pendingArgs = undefined;
        fn(...args);
        startCooldown();
      }
    }, wait);
  };

  const throttled = ((...args: Parameters<F>) => {
    if (timer === undefined) {
      fn(...args);
      startCooldown();
    } else {
      pendingArgs = args; // keep only the latest
    }
  }) as Cancelable<F>;

  throttled.cancel = () => {
    if (timer !== undefined) clearTimeout(timer);
    timer = undefined;
    pendingArgs = undefined;
  };
  throttled.flush = () => {
    if (pendingArgs) {
      const args = pendingArgs;
      pendingArgs = undefined;
      fn(...args);
    }
  };
  return throttled;
}
