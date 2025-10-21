import {
    patchState,
    signalStoreFeature,
    type,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { inject } from '@angular/core';

import { take } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { UVEFlags, WithFlagsState } from './models';

import { UVEState } from '../../models';

/**
 *
 * @description This feature is used to handle the fetch of flags
 * @export
 * @return {*}
 */
export function withFlags(flags: FeaturedFlags[]) {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withState<WithFlagsState>({ flags: {} }),
        withMethods((store) => ({
            setFlags: (flags: UVEFlags) => {
                patchState(store, { flags: { ...flags } });
            }
        })),
        withHooks({
            onInit: (store) => {
                const propertiesService = inject(DotPropertiesService);

                propertiesService
                    .getFeatureFlags(flags)
                    .pipe(take(1))
                    .subscribe((flags) => {
                        store.setFlags(flags);
                    });
            }
        })
    );
}
