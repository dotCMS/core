import { Injectable } from '@angular/core';
import { CanDeactivate } from '@angular/router';
import { DotEditLayoutService } from '../dot-edit-layout/dot-edit-layout.service';
import { filter } from 'rxjs/operators';
import { Observable } from 'rxjs';

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
export class LayoutEditorCanDeactivateGuardService implements CanDeactivate<CanDeactivateGuarded> {

    constructor(private dotEditLayoutService: DotEditLayoutService) {}

    /**
     *
     * Make sure the changes have been saved before leaving the route.
     * @return {*}  {Observable<boolean>}
     * @memberof LayoutEditorCanDeactivateGuardService
     */
    canDeactivate(): Observable<boolean> {
        return this.dotEditLayoutService.canBeDesactivated$.pipe(
            filter((res) => {
                if(!res) {
                    this.dotEditLayoutService.changeMessageState(!res);
                }
                return res;
            })
        );
    }
}
