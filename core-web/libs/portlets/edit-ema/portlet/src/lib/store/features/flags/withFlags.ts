import { patchState, signalStoreFeature, type, withHooks, withState } from '@ngrx/signals';

import { inject } from '@angular/core';

import { map, take } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';
import { FEATURE_FLAG_NOT_FOUND, FeaturedFlags } from '@dotcms/dotcms-models';

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
                    .pipe(
                        take(1),
                        // Normalize to boolean: true or NOT_FOUND (flag absent on server) → enabled.
                        // Mirrors the single-flag getFeatureFlag() default (dot-properties.service.ts:88).
                        // Any other value, including explicit false, disables the flag.
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
