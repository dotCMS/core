import { computed, inject, Provider } from '@angular/core';

import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';
import { DOT_FILTER_FACADE, DotFilterFacade, DotFilterValue } from '@dotcms/ui';

import { DotContentDriveStore } from './dot-content-drive.store';

import { MAP_BASE_TYPES_TO_NUMBERS, MAP_NUMBERS_TO_BASE_TYPES } from '../shared/constants';
import { hasNonDefaultFilters } from '../utils/functions';

type ContentDriveStore = InstanceType<typeof DotContentDriveStore>;

/** The one filter key whose stored form differs from what chips speak. */
const BASE_TYPE_KEY = 'baseType';

/** Whether two filter values are the same selection, order included. */
const sameValue = (a?: DotFilterValue, b?: DotFilterValue): boolean => {
    if (Array.isArray(a) && Array.isArray(b)) {
        return a.length === b.length && a.every((value, index) => value === b[index]);
    }

    return a === b;
};

/** Stored numeric keys → base-type names. Unmapped keys are dropped, not passed through. */
const toBaseTypeNames = (raw: DotFilterValue | undefined): string[] => {
    const keys = Array.isArray(raw) ? raw : raw ? [raw] : [];

    return keys.map((key) => MAP_NUMBERS_TO_BASE_TYPES[Number(key)]).filter(Boolean);
};

/** Base-type names → the numeric keys the URL round-trips. Unknown names are dropped. */
const toBaseTypeKeys = (value: DotFilterValue): string[] => {
    const names = Array.isArray(value) ? value : [value];

    return names
        .map((name) => MAP_BASE_TYPES_TO_NUMBERS[name as DotCMSBaseTypesContentTypes])
        .filter((key): key is string => !!key);
};

/**
 * Content Drive's {@link DotFilterFacade}.
 *
 * Its whole job is absorbing one encoding difference so no shared chip has to know it exists: the
 * store persists base types as the **numeric keys** the drive API and the URL use, while chips
 * speak base-type **names**. That translation used to live in
 * `DotContentDriveContentTypeFilterComponent` — a per-surface adapter written once per consumer,
 * which is precisely the tax that let the AssetPicker fall behind on every chip Content Drive
 * gained. Here it is written once, below the chips.
 *
 * Everything else delegates. In particular `$hasNonDefaultFilters` reuses the existing
 * `hasNonDefaultFilters` util rather than reimplementing "what counts as a default" — the seeded
 * language and the shared-assets toggle are always present, so counting keys would offer to clear a
 * drive nobody has filtered.
 *
 * @param store The Content Drive store.
 * @return A facade over that store.
 */
export function createContentDriveFilterFacade(store: ContentDriveStore): DotFilterFacade {
    const $hasNonDefaultFilters = computed(() =>
        hasNonDefaultFilters(store.filters(), store.defaultLanguageId())
    );

    return {
        getFilterValue: (key: string) => {
            const raw = store.getFilterValue(key);

            return key === BASE_TYPE_KEY ? toBaseTypeNames(raw) : raw;
        },

        patchFilters: (patch: Record<string, DotFilterValue>): void => {
            const encoded: Record<string, DotFilterValue> = {};

            for (const [key, value] of Object.entries(patch)) {
                encoded[key] = key === BASE_TYPE_KEY ? toBaseTypeKeys(value) : value;
            }

            // O9: a patch that changes nothing must not notify or reset paging. Compared against
            // the *stored* form, after encoding, so a name→number round-trip does not read as a
            // change. The store's own `patchFilters` resets paging unconditionally, which is right
            // for its other callers.
            const changed = Object.entries(encoded).some(
                ([key, value]) => !sameValue(store.getFilterValue(key), value)
            );

            if (!changed) {
                return;
            }

            store.patchFilters(encoded);
        },

        removeFilter: (key: string): void => store.removeFilter(key),

        clearFilters: (): void => store.clearFilters(),

        $hasNonDefaultFilters
    };
}

/**
 * Provides {@link DOT_FILTER_FACADE} over the Content Drive store.
 *
 * Goes on the component that provides `DotContentDriveStore` — the portlet shell — so every chip in
 * the toolbar resolves the same instance.
 */
export function provideContentDriveFilterFacade(): Provider {
    return {
        provide: DOT_FILTER_FACADE,
        useFactory: () => createContentDriveFilterFacade(inject(DotContentDriveStore))
    };
}
