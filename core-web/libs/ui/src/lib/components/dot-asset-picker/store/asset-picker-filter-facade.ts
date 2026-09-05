import { inject, Provider } from '@angular/core';

import { DotAssetPickerStore } from './dot-asset-picker.store';

import {
    DOT_FILTER_FACADE,
    DotFilterFacade,
    DotFilterValue
} from '../../dot-filter-bar/filter-facade.token';

type AssetPickerStore = InstanceType<typeof DotAssetPickerStore>;

/** Whether two filter values are the same selection, order included. */
const sameValue = (a?: DotFilterValue, b?: DotFilterValue): boolean => {
    if (Array.isArray(a) && Array.isArray(b)) {
        return a.length === b.length && a.every((value, index) => value === b[index]);
    }

    return a === b;
};

/**
 * The AssetPicker's {@link DotFilterFacade}.
 *
 * Almost a pass-through, and deliberately so: the picker stores exactly the vocabulary shared chips
 * speak, so there is no encoding to translate. Base types are held by **name** here, unlike Content
 * Drive, which encodes them as the numeric keys its URL needs — that difference is the whole reason
 * this seam exists, and absorbing it per surface is what lets one chip drive both.
 *
 * Two things it does add over the store:
 *
 * - **Idempotence** (contract O9). The store resets paging on every `patchFilters`, which is right
 *   for a real change and wrong for a no-op: a chip that re-emits its current selection — a popover
 *   closing, a `linkedSignal` settling — would bounce the editor back to page 1 for nothing. The
 *   guard lives here rather than in the store so the store's own callers keep their unconditional
 *   semantics.
 * - **A closed surface** (contract O8). `mimeTypes`, `allowedBaseTypes` and the version-state pin
 *   live on `config`, never in the filter bag, so no chip can read or widen past them. That is
 *   structural rather than a convention: there is no code path from here to `config`.
 *
 * @param store The picker's store, provided per dialog instance.
 * @return A facade over that store.
 */
export function createAssetPickerFilterFacade(store: AssetPickerStore): DotFilterFacade {
    return {
        getFilterValue: (key: string) => store.getFilterValue(key),

        patchFilters: (patch: Record<string, DotFilterValue>): void => {
            // O9: a patch that changes nothing must not notify or reset paging.
            const changed = Object.entries(patch).some(
                ([key, value]) => !sameValue(store.getFilterValue(key), value)
            );

            if (!changed) {
                return;
            }

            store.patchFilters(patch);
        },

        removeFilter: (key: string): void => store.removeFilter(key),

        clearFilters: (): void => store.clearFilters(),

        $hasNonDefaultFilters: store.$hasNonDefaultFilters
    };
}

/**
 * Provides {@link DOT_FILTER_FACADE} over the picker's store.
 *
 * Add it to the **component** that provides `DotAssetPickerStore`, never to `root`: each open
 * picker owns its own store, and a root-provided facade would reach across dialogs.
 */
export function provideAssetPickerFilterFacade(): Provider {
    return {
        provide: DOT_FILTER_FACADE,
        useFactory: () => createAssetPickerFilterFacade(inject(DotAssetPickerStore))
    };
}
