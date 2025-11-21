import { combineLatest } from 'rxjs';

import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotPropertiesService, EmaAppConfigurationService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

export const editPageGuard: CanMatchFn = () => {
    const properties = inject(DotPropertiesService);
    const emaConfiguration = inject(EmaAppConfigurationService);

    const router = inject(Router);

    const url = router.currentNavigation().extractedUrl.queryParams['url'];

    return combineLatest([
        properties.getFeatureFlag(FeaturedFlags.FEATURE_FLAG_NEW_EDIT_PAGE),
        emaConfiguration.get(url)
    ]).pipe(map(([flag, value]) => !(flag || value))); // Returns true if EMA Flag is false or if EMA Config doesn't exist for this page
};
