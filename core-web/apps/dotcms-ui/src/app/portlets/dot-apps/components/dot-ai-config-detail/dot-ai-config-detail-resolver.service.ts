import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { take } from 'rxjs/operators';

import { DotAppsService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';

const DOT_AI_APP_KEY = 'dotAI';

@Injectable()
export class DotAiConfigDetailResolver implements Resolve<DotApp> {
    private dotAppsService = inject(DotAppsService);

    resolve(route: ActivatedRouteSnapshot): Observable<DotApp> {
        const id = route.paramMap.get('id');

        return this.dotAppsService.getConfiguration(DOT_AI_APP_KEY, id).pipe(take(1));
    }
}
