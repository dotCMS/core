import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { Resolve } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotCurrentUserService } from '@dotcms/data-access';

export interface DotContentTypeTabsResolvedData {
    showPermissionsTab: boolean;
}

@Injectable()
export class DotContentTypeTabsResolver implements Resolve<DotContentTypeTabsResolvedData> {
    private dotCurrentUserService = inject(DotCurrentUserService);

    resolve(): Observable<DotContentTypeTabsResolvedData> {
        return this.dotCurrentUserService
            .hasAccessToPortlet('permissions')
            .pipe(map((showPermissionsTab) => ({ showPermissionsTab })));
    }
}
