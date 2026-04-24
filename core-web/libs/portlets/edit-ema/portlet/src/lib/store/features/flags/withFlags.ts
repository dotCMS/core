import { patchState, signalStoreFeature, type, withHooks, withState } from '@ngrx/signals';

import { inject } from '@angular/core';

import { map, take } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';
import { FEATURE_FLAG_NOT_FOUND, FeaturedFlags } from '@dotcms/dotcms-models';

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
        { state: type<UVEState>() },
        withState<WithFlagsState>({ flags: {} }),
        withHooks({
            onInit: (store) => {
                const propertiesService = inject(DotPropertiesService);
                propertiesService
                    .getFeatureFlags(flags)
                    .pipe(
                        take(1),
                        map((rawFlags) =>
                            Object.fromEntries(
                                Object.entries(rawFlags).map(([key, value]) => [
                                    key,
                                    value === true || value === FEATURE_FLAG_NOT_FOUND
                                ])
                            )
                        )
                    )
                    .subscribe((flags) => {
                        patchState(store, { flags });
                    });
            }
        })
    );
}
