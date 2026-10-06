import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotCurrentUserService, DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

const redirectToFields = (router: Router, url: string) =>
    router.parseUrl(url.replace(/\/[^/?]+(\?.*)?$/, '/fields$1'));

export const styleEditorTabGuard: CanActivateFn = (_route, state) => {
    const router = inject(Router);

    return inject(DotPropertiesService)
        .getFeatureFlag(FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR)
        .pipe(map((enabled) => enabled || redirectToFields(router, state.url)));
};

export const permissionsTabGuard: CanActivateFn = (_route, state) => {
    const router = inject(Router);

    return inject(DotCurrentUserService)
        .hasAccessToPortlet('permissions')
        .pipe(map((hasAccess) => hasAccess || redirectToFields(router, state.url)));
};
