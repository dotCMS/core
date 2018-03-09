import { DotRouterService } from './../../../../../api/services/dot-router/dot-router.service';
import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs/Observable';

/**
 * Route Guard that checks if a User is logged in.
 */
@Injectable()
export class DotEditLayoutGuardService implements CanActivate {
    constructor(private dotRouterService: DotRouterService) {}

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
        if (route.parent.data.content) {
            return route.parent.data.content.page.canEdit;
        }
        return true;
    }
}
