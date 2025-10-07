import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateChild, RouterStateSnapshot } from '@angular/router';

import { map } from 'rxjs/operators';

import { DotContentTypeService } from '@dotcms/data-access';

import { DotNavigationService } from '../../../view/components/dot-navigation/services/dot-navigation.service';

/**
 * Route Guard that checks if a User have access to the specified Content Type.
 */
@Injectable()
export class ContentletGuardService implements CanActivateChild {
    private dotContentTypeService = inject(DotContentTypeService);
    private dotNavigationService = inject(DotNavigationService);

    canActivateChild(
        route: ActivatedRouteSnapshot,
        _state: RouterStateSnapshot
    ): Observable<boolean> {
        return this.canAccessContentType(route.params.id);
    }

    /**
     * Check if User has access to the requested route (url) based on the Content Type, otherwise return to the 'First Portlet'.
     *
     * @param string url
     * @returns boolean
     */
    canAccessContentType(url: string): Observable<boolean> {
        return this.dotContentTypeService.isContentTypeInMenu(url).pipe(
            map((res) => {
                if (!res) {
                    this.dotNavigationService.goToFirstPortlet();
                }

                return res;
            })
        );
    }
}
