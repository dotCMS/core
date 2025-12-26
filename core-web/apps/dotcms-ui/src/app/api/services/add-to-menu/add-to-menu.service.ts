import { Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';

const addToMenuUrl = `v1/portlet`;

export interface DotCreateCustomTool {
    contentTypes: string;
    dataViewMode: string;
    portletName: string;
}

export interface DotCustomToolToLayout {
    dataViewMode: string;
    layoutId: string;
    portletName: string;
}

/**
 * Provides methods to create and assign custom tools portlet to layout menu.
 * @export
 * @class DotAddToMenuService
 */
@Injectable()
export class DotAddToMenuService {
    private coreWebService = inject(CoreWebService);
    private httpErrorManagerService = inject(DotHttpErrorManagerService);

    /**
     * Cleans the portletId string from special chars and replaces then with a dash
     *
     * @param {string} name
     * @returns string
     * @memberof DotAddToMenuService
     */
    cleanUpPorletId(name: string): string {
        return name.replace(/[^0-9a-z-]/gi, '-').replace(/(?:\s*-\s*)+|\s{2,}/gm, '-');
    }

    /**
     * Creates a Custom tool portlet and returns the name of the portlet created
     *
     * @param {DotCreateCustomTool} params
     * @returns Observable<string>
     * @memberof DotAddToMenuService
     */
    createCustomTool(params: DotCreateCustomTool): Observable<string> {
        return this.coreWebService
            .requestView({
                body: {
                    ...params,
                    portletId: `${this.cleanUpPorletId(params.portletName)}_${params.dataViewMode}`
                },
                method: 'POST',
                url: `${addToMenuUrl}/custom`
            })
            .pipe(
                map((x) => x?.entity),
                catchError((error: HttpErrorResponse) => {
                    if (error.status === 400) {
                        return of(null);
                    } else {
                        return this.httpErrorManagerService.handle(error).pipe(
                            take(1),
                            map(() => null)
                        );
                    }
                })
            );
    }

    /**
     * Assigns a Custom tool portlet to a layout Id (menu)
     *
     * @param {DotCustomToolToLayout} params
     * @returns Observable<string>
     * @memberof DotAddToMenuService
     */
    addToLayout(params: DotCustomToolToLayout): Observable<string> {
        const portletId = `${this.cleanUpPorletId(params.portletName)}_${params.dataViewMode}`;

        return this.coreWebService
            .requestView({
                method: 'PUT',
                url: `${addToMenuUrl}/custom/c_${portletId}/_addtolayout/${params.layoutId}`
            })
            .pipe(
                map((x) => x?.entity),
                catchError((error: HttpErrorResponse) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }
}
