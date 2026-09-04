import { Observable, catchError, of } from 'rxjs';

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
 * **Fails closed.** `getFreshFeatureFlag` errors rather than emitting when the config read fails,
 * so the `catchError` here is what satisfies FR-015: an unreadable switch behaves as off, which is
 * the current pre-change behavior. The guard belongs at this call site rather than inside
 * {@link DotPropertiesService}, whose error behavior every other flag consumer already depends on.
 *
 * Call it with no argument inside an injection context, or pass an already-injected
 * {@link DotPropertiesService} to read it from a method — which is what the toolbar does, so the
 * value is fetched at the gesture rather than fixed for the component's lifetime.
 */
export function readExperimentsPortletSwitch(
    propertiesService: DotPropertiesService = inject(DotPropertiesService)
): Observable<boolean> {
    return propertiesService
        .getFreshFeatureFlag(FeaturedFlags.FEATURE_FLAG_EXPERIMENTS_PORTLET)
        .pipe(catchError(() => of(false)));
}
