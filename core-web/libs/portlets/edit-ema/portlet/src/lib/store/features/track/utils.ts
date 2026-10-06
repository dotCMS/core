export const DEBOUNCE_FOR_TRACKING = <T>(func: (...args: T[]) => void, delay: number) => {
    let timeout: ReturnType<typeof setTimeout>;

    // `this: unknown` declares what the `apply` below forwards: a plain function expression takes
    // its `this` from the call site, and the debounced wrapper passes it straight through.
    return function (this: unknown, ...args: T[]) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), delay);
    };
};

export const TRACKING_DELAY = 5000;
