import { Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import {
    ActivatedRouteSnapshot,
    CanActivate,
    CanActivateChild,
    RouterStateSnapshot
} from '@angular/router';

import { catchError, map, switchMap, take } from 'rxjs/operators';

import { DotMessageService, DotRouterService } from '@dotcms/data-access';
import { DotcmsConfigService, LoggerService, LoginService } from '@dotcms/dotcms-js';

/**
 * Route Guard that checks if a User is logged in.
 */
@Injectable()
export class AuthGuardService implements CanActivate, CanActivateChild {
    private dotRouterService = inject(DotRouterService);
    private loginService = inject(LoginService);
    private configService = inject(DotcmsConfigService);
    private messageService = inject(DotMessageService);
    private logger = inject(LoggerService);

    canActivateChild(
        route: ActivatedRouteSnapshot,
        state: RouterStateSnapshot
    ): Observable<boolean> {
        return this.canActivate(route, state);
    }

    canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.loginService.isLogin$.pipe(
            take(1),
            switchMap((isLogin) => {
                if (!isLogin) {
                    this.dotRouterService.goToLogin();
                    this.dotRouterService.storedRedirectUrl = state.url;

                    return of(false);
                }

                // Reuse a matching bundle, or await the fetch before components translate their labels.
                return this.configService.getConfig().pipe(
                    take(1),
                    catchError((error) => {
                        this.logger.warn(
                            'Unable to load configuration; fetching uncached translations.',
                            error
                        );

                        return of(null);
                    }),
                    switchMap((config) =>
                        this.messageService.init({
                            language:
                                this.loginService.auth?.loginAsUser?.languageId ||
                                this.loginService.currentUserLanguageId ||
                                'default',
                            languageBundleVersion: config?.languageBundleVersion
                        })
                    ),
                    map(() => true),
                    catchError((error) => {
                        this.logger.error(
                            'Unable to load translations; continuing with available messages.',
                            error
                        );

                        return of(true);
                    })
                );
            })
        );
    }
}
