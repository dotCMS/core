export const DEBOUNCE_FOR_TRACKING = <T>(func: (...args: T[]) => void, delay: number) => {
    let timeout: ReturnType<typeof setTimeout>;

    return function (...args: T[]) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), delay);
    };
};

export const TRACKING_DELAY = 5000;
