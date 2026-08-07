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
 * Kept as its own feature because AssetPicker 6/7 hangs the global last-used-path bookkeeping off
 * the moment an asset is chosen.
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
