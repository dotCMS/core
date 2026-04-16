import { patchState, signalStoreFeature, type, withHooks, withState } from '@ngrx/signals';

import { inject } from '@angular/core';

import { take } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { UVEFlags, WithFlagsState } from './models';

import { UVEState } from '../../models';

/**
 *
 * @description Loads feature flags from configuration, then forces `FEATURE_FLAG_UVE_STYLE_EDITOR` to `true`.
 * @export
 * @return {*}
 */
export function withFlags(featureFlagKeys: FeaturedFlags[]) {
    return signalStoreFeature(
        { state: type<UVEState>() },
        withState<WithFlagsState>({ flags: {} }),
        withHooks({
            onInit: (store) => {
                const propertiesService = inject(DotPropertiesService);

                propertiesService
                    .getFeatureFlags(featureFlagKeys)
                    .pipe(take(1))
                    .subscribe((fetchedFlags) => {
                        // TODO: Remove this, only harcoded until the PR that fix is merged
                        const flags: UVEFlags = { ...(fetchedFlags as unknown as UVEFlags) };

                        flags[FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR] = true;

                        patchState(store, { flags });
                    });
            }
        })
    );
}
