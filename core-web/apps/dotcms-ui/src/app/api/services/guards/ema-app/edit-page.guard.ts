import { combineLatest, of } from 'rxjs';

import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotPropertiesService, EmaAppConfigurationService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

export const editPageGuard: CanMatchFn = () => {
    const properties = inject(DotPropertiesService);
    const emaConfiguration = inject(EmaAppConfigurationService);
    const router = inject(Router);
    const DEFAULT_LANGUAGE_ID = 1;

    const queryParams = router.getCurrentNavigation().extractedUrl.queryParams;
    const url = queryParams['url'];
    const languageID = queryParams['language_id'];

    if (!languageID) {
        router.navigate([`/edit-page/content`], {
            queryParams: {
                ...queryParams,
                language_id: DEFAULT_LANGUAGE_ID
            },
            replaceUrl: true
        });

        return of(false);
    }

    return combineLatest([
        properties.getFeatureFlag(FeaturedFlags.FEATURE_FLAG_NEW_EDIT_PAGE),
        emaConfiguration.get(url)
    ]).pipe(map(([flag, value]) => !(flag || value))); // Returns true if EMA Flag is false or if EMA Config doesn't exist for this page
};
