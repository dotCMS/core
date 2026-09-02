import { isReactive, isRef, toRaw } from 'vue';

/**
 * Recursively unwrap Vue reactivity (refs / reactive proxies) into a plain,
 * structured-clone-safe object.
 *
 * The UVE bridge sends data to the editor via `postMessage`, which uses the
 * structured clone algorithm — and that throws a `DataCloneError` on Vue
 * reactive Proxies. Values passed to UVE actions (`initUVE`, `editContentlet`,
 * `enableBlockEditorInline`, …) commonly come through reactive props or
 * `reactive()`, so unwrap them first. Plain values pass through untouched;
 * cycles are guarded and non-plain objects (e.g. `Date`) are preserved as-is.
 */
export function toPlain<T>(value: T): T {
    const seen = new WeakMap<object, unknown>();

    const unwrap = (input: unknown): unknown => {
        const raw = isRef(input)
            ? (input as { value: unknown }).value
            : isReactive(input)
              ? toRaw(input)
              : input;

        if (raw === null || typeof raw !== 'object') {
            return raw;
        }

        if (seen.has(raw as object)) {
            return seen.get(raw as object);
        }

        if (Array.isArray(raw)) {
            const arr: unknown[] = [];
            seen.set(raw as object, arr);
            raw.forEach((item) => arr.push(unwrap(item)));

            return arr;
        }

        // Preserve non-plain objects (Date, etc.) as-is — they clone fine.
        const proto = Object.getPrototypeOf(raw);
        if (proto !== Object.prototype && proto !== null) {
            return raw;
        }

        const out: Record<string, unknown> = {};
        seen.set(raw as object, out);
        for (const key of Object.keys(raw as Record<string, unknown>)) {
            out[key] = unwrap((raw as Record<string, unknown>)[key]);
        }

        return out;
    };

    return unwrap(value) as T;
}
