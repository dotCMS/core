import { Location } from '@angular/common';
import { ActivatedRoute, Params, Router } from '@angular/router';

/**
 * Updates the URL via `location.replaceState` without triggering Angular router events.
 * Merges the provided queryParams into the current URL.
 */
export function silentNavigate(
    router: Router,
    location: Location,
    route: ActivatedRoute,
    queryParams: Params
): void {
    const urlTree = router.createUrlTree([], {
        relativeTo: route,
        queryParams,
        queryParamsHandling: 'merge'
    });
    location.replaceState(router.serializeUrl(urlTree));
}
