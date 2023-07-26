import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { CanDeactivate } from '@angular/router';

import { filter } from 'rxjs/operators';

import { DotRouterService } from '@services/dot-router/dot-router.service';

/**
 *
 * @export
 * @interface CanDeactivateGuarded
 */
interface CanDeactivateGuard {
    canDeactivate: () => Observable<boolean>;
}

/**
 *
 * Allows to set whether a route can be deactivated.
 * @export
 * @class CanDeactivateGuardService
 * @implements {CanDeactivate<CanDeactivateGuard>}
 */
@Injectable()
export class CanDeactivateGuardService implements CanDeactivate<CanDeactivateGuard> {
    constructor(private dotRouterService: DotRouterService) {}

    /**
     *
     * Make sure the changes have been saved before leaving the route.
     * @return {*}  {Observable<boolean>}
     * @memberof CanDeactivateGuardService
     */
    canDeactivate(): Observable<boolean> {
        return this.dotRouterService.canDeactivateRoute$.pipe(
            filter((res) => {
                console.log('can deactivate route', res);
                if (!res) {
                    this.dotRouterService.requestPageLeave();
                }

                return res;
            })
        );
    }
}
