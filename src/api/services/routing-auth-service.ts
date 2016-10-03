import {Injectable} from '@angular/core';
import {CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot} from '@angular/router';
import {RoutingService} from './routing-service';
import _ from 'lodash';

@Injectable()
export class RoutingAuthService implements CanActivate {
    constructor(private router: Router, private rutingService: RoutingService) {}

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
        return this.isRouteAllow(state.url);
    }

    public isRouteAllow(url: string): boolean {
        let isRouteLoaded = _.chain(this.rutingService.currentMenu)
            .flatMap(item => item.menuItems)
            .some(['url', url])
            .value();

        if (isRouteLoaded) {
            return true;
        } else {
            this.router.navigate(['/public/login']);
            return false;
        }
    }

}