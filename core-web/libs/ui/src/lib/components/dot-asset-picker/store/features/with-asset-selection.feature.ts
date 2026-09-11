import { patchState, signalStoreFeature, type, withMethods, withState } from '@ngrx/signals';

import { DotContentDriveBrowseItem } from '@dotcms/dotcms-models';

import { DotAssetPickerSelectionState, DotAssetPickerState } from '../models';

const initialState: DotAssetPickerSelectionState = {
    selectedAsset: null
};

/**
 * The picker returns exactly one asset, so selection is a slot, not a set — picking a second asset
 * replaces the first rather than adding to it.
 *
 * Selection is only a highlight. Remembering where an asset came from happens when the user
 * *confirms* (see `DotAssetPickerComponent.confirm`), so an exploratory click that ends in Cancel
 * doesn't move a value shared by every picker in the system.
 */
export function withAssetSelection() {
    return signalStoreFeature(
        // Declares the base state it sits on even though it reads none of it. A feature composed
        // without an input constraint erases the accumulated state for every feature AFTER it, and
        // this one is no longer last in the chain — `withAssetBrowse` needs `selectedAsset` to
        // already exist, so it runs later and would otherwise lose `config`/`path`/`filters`.
        { state: type<DotAssetPickerState>() },
        withState<DotAssetPickerSelectionState>(initialState),
        withMethods((store) => ({
            setSelectedAsset: (selectedAsset: DotContentDriveBrowseItem): void => {
                patchState(store, { selectedAsset });
            },

            clearSelection: (): void => {
                patchState(store, { selectedAsset: null });
            }
        }))
    );
}
