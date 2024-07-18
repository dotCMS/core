import { signalStoreFeature, type, withState, withMethods, patchState } from '@ngrx/signals';

import { DotDevice } from '@dotcms/dotcms-models';

import { EditorToolbarState, UVEState } from '../../../models';

const initialState: EditorToolbarState = {
    device: undefined,
    socialMedia: undefined
};

/**
 * Add computed properties to the store to handle the UVE status
 *
 * @export
 * @return {*}
 */
export function withEditorToolbar() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withState<EditorToolbarState>(initialState),
        withMethods((store) => {
            return {
                setDevice: (device: DotDevice) => {
                    patchState(store, { device, socialMedia: undefined });
                },
                setSocialMedia: (socialMedia: string) => {
                    patchState(store, { socialMedia, device: undefined });
                },
                clearDeviceAndSocialMedia: () => {
                    patchState(store, initialState);
                }
            };
        })
    );
}
