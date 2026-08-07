import { patchState, signalStoreFeature, withHooks, withState } from '@ngrx/signals';
import { of } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, map, take } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';
import { FEATURE_FLAG_NOT_FOUND, FeaturedFlags } from '@dotcms/dotcms-models';

/**
 * Resolved feature-flag map: each requested flag mapped to whether it is enabled. Generic over the
 * flag keys so consumers get a signal typed to exactly the flags they asked for — pass the flag
 * list `as const` to narrow `F` (otherwise it widens to the whole `FeaturedFlags` enum).
 */
export type DotFeatureFlags<F extends FeaturedFlags = FeaturedFlags> = Partial<Record<F, boolean>>;

/**
 * Generic signal-store feature that batch-fetches the given feature flags once on init and exposes
 * them as a typed `flags()` signal. Composes into ANY signal store — it declares no host-state
 * constraint because it only reads/writes its own `flags` slice.
 *
 * Values come from {@link DotPropertiesService.getFeatureFlags}: a flag is enabled when the server
 * returns `true` or `FEATURE_FLAG_NOT_FOUND` (flag absent → enabled by default, mirroring the
 * single-flag `getFeatureFlag()` default); any other value, including explicit `false`, disables
 * it. A failed config read degrades every flag to `false` (safe/off) instead of throwing.
 */
export function withFlags<F extends FeaturedFlags>(flags: readonly F[]) {
    return signalStoreFeature(
        withState<{ flags: DotFeatureFlags<F> }>({ flags: {} }),
        withHooks({
            onInit: (store) => {
                inject(DotPropertiesService)
                    .getFeatureFlags([...flags])
                    .pipe(
                        take(1),
                        map(
                            (rawFlags) =>
                                Object.fromEntries(
                                    Object.entries(rawFlags).map(([key, value]) => [
                                        key,
                                        value === true || value === FEATURE_FLAG_NOT_FOUND
                                    ])
                                ) as DotFeatureFlags<F>
                        ),
                        catchError(() => of({} as DotFeatureFlags<F>))
                    )
                    .subscribe((flags) => patchState(store, { flags }));
            }
        })
    );
}
