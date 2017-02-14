import {Injectable} from '@angular/core';
import {CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot} from '@angular/router';
import {DotRouterService} from './dot-router-service';

/**
 * TODO: This service is not used any more. Maybe should be deleted
 */
@Injectable()
export class RoutingRootAuthService implements CanActivate {

    constructor(private router: DotRouterService) {}

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
        this.router.goToRoot();
        return true;
    }
}