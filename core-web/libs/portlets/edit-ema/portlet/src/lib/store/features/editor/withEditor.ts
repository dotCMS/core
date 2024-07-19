import { signalStoreFeature, type } from '@ngrx/signals';

import { withEditorToolbar } from './toolbar/withEditorToolbar';

import { UVEState } from '../../models';

/**
 * Add computed properties to the store to handle the UVE status
 *
 * @export
 * @return {*}
 */
export function withEditor() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withEditorToolbar()
    );
}
