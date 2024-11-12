import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withState
} from '@ngrx/signals';

import { computed, inject } from '@angular/core';

import { take } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { WithFlagsState } from './models';

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
        withComputed(({ flags }) => {
            return {
                $previewMode: computed(() => {
                    const currentFlags = flags();

                    return currentFlags[FeaturedFlags.FEATURE_FLAG_UVE_PREVIEW_MODE];
                })
            };
        }),
        withHooks({
            onInit: (store) => {
                const propertiesService = inject(DotPropertiesService);

                propertiesService
                    .getFeatureFlags(flags)
                    .pipe(take(1))
                    .subscribe((flags) => {
                        patchState(store, { flags: { ...flags } });
                    });
            }
        })
    );
}
