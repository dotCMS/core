import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { CanDeactivate } from '@angular/router';

import { filter } from 'rxjs/operators';

import { DotEditLayoutService } from '@services/dot-edit-layout/dot-edit-layout.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';

/**
 *
 * @export
 * @interface CanDeactivateGuarded
 */
export interface CanDeactivateGuarded {
    canDeactivate: () => Observable<boolean>;
}

/**
 *
 * Allows to set whether a route can be deactivated.
 * @export
 * @class LayoutEditorCanDeactivateGuardService
 * @implements {CanDeactivate<CanDeactivateGuarded>}
 */
@Injectable()
// todo consider renaming
export class LayoutEditorCanDeactivateGuardService implements CanDeactivate<CanDeactivateGuarded> {
    constructor(
        private dotEditLayoutService: DotEditLayoutService,
        private dotRouterService: DotRouterService
    ) {}

    /**
     *
     * Make sure the changes have been saved before leaving the route.
     * @return {*}  {Observable<boolean>}
     * @memberof LayoutEditorCanDeactivateGuardService
     */
    canDeactivate(): Observable<boolean> {
        return this.dotRouterService.canDeactivateRoute$.pipe(
            filter((res) => {
                if (!res) {
                    this.dotRouterService.requestPageLeave();
                }

                return res;
            })
        );
    }
}
