import { signalStoreFeature, withComputed, type, withMethods, patchState } from '@ngrx/signals';

import { computed } from '@angular/core';

import { UVE_STATUS } from '../../../shared/enums';
import { UVEState } from '../../models';

/**
 * Add computed properties to the store to handle the UVE status
 *
 * @export
 * @return {*}
 */
export function withUveStatus() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withComputed((store) => ({
            isLoading: computed(() => store.status() === UVE_STATUS.LOADING),
            isLoaded: computed(() => store.status() === UVE_STATUS.LOADED),
            isError: computed(() => store.status() === UVE_STATUS.ERROR)
        })),
        withMethods((store) => ({
            setUveStatus: (status: UVE_STATUS) => patchState(store, { status })
        }))
    );
}
