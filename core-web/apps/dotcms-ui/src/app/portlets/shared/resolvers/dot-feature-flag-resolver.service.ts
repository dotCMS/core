import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { DotPropertiesService } from '@dotcms/data-access';

/**
 * Return if the Feature flag exist or not
 *
 * @export
 * @class DotFeatureFlagResolver
 * @implements {Observable<Record<string, boolean>>}
 *
 * Need set in data the feature flag with the index name featuredFlag and the FeatureFlag needed
 * @example
 *  data: {
 *      featuredFlag: FEATURED_FLAG_STRING
 *  }
 */
@Injectable()
export class DotFeatureFlagResolver
    implements Resolve<Observable<Record<string, boolean | string>> | Observable<boolean | string>>
{
    constructor(private readonly dotConfigurationService: DotPropertiesService) {}

    resolve(route: ActivatedRouteSnapshot) {
        if (route.data.featuredFlagsToCheck) {
            return this.dotConfigurationService.getFeatureFlags(route.data.featuredFlagsToCheck);
        }

        return of(false);
    }
}
