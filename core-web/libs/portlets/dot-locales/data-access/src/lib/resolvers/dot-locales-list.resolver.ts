import { forkJoin, Observable } from 'rxjs';

import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage, DotLanguagesISO } from '@dotcms/dotcms-models';

export interface DotLocalesListResolverData extends DotLanguagesISO {
    locales: DotLanguage[];
}

export const DotLocalesListResolver: ResolveFn<DotLocalesListResolverData> = (
    _route: ActivatedRouteSnapshot,
    _state: RouterStateSnapshot
): Observable<DotLocalesListResolverData> => {
    const languageService = inject(DotLanguagesService);

    return forkJoin([languageService.get(), languageService.getISO()]).pipe(
        map(([locales, iso]) => ({
            locales,
            ...iso
        }))
    );
};
