import { Observable, of } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSResponse } from '@dotcms/dotcms-models';

const addToMenuUrl = '/api/v1/portlet';

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
    private http = inject(HttpClient);
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
        return this.http
            .post<DotCMSResponse<string>>(`${addToMenuUrl}/custom`, {
                ...params,
                portletId: `${this.cleanUpPorletId(params.portletName)}_${params.dataViewMode}`
            })
            .pipe(
                map((response) => response.entity),
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

        return this.http
            .put<
                DotCMSResponse<string>
            >(`${addToMenuUrl}/custom/c_${portletId}/_addtolayout/${params.layoutId}`, {})
            .pipe(
                map((response) => response.entity),
                catchError((error: HttpErrorResponse) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }
}
