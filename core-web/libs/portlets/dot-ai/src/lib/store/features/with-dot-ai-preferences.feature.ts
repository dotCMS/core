import { patchState, signalStoreFeature, type, withHooks, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { Observable } from 'rxjs';

import { computed } from '@angular/core';

import { debounceTime, skip, tap } from 'rxjs/operators';

import { readJson, writeJson } from '@dotcms/data-access';
import { DOT_AI_VECTOR_OPERATOR } from '@dotcms/dotcms-models';

import {
    DOT_AI_SETTINGS_KEYS,
    DotAiPortletState,
    DotAiRetrievalSettings
} from '../../models/dot-ai-portlet.models';

/** One key, one JSON blob — unlike `withPersistedQuery` this can hold numbers and nulls. */
const PREFERENCES_KEY = 'dotcms.devtools.dotai.settings';

/**
 * Bumped whenever a default changes in a way a stored value would otherwise mask.
 *
 * Without this, changing a default only reaches people who have never opened the portlet:
 * everyone else has the old value persisted, and merging it back pins them to it forever.
 *
 * v2 re-defaults `settingsThreshold`. Blobs written at v1 hold 0.25, which is tight enough
 * that ordinary questions retrieve nothing and the answer is "no matching content found in the
 * index for your query". Only that key is dropped — every other stored control survives, so
 * this is a targeted re-default, not a reset of the panel.
 */
const PREFERENCES_VERSION = 2;

/** Matches `withPersistedQuery`'s cadence. */
const PERSIST_DEBOUNCE_MS = 300;

/** Keys a stored blob gives up when it predates the current version. */
const RE_DEFAULTED_KEYS: Record<number, ReadonlySet<string>> = {
    2: new Set(['settingsThreshold'])
};

/**
 * Exactly the retrieval-panel controls. Nothing else is persisted.
 *
 * Shared with `setSettings` rather than restated, so the set that can be written and the set
 * that is remembered cannot drift apart.
 */
const PREFERENCE_KEYS = DOT_AI_SETTINGS_KEYS;

type DotAiPreferences = DotAiRetrievalSettings;

const OPERATORS: ReadonlySet<string> = new Set(Object.values(DOT_AI_VECTOR_OPERATOR));

/**
 * Whether a stored value is usable for the key it was stored under.
 *
 * localStorage is a trust boundary: the blob can be stale, hand-edited, or written by an
 * older build, and whatever survives here is patched straight into typed state and then
 * assembled into a request body. The numerics are additionally clamped downstream in
 * `withRetrievalSettings`, but the operator has no such guard — an unrecognised one would
 * reach the wire and the server would reject the whole search.
 */
const isUsable = (key: keyof DotAiPreferences, value: unknown): boolean => {
    switch (key) {
        case 'settingsThreshold':
        case 'settingsTemperature':
        case 'settingsResponseLength':
            return typeof value === 'number' && Number.isFinite(value);
        case 'settingsOperator':
            return typeof value === 'string' && OPERATORS.has(value);
        case 'settingsSite':
            // null is meaningful here: it means "all sites".
            return value === null || typeof value === 'string';
        default:
            return typeof value === 'string';
    }
};

/**
 * Persists the retrieval-settings panel between visits.
 *
 * `withPersistedQuery` cannot do this — it holds a single string field and can only be
 * composed once, since it contributes a `clearPersistedQuery()` method a second instance
 * would collide on. This writes one JSON blob under its own key instead, which can carry
 * numbers and nulls.
 *
 * **Merges over defaults, never replaces.** A blob written months ago may name a model the
 * provider no longer offers or an index that has since been deleted; merging means an unknown
 * key is simply ignored rather than pinning the panel to something unavailable (FR-018). The
 * index and model are additionally re-seeded by `withAiConfig` / `withAiIndexes` when what was
 * stored is no longer on offer.
 *
 * Deliberately a **new** key rather than the legacy `com.dotcms.ai.settings` blob: three of
 * that blob's ten fields describe DOM that no longer exists, and its `searchQuery` served both
 * Search and Chat, which are now separate prompts. The old entry is left untouched for the
 * legacy screen.
 */
export function withDotAiPreferences() {
    return signalStoreFeature(
        type<{ state: DotAiPortletState }>(),
        withMethods((store) => ({
            hydratePreferences(): void {
                const stored = readJson<Partial<DotAiPreferences> & { version?: number }>(
                    PREFERENCES_KEY,
                    {}
                );

                if (!stored || typeof stored !== 'object') {
                    return;
                }

                // A blob from an older version gives up the keys that version re-defaulted,
                // so the new default is what the panel actually shows.
                // Union every version newer than the blob's own, so a v1 blob upgrading
                // straight to v3 still gives up what v2 re-defaulted. Indexing a single
                // version would also throw on the next bump, because a version with no entry
                // yields undefined and `.has` blows up inside onInit.
                const storedVersion = stored.version ?? 0;
                const reDefaulted = new Set<string>(
                    Object.entries(RE_DEFAULTED_KEYS)
                        .filter(([version]) => Number(version) > storedVersion)
                        .flatMap(([, keys]) => [...keys])
                );

                // Merge over the defaults rather than replacing them: only keys we recognise,
                // and only where the stored value is usable for that key. Anything else is
                // dropped, so the default shows through rather than a bad value reaching the
                // request body.
                const merged = PREFERENCE_KEYS.reduce<Partial<DotAiPreferences>>((acc, key) => {
                    if (reDefaulted.has(key)) {
                        return acc;
                    }

                    const value = stored[key];
                    const isSet = key === 'settingsSite' ? value !== undefined : value != null;

                    return isSet && isUsable(key, value) ? { ...acc, [key]: value } : acc;
                }, {});

                patchState(store, merged as Partial<DotAiPortletState>);
            }
        })),
        withHooks({
            onInit(store) {
                store.hydratePreferences();

                const snapshot = computed<DotAiPreferences>(() =>
                    PREFERENCE_KEYS.reduce(
                        (acc, key) => ({ ...acc, [key]: store[key]() }),
                        {} as DotAiPreferences
                    )
                );

                // Debounced, like `withPersistedQuery`: the panel writes straight into the
                // store on every keystroke and number-spinner tick, and each write is a
                // synchronous localStorage round trip on the typing path. `skip(1)` drops the
                // value just hydrated instead of writing it straight back.
                rxMethod<DotAiPreferences>((source$: Observable<DotAiPreferences>) =>
                    source$.pipe(
                        skip(1),
                        debounceTime(PERSIST_DEBOUNCE_MS),
                        tap((value: DotAiPreferences) =>
                            writeJson(PREFERENCES_KEY, {
                                ...value,
                                version: PREFERENCES_VERSION
                            })
                        )
                    )
                )(snapshot);
            }
        })
    );
}
