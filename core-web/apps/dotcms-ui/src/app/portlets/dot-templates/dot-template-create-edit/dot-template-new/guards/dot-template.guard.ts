import { Injectable, inject } from '@angular/core';
import { CanLoad, Route, UrlSegment } from '@angular/router';

import { DotRouterService } from '@dotcms/data-access';

@Injectable()
export class DotTemplateGuard implements CanLoad {
    private dotRouterService = inject(DotRouterService);

    canLoad(_route: Route, segments: UrlSegment[]): boolean {
        const [{ path }] = segments;

        const isValidTemplatePath = path === 'designer' || path === 'advanced';

        if (isValidTemplatePath) {
            return true;
        }

        this.dotRouterService.gotoPortlet('templates');

        return false;
    }
}
