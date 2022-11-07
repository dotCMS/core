import { pluck, catchError, take, map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { HttpErrorResponse } from '@angular/common/http';

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
    constructor(
        private coreWebService: CoreWebService,
        private httpErrorManagerService: DotHttpErrorManagerService
    ) {}

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
                pluck('entity'),
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
                pluck('entity'),
                catchError((error: HttpErrorResponse) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => null)
                    );
                })
            );
    }
}
