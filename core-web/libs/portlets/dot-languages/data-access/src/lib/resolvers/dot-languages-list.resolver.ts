import { Observable } from 'rxjs';

import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot } from '@angular/router';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

export const DotLanguagesListResolver: ResolveFn<DotLanguage[]> = (
    _route: ActivatedRouteSnapshot,
    _state: RouterStateSnapshot
): Observable<DotLanguage[]> => {
    const languageService = inject(DotLanguagesService);

    return languageService.get();
};
