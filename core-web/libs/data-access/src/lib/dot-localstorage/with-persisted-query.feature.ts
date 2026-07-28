import { patchState, signalStoreFeature, type, withHooks, withMethods } from '@ngrx/signals';

import { effect } from '@angular/core';

import {
    buildPersistedQueryKey,
    readJson,
    removeKey,
    writeJson
} from './dot-persisted-query.utils';

/** Default debounce for localStorage writes while the user is typing. */
const DEFAULT_DEBOUNCE_MS = 300;

export interface PersistedQueryConfig<Field extends string> {
    /**
     * Short portlet slug used in the storage key
     * (`dotcms.devtools.{portletKey}.lastQuery`).
     *
     * Example: `'query-tool'`, `'es-search'`, `'velocity-playground'`.
     */
    portletKey: string;

    /**
     * Name of the store state property holding the user's query string.
     * Must be a `string` field on the parent store state.
     */
    field: Field;

    /**
     * Milliseconds to wait after the last change before writing to
     * localStorage. Defaults to {@link DEFAULT_DEBOUNCE_MS} (300ms).
     */
    debounceMs?: number;
}

/**
 * SignalStore feature that persists a single query-string field to
 * `localStorage` and restores it on store init.
 *
 * Behavior:
 * - On init, reads `dotcms.devtools.{portletKey}.lastQuery` and patches
 *   `state[field]` if a non-empty stored value exists.
 * - Watches `state[field]` and writes changes to localStorage, debounced by
 *   `debounceMs` (default 300ms). The first tick after hydration is skipped
 *   to avoid a redundant write of the freshly-hydrated value.
 * - Adds a `clearPersistedQuery()` method that resets `state[field]` to an
 *   empty string and removes the stored entry.
 *
 * Composition:
 * ```ts
 * export const MyStore = signalStore(
 *     withState<{ query: string }>({ query: '' }),
 *     withPersistedQuery({ portletKey: 'query-tool', field: 'query' }),
 *     // ...other features
 * );
 * ```
 *
 * The parent state must declare `field` as a `string` — the `type<>()`
 * constraint enforces this at compile time.
 */
export function withPersistedQuery<Field extends string>(config: PersistedQueryConfig<Field>) {
    const storageKey = buildPersistedQueryKey(config.portletKey);
    const debounceMs = config.debounceMs ?? DEFAULT_DEBOUNCE_MS;
    const field = config.field;

    // Runtime patch helper — the Record cast satisfies patchState's shape check
    // for a dynamic field name; the outer generic already guarantees Field is
    // a string-typed key on the parent state.
    const patchField = (store: unknown, value: string): void => {
        patchState(
            store as Parameters<typeof patchState>[0],
            {
                [field]: value
            } as Record<Field, string>
        );
    };

    return signalStoreFeature(
        { state: type<Record<Field, string>>() },
        withMethods((store) => ({
            /**
             * Reset the persisted query to an empty string and delete the
             * localStorage entry. The debounced-write effect will observe the
             * empty value but the removeKey call has already cleared storage.
             */
            clearPersistedQuery(): void {
                patchField(store, '');
                removeKey(storageKey);
            }
        })),
        withHooks({
            onInit(store) {
                const stored = readJson<string>(storageKey, '');
                if (typeof stored === 'string' && stored.length > 0) {
                    patchField(store, stored);
                }

                // Skip the first effect tick (post-hydration): the value we'd
                // write is the value we just read.
                let hydrated = false;
                const readField = () => (store as unknown as Record<Field, () => string>)[field]();

                effect((onCleanup) => {
                    const value = readField();
                    if (!hydrated) {
                        hydrated = true;

                        return;
                    }

                    const timer = setTimeout(() => writeJson(storageKey, value), debounceMs);
                    onCleanup(() => clearTimeout(timer));
                });
            }
        })
    );
}
