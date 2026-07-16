/**
 * Callback for a fulfilled promise.
 *
 * @template T - The type of the response.
 * @callback ThenableCallback
 * @param {T} value - The response value.
 * @returns {T | PromiseLike<T> | void} The processed response or a promise.
 */
export type ThenableCallback<T> = ((value: T) => T | PromiseLike<T> | void) | undefined | null;
