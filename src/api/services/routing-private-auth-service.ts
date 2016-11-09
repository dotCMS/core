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

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {

        return new Promise(resolve => {
            if (this.loginService.isLogin) {
                resolve(this.checkAccess(state.url));
            } else {
                this.loginService.loadAuth().subscribe(() => {
                    if (this.loginService.isLogin) {
                        this.dotcmsConfig.getConfig().subscribe( () => {
                            if (this.checkAccess(state.url)) {
                                resolve(true);
                            } else {
                                this.router.goToMain();
                                resolve(false);
                            }
                        });
                    } else {
                        this.router.goToLogin();
                        resolve(false);
                    }
                });
            }
        });
    }

    private checkAccess(url: string): boolean {
        let isRouteLoaded = true;

        if (this.routingService.currentMenu && url !== '/dotCMS/pl') {
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