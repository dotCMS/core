import { computed, inject, Provider } from '@angular/core';

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
 * - **The reload.** The store's `patchFilters` and `removeFilter` only record the change; they do
 *   not fetch, because `onSearch` patches and loads in one go and a load inside them would make it
 *   search twice. A chip has no such second step, so the reload belongs here — without it the
 *   Locale chip recorded a filter, reset paging, and left the previous locale's rows on screen
 *   until some unrelated control happened to fire a search.
 * - **Idempotence** (contract O9). The store resets paging on every `patchFilters`, which is right
 *   for a real change and wrong for a no-op: a chip re-emitting its current selection — a popover
 *   closing, a `linkedSignal` settling — would send the editor back to page 1 for nothing. Now that
 *   a patch also reloads, the guard saves a request as well as the page.
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
            store.load();
        },

        // Same reload as `patchFilters`: clearing a chip is a filter change like any other, and a
        // chip that changes the bag without changing the results is a control that does nothing.
        // Same reload as `patchFilters`: clearing a chip is a filter change like any other, and a
        // chip that changes the bag without changing the results is a control that does nothing.
        removeFilter: (key: string): void => {
            store.removeFilter(key);
            store.load();
        },

        // "Clear all" here means the browsed site too, not just the chips — see `reset()`. The
        // wider clear is what keeps obligation O5 true once scope counts below: the bar asserts
        // that after clearing there is nothing left worth clearing.
        clearFilters: (): void => store.reset(),

        /**
         * Extended beyond the filter bag, deliberately.
         *
         * The contract asks whether anything differs from *this surface's defaults* — not whether a
         * filter does. The browsed site is one of this surface's defaults: it opens on the
         * contentlet's own site, and moving off it is a change the editor made and may want undone.
         * Keeping it out would mean browsing into an empty site and being offered no way back,
         * which is the one case where the editor is actually stuck.
         *
         * Content Drive answers differently because its browsed folder lives in the URL and its
         * tree is always on screen. Absorbing that difference per surface is exactly why this seam
         * exists.
         */
        $hasNonDefaultFilters: computed(
            () => store.$hasNonDefaultFilters() || store.assetPath() !== store.defaultAssetPath()
        )
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
