import { Injectable } from '@angular/core';
import { CanActivateChild, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs/Rx';
import { DotContentletService } from './../dot-contentlet.service';
import { DotNavigationService } from '../../../view/components/dot-navigation/dot-navigation.service';

/**
 * Route Guard that checks if a User have access to the specified Content Type.
 */
@Injectable()
export class ContentletGuardService implements CanActivateChild {
    constructor(
        private dotContentletService: DotContentletService,
        private dotNavigationService: DotNavigationService
    ) {}

    canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.canAccessContentType(route.params.id);
    }

    /**
     * Check if User has access to the requested route (url) based on the Content Type, otherwise return to the 'First Portlet'.
     *
     * @param {string} url
     * @returns {boolean}
     */
    canAccessContentType(url: string): Observable<boolean> {
        return this.dotContentletService.isContentTypeInMenu(url).map(res => {
            if (!res) {
                this.dotNavigationService.goToFirstPortlet();
            }
            return res;
        });
    }
}
