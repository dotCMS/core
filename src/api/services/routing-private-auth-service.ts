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
            if (!this.loginService.isLogin) {
                this.loginService.loadAuth().subscribe(() => {
                    this.checkAccessLoginUser(resolve, state.url);
                });
            } else {
                this.checkAccessLoginUser(resolve, state.url);
            }
        });
    }

    private checkAccessLoginUser(resolve, url: string): void {
        if (this.loginService.isLogin) {
            this.dotcmsConfig.getConfig().then( dotcmsConfig => {
                this.checkAccess(url).then(checkAccess => {
                    if (!checkAccess) {
                        this.router.goToMain();
                    }

                    resolve(checkAccess);
                });
            });
        } else {
            this.router.goToLogin();
            resolve(false);
        }
    }

    private checkAccess(url: string): Promise<boolean> {
        return new Promise( resolve => {
            if (this.routingService.currentMenu) {
                resolve(this.check.bind(this)(url));
            } else {
                this.routingService.menusChange$.subscribe(() => resolve(this.check.bind(this)(url)));
            }
        });
    }

    private check(url: string): boolean {
        let isRouteLoaded = true;

        if (url !== '/dotCMS/pl') {
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