import { Observable, Observer } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { RoutingService } from '@dotcms/dotcms-js';
import { LoginService, DotcmsConfigService } from '@dotcms/dotcms-js';
import { DotRouterService } from '@dotcms/dotcms-js';

@Injectable()
export class RoutingPrivateAuthService implements CanActivate {
    private router = inject(DotRouterService);
    private routingService = inject(RoutingService);
    private loginService = inject(LoginService);
    private dotcmsConfigService = inject(DotcmsConfigService);

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return Observable.create((obs) => {
            this.loginService.isLogin$.subscribe((isLogin) => {
                if (isLogin) {
                    this.dotcmsConfigService.getConfig().subscribe((configParams) => {
                        if (state.url.indexOf('home') > -1) {
                            if (this.routingService.firstPortlet) {
                                this.goToFirstPortlet(obs);
                            } else {
                                this.routingService.menusChange$.subscribe((res) => {
                                    this.goToFirstPortlet(obs);
                                });
                            }
                        } else {
                            this.checkAccess(state.url).subscribe((checkAccess) => {
                                if (!checkAccess) {
                                    this.router.goToMain();
                                }

                                obs.next(checkAccess);
                            });
                        }
                    });
                } else {
                    this.router.goToLogin();
                    obs.next(false);
                }
            });
        }).take(1);
    }

    private goToFirstPortlet(obs: Observer<boolean>): void {
        this.router.goToURL(`/c/${this.routingService.firstPortlet}`);
        obs.next(false);
    }

    private checkAccess(url: string): Observable<boolean> {
        return Observable.create((obs) => {
            if (this.routingService.currentMenu) {
                obs.next(this.check(url));
            } else {
                this.routingService.menusChange$.subscribe(() => obs.next(this.check(url)));
            }
        }).take(1);
    }

    private check(url: string): boolean {
        const isRouteLoaded = this.routingService.isPortlet(url);

        if (isRouteLoaded) {
            this.routingService.setCurrentPortlet(url);
        }

        return true;
    }
}
