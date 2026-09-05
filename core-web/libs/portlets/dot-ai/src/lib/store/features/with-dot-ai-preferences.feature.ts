import { patchState, signalStoreFeature, type, withHooks, withMethods } from '@ngrx/signals';

import { effect, untracked } from '@angular/core';

import { readJson, writeJson } from '@dotcms/data-access';

import { DotAiPortletState } from '../../models/dot-ai-portlet.models';

/** One key, one JSON blob — unlike `withPersistedQuery` this can hold numbers and nulls. */
const PREFERENCES_KEY = 'dotcms.devtools.dotai.settings';

/** Exactly the retrieval-panel controls. Nothing else is persisted. */
type DotAiPreferences = Pick<
    DotAiPortletState,
    | 'settingsIndexName'
    | 'settingsSite'
    | 'settingsContentTypes'
    | 'settingsThreshold'
    | 'settingsOperator'
    | 'settingsModel'
    | 'settingsTemperature'
    | 'settingsResponseLength'
>;

const PREFERENCE_KEYS = [
    'settingsIndexName',
    'settingsSite',
    'settingsContentTypes',
    'settingsThreshold',
    'settingsOperator',
    'settingsModel',
    'settingsResponseLength',
    'settingsTemperature'
] as const;

const pick = (state: DotAiPortletState): DotAiPreferences =>
    PREFERENCE_KEYS.reduce((acc, key) => ({ ...acc, [key]: state[key] }), {} as DotAiPreferences);

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
                const stored = readJson<Partial<DotAiPreferences>>(PREFERENCES_KEY, {});

                if (!stored || typeof stored !== 'object') {
                    return;
                }

                // Merge over the defaults rather than replacing them: only keys we recognise,
                // and only where a value was actually stored. `settingsSite` is allowed to be
                // null, because null is a meaningful value there — it means "all sites".
                const merged = PREFERENCE_KEYS.reduce<Partial<DotAiPreferences>>((acc, key) => {
                    const value = stored[key];
                    const isSet = key === 'settingsSite' ? value !== undefined : value != null;

                    return isSet ? { ...acc, [key]: value } : acc;
                }, {});

                patchState(store, merged as Partial<DotAiPortletState>);
            }
        })),
        withHooks({
            onInit(store) {
                store.hydratePreferences();

                effect(() => {
                    // Track every persisted field, then write outside the reactive context.
                    const snapshot = pick({
                        settingsIndexName: store.settingsIndexName(),
                        settingsSite: store.settingsSite(),
                        settingsContentTypes: store.settingsContentTypes(),
                        settingsThreshold: store.settingsThreshold(),
                        settingsOperator: store.settingsOperator(),
                        settingsModel: store.settingsModel(),
                        settingsTemperature: store.settingsTemperature(),
                        settingsResponseLength: store.settingsResponseLength()
                    } as DotAiPortletState);

                    untracked(() => writeJson(PREFERENCES_KEY, snapshot));
                });
            }
        })
    );
}
