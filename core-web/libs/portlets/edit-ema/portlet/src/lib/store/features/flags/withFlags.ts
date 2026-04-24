import { patchState, signalStoreFeature, type, withHooks, withState } from '@ngrx/signals';

import { inject } from '@angular/core';

import { take } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { WithFlagsState } from './models';

import { UVEState } from '../../models';

/**
 * @description Fetches feature flags on store init and patches them into state.
 *
 * Flag values come from {@link DotPropertiesService.getFeatureFlags}, which returns
 * booleans for defined flags and maps `FEATURE_FLAG_NOT_FOUND` (flag not set on the
 * server) to `true` — meaning undefined flags are treated as enabled by default.
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
                    .pipe(take(1))
                    .subscribe((flags) => {
                        patchState(store, { flags });
                    });
            }
        })
    );
}
