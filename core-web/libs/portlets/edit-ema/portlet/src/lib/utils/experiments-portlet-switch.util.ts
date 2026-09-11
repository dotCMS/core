import { Observable, catchError, map, of } from 'rxjs';

import { inject } from '@angular/core';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

/**
 * Reads the UVE Experiments entry-point switch (#37005).
 *
 * `true` sends the Experiments navigation item to the new site-wide portlet; `false` — the shipped
 * default — leaves it on the legacy per-page screens.
 *
 * Two deliberate choices, both of which are invisible in the UI when wrong:
 *
 * **Uncached.** `getFreshFeatureFlag` re-fetches every call, where `getFeatureFlag` memoizes for
 * the life of the SPA session. SC-002 gives an operator under a minute to move between entry
 * points without a deployment or restart, and the cached reader would hold the old value until a
 * hard reload. Same reader and same reasoning as `dotAiConfigDetailMatchGuard`. The cost is one
 * small request per Experiments gesture — not per render.
 *
 * **Fails closed, on the key as well as on the request.** Only an explicit `true` turns this on;
 * a failed read, an absent key and any other value all resolve to `false`, which is the
 * pre-change behavior FR-015 requires.
 *
 * That is why this reads the raw key instead of `getFreshFeatureFlag`: the shared normaliser maps
 * a missing key to *enabled* (`normalizeFlagValue`, for flags that ship on), so a successful
 * response that simply did not carry this one would have switched the entry point over. A stock
 * build cannot reach that — the property ships `false` and is whitelisted and boolean-typed in
 * `ConfigurationResource` — but then the guarantee would rest on config wiring rather than on
 * this code, which is not what a switch adjacent to the visitor-facing kill switch should do.
 * `getKey` is uncached like `getFreshFeatureFlag`, so SC-002 is unaffected.
 *
 * The guard belongs at this call site rather than inside {@link DotPropertiesService}, whose
 * behavior every other flag consumer already depends on.
 *
 * Call it with no argument inside an injection context, or pass an already-injected
 * {@link DotPropertiesService} to read it from a method — which is what the toolbar does, so the
 * value is fetched at the gesture rather than fixed for the component's lifetime.
 */
export function readExperimentsPortletSwitch(
    propertiesService: DotPropertiesService = inject(DotPropertiesService)
): Observable<boolean> {
    return propertiesService.getKey(FeaturedFlags.FEATURE_FLAG_EXPERIMENTS_PORTLET).pipe(
        map((value) => value === true || value === 'true'),
        catchError(() => of(false))
    );
}
