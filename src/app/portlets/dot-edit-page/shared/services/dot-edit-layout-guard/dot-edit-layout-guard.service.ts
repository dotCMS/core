import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

/**
 * Route Guard that checks if a User is logged in.
 */
@Injectable()
export class DotEditLayoutGuardService implements CanActivate {
    constructor() {}

    canActivate(route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): boolean {
        if (route.parent.data.content) {
            return route.parent.data.content.page.canEdit;
        }
        return true;
    }
}
