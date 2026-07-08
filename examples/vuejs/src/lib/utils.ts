import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { toRaw } from 'vue';

/**
 * Deep-unwrap a Vue reactive value into a plain, structured-clone-safe object.
 *
 * UVE editor actions (`editContentlet`, `enableBlockEditorInline`, …) send their
 * argument to the editor via `postMessage`, which uses the structured clone
 * algorithm and throws `DataCloneError` on a Vue reactive Proxy. Contentlets
 * here come from reactive props / page state, so unwrap before calling those.
 */
export function toPlain<T>(value: T): T {
    return structuredClone(toRaw(value)) as T;
}

/**
 * Merge Tailwind class names, resolving conflicts (the later class wins, e.g.
 * `cn("px-2", "px-4")` -> `"px-4"`). Combines `clsx` (conditional composition)
 * with `tailwind-merge` (conflict resolution).
 */
export function cn(...inputs: ClassValue[]): string {
    return twMerge(clsx(inputs));
}

/**
 * Coerce a free-form value (e.g. a dotCMS style property) into a known set of
 * variant keys, returning `undefined` for unknown values so a `cva` call falls
 * back to its `defaultVariants`.
 */
export function variant<T extends string>(value: unknown, allowed: readonly T[]): T | undefined {
    return allowed.includes(value as T) ? (value as T) : undefined;
}
