import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { DotPropertiesService } from '@dotcms/data-access';
import { map } from 'rxjs/operators';

/**
 * Return if the Feature flag exist or not
 *
 * @export
 * @class DotFeatureFlagResolver
 * @implements {Resolve<Observable<boolean>>}
 *
 * Need set in data the feature flag with the index name featuredFlag and the FeatureFlag needed
 * @example
 *  data: {
 *      featuredFlag: FEATURED_FLAG_STRING
 *  }
 */
@Injectable()
export class DotFeatureFlagResolver implements Resolve<Observable<boolean>> {
    constructor(private readonly dotConfigurationService: DotPropertiesService) {}

    resolve(route: ActivatedRouteSnapshot) {
        if (route.data.featuredFlagToCheck) {
            return this.dotConfigurationService.getKey(route.data.featuredFlagToCheck).pipe(
                map((result) => {
                    return result && result === 'true';
                })
            );
        }

        return of(false);
    }
}
