import { Injectable } from '@angular/core';
import { CoreWebService } from '@dotcms/dotcms-js';
import { Observable } from 'rxjs';
import { pluck } from 'rxjs/operators';

@Injectable()
export class DotToolGroupService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Hide the toolGroup in the menu.
     * @param {string} layoutId
     * @returns Observable<{message: string}>
     * @memberof DotToolGroupService
     */
    hide(layoutId: string): Observable<{ message: string }> {
        return this.coreWebService
            .requestView({
                url: `api/v1/toolgroups/${layoutId}/_removefromcurrentuser`,
                method: 'PUT'
            })
            .pipe(pluck('entity'));
    }
    /**
     * Show the toolGroup in the menu.
     * @param {string} layoutId
     * @returns Observable<{message: string}>
     * @memberof DotToolGroupService
     */
    show(layoutId: string): Observable<{ message: string }> {
        return this.coreWebService
            .requestView({
                url: `api/v1/toolgroups/${layoutId}/_addtocurrentuser`,
                method: 'PUT'
            })
            .pipe(pluck('entity'));
    }
}
