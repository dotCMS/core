import { patchState, signalStoreFeature, type, withHooks, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed } from '@angular/core';

import { debounceTime, skip, tap } from 'rxjs/operators';

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
 * - Watches `state[field]` via `rxMethod` and writes changes to localStorage,
 *   debounced by `debounceMs` (default 300 ms). The first emission after
 *   hydration is skipped so we don't rewrite the value we just read.
 * - Empty values are treated as "clear" — after the debounce we call
 *   `removeKey` rather than `writeJson('""')`, so a `clearPersistedQuery`
 *   call followed by no typing leaves storage empty (previously a race
 *   re-created the entry as `""` once the debounce elapsed).
 * - Adds a `clearPersistedQuery()` method that resets `state[field]` to an
 *   empty string and removes the stored entry synchronously.
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
             * storage entry synchronously. The rxMethod pipeline handles
             * the follow-up emission by calling `removeKey` again after the
             * debounce (no-op — key is already gone).
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

                // Bind the debounced persistence pipeline to the tracked
                // field signal. `skip(1)` drops the hydrated value so we
                // don't immediately write it back; `debounceTime` coalesces
                // rapid typing; empty values route to `removeKey` to avoid
                // resurrecting a `""` entry after `clearPersistedQuery`.
                const source = computed(() =>
                    (store as unknown as Record<Field, () => string>)[field]()
                );
                rxMethod<string>(
                    pipe(
                        skip(1),
                        debounceTime(debounceMs),
                        tap((value) => {
                            if (value.length === 0) {
                                removeKey(storageKey);
                            } else {
                                writeJson(storageKey, value);
                            }
                        })
                    )
                )(source);
            }
        })
    );
}
