import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';

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
        if (route.data.featuredFlagsToCheck) {
            return this.dotConfigurationService.getKeys(route.data.featuredFlagsToCheck).pipe(
                map((result) =>
                    route.data.featuredFlagsToCheck.reduce(
                        (
                            acc: {
                                [key: string]: boolean;
                            },
                            key: string
                        ) => {
                            if (result && result[key]) {
                                acc[key] = result && result[key] === 'true';
                            }

                            return acc;
                        },
                        {}
                    )
                )
            );
        }

        return of(false);
    }
}
