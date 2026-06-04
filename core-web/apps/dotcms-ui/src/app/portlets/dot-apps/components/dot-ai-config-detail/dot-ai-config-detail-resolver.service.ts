import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn } from '@angular/router';

import { take } from 'rxjs/operators';

import { DotAppsService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';

const DOT_AI_APP_KEY = 'dotAI';

export const dotAiConfigDetailResolver: ResolveFn<DotApp> = (route: ActivatedRouteSnapshot) => {
    const id = route.paramMap.get('id');

    return inject(DotAppsService).getConfiguration(DOT_AI_APP_KEY, id).pipe(take(1));
};
