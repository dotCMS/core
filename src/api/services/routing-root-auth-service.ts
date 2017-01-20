import {Injectable} from '@angular/core';
import {CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot} from '@angular/router';
import {RoutingService} from './routing-service';
import _ from 'lodash';
import {Observable} from '../../../node_modules/rxjs/Rx.d';
import {DotcmsConfig} from './system/dotcms-config';
import {LoginService} from './login-service';
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