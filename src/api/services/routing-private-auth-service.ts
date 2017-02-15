import {Injectable} from '@angular/core';
import {CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot} from '@angular/router';
import {RoutingService} from './routing-service';
import _ from 'lodash';
import {Observable} from 'rxjs/Rx';
import {DotcmsConfig} from './system/dotcms-config';
import {LoginService} from './login-service';
import {DotRouterService} from './dot-router-service';

@Injectable()
export class RoutingPrivateAuthService implements CanActivate {
    constructor(private router: DotRouterService, private routingService: RoutingService,
             private loginService: LoginService, private dotcmsConfig: DotcmsConfig) {}

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {

        return Observable.create( obs => {
            this.loginService.isLogin$.subscribe(isLogin => {

                if (isLogin) {
                    this.dotcmsConfig.getConfig().subscribe( configParams => {
                        this.checkAccess(state.url).subscribe(checkAccess => {
                            if (!checkAccess) {
                                this.router.goToMain();
                            }
                            obs.next(checkAccess);
                        });
                    });
                } else {
                    this.router.goToLogin();
                    obs.next(false);
                }
            });
        }).take(1);
    }

    private checkAccess(url: string): Observable<boolean> {
        return Observable.create( obs => {
            if (this.routingService.currentMenu) {
                obs.next(this.check(url));
            } else {
                this.routingService.menusChange$.subscribe(() => obs.next(this.check.bind(this)(url)));
            }
        }).take(1);
    }

    private check(url: string): boolean {
        let isRouteLoaded = true;

        if (url !== '/c/pl') {
            isRouteLoaded = this.routingService.isPortlet(url);

            if (!isRouteLoaded) {
                this.router.goToLogin();
            }else {
                this.routingService.setCurrentPortlet(url);
            }
        }

        return isRouteLoaded;
    }
}