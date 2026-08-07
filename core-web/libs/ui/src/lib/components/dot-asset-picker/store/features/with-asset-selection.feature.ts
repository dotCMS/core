import { patchState, signalStoreFeature, withMethods, withState } from '@ngrx/signals';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotAssetPickerSelectionState } from '../models';

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
        withState<DotAssetPickerSelectionState>(initialState),
        withMethods((store) => ({
            setSelectedAsset: (selectedAsset: DotCMSContentlet): void => {
                patchState(store, { selectedAsset });
            },

            clearSelection: (): void => {
                patchState(store, { selectedAsset: null });
            }
        }))
    );
}
