import { inject, Provider } from '@angular/core';

import { DOT_FILTER_FACADE, DotFilterFacade, DotFilterValue, isNoOpFilterPatch } from '@dotcms/ui';

import { AddRelationshipsStore } from './add-relationships.store';

type Store = InstanceType<typeof AddRelationshipsStore>;

/**
 * The "Add Relationships" dialog's {@link DotFilterFacade}.
 *
 * Like the AssetPicker's, it is close to a pass-through: this dialog has no URL, so its filter bag
 * already holds the vocabulary shared chips speak and there is no encoding to translate. Content
 * Drive's facade is the one that has to encode, because its values round-trip through a URL — and
 * absorbing that difference per surface is exactly why this seam exists.
 *
 * What it adds over the store:
 *
 * - **Idempotence** (contract O9). The store resets paging on every `patchFilters`, which is right
 *   for a real change and wrong for a no-op: a chip re-emitting its current selection — a popover
 *   closing, a `linkedSignal` settling — would send the editor back to page 1 for nothing.
 * - **A closed surface** (contract O8). The relationship's target content type lives on the store's
 *   own `contentTypeId`, never in the filter bag, so no chip can read it or widen past it. There is
 *   no code path from here to it, which makes the guarantee structural rather than a convention.
 */
export function createAddRelationshipsFilterFacade(store: Store): DotFilterFacade {
    return {
        getFilterValue: (key: string) => store.getFilterValue(key),

        patchFilters: (patch: Record<string, DotFilterValue>): void => {
            // O9: a patch that changes nothing must not reset paging.
            if (isNoOpFilterPatch(patch, (key) => store.getFilterValue(key))) {
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
 * Provides {@link DOT_FILTER_FACADE} over this dialog's store.
 *
 * Add it to the **component** that provides `AddRelationshipsStore`, never to `root`: each open
 * dialog owns its own store, and a root-provided facade would reach across dialogs.
 */
export function provideAddRelationshipsFilterFacade(): Provider {
    return {
        provide: DOT_FILTER_FACADE,
        useFactory: () => createAddRelationshipsFilterFacade(inject(AddRelationshipsStore))
    };
}
