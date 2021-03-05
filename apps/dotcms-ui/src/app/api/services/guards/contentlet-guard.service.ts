import { map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { CanActivateChild, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotContentTypeService } from '@services/dot-content-type/dot-content-type.service';

/**
 * Route Guard that checks if a User have access to the specified Content Type.
 */
@Injectable()
export class ContentletGuardService implements CanActivateChild {
    constructor(
        private dotContentTypeService: DotContentTypeService,
        private dotNavigationService: DotNavigationService
    ) {}

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
