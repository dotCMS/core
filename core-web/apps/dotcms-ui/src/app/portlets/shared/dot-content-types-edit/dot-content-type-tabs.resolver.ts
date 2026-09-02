import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotCurrentUserService } from '@dotcms/data-access';

export interface DotContentTypeTabsResolvedData {
    showPermissionsTab: boolean;
}

export const dotContentTypeTabsResolver: ResolveFn<DotContentTypeTabsResolvedData> = () =>
    inject(DotCurrentUserService)
        .hasAccessToPortlet('permissions')
        .pipe(map((showPermissionsTab) => ({ showPermissionsTab })));
