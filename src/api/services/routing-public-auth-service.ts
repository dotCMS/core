import {Injectable} from '@angular/core';
import {CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot} from '@angular/router';
import _ from 'lodash';
import {Observable} from 'rxjs/Rx';
import {DotcmsConfig} from './system/dotcms-config';
import {LoginService} from './login-service';
import {DotRouterService} from './dot-router-service';

@Injectable()
export class RoutingPublicAuthService implements CanActivate {
    constructor(private router: DotRouterService,
                private loginService: LoginService, private dotcmsConfig: DotcmsConfig) {}

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
        return new Promise(resolve => {
            if (this.loginService.isLogin) {
                this.router.goToMain();
                resolve(false);
            } else {
                this.loginService.loadAuth().subscribe(() => {
                    if (this.loginService.isLogin) {
                        this.dotcmsConfig.getConfig().then( dotcmsConfig => {
                            this.router.goToMain();
                            resolve(false);
                        });
                    } else {
                        resolve(true);
                    }
                });
            }
        });
    }
}