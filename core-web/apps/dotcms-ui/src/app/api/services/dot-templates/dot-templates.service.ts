import { Observable } from 'rxjs';

import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotActionBulkResult, DotCMSResponse, DotTemplate } from '@dotcms/dotcms-models';

export const TEMPLATE_API_URL = '/api/v1/templates/';

export type DotTemplatesRequestOptions = {
    host?: string;
    archive?: boolean;
    page?: number;
    per_page?: number;
    direction?: string;
    orderby?: string;
    filter?: string;
};

export const DEFAULT_PER_PAGE = 40;
export const DEFAULT_PAGE = 1;
export const DEFAULT_ORDERBY = 'modDate';
export const DEFAULT_DIRECTION = 'DESC';
export const DEFAULT_ARCHIVE = false;

/**
 * Provide util methods to handle templates in the system.
 * @export
 * @class DotTemplatesService
 */
@Injectable()
export class DotTemplatesService {
    private http = inject(HttpClient);
    private httpErrorManagerService = inject(DotHttpErrorManagerService);

    /**
     * Return a list of templates.
     * @returns Observable<DotTemplate[]>
     * @memberof DotTemplatesService
     */
    get(): Observable<DotTemplate[] | null> {
        return this.http.get<DotCMSResponse<DotTemplate[]>>(TEMPLATE_API_URL).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotTemplate[]>(error))
        );
    }

    /**
     * Get the template, pass the version default working
     *
     * @param {string} id
     * @param {string} [version='working']
     * @returns {Observable<DotTemplate>}
     * @memberof DotTemplatesService
     */
    getById(id: string, version = 'working'): Observable<DotTemplate | null> {
        const url = `${TEMPLATE_API_URL}${id}/${version}`;

        return this.http.get<DotCMSResponse<DotTemplate>>(url).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotTemplate>(error))
        );
    }

    /**
     * Get the template filtered by tittle or inode .
     *
     * @param {DotTemplatesRequestOptions} options
     * @returns {Observable<{ templates: DotTemplate[]; totalRecords: number }>}
     * @memberof DotTemplatesService
     */
    getFiltered(
        options: DotTemplatesRequestOptions
    ): Observable<{ templates: DotTemplate[]; totalRecords: number }> {
        const url = `${TEMPLATE_API_URL}`;
        const per_page = options.per_page ?? DEFAULT_PER_PAGE;
        const page = options.page ?? DEFAULT_PAGE;
        const orderby = options.orderby ?? DEFAULT_ORDERBY;
        const direction = options.direction ?? DEFAULT_DIRECTION;
        const archive = options.archive ?? DEFAULT_ARCHIVE;
        let params = new HttpParams()
            .set('per_page', per_page.toString())
            .set('page', page.toString())
            .set('orderby', orderby.toString())
            .set('direction', direction.toString())
            .set('archive', archive.toString());

        if (options.filter != null) {
            params = params.set('filter', options.filter);
        }

        return this.http
            .get<{ entity: DotTemplate[]; pagination: { totalEntries: number } }>(url, {
                params,
                observe: 'response'
            })
            .pipe(
                map((response) => {
                    const templates = response.body?.entity || [];
                    const totalRecords =
                        response.body?.pagination?.totalEntries || templates.length;

                    return { templates, totalRecords };
                }),
                catchError((error: HttpErrorResponse) => {
                    return this.httpErrorManagerService.handle(error).pipe(
                        take(1),
                        map(() => ({ templates: [], totalRecords: 0 }))
                    );
                })
            );
    }

    /**
     * Creates a template
     *
     * @param {DotTemplate} values
     * @return Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    create(values: DotTemplate): Observable<DotTemplate | null> {
        return this.http.post<DotCMSResponse<DotTemplate>>(TEMPLATE_API_URL, values).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotTemplate>(error))
        );
    }

    /**
     * Updates a template
     * @returns Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    update(values: DotTemplate): Observable<DotTemplate | null> {
        return this.http.put<DotCMSResponse<DotTemplate>>(TEMPLATE_API_URL, values).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotTemplate>(error))
        );
    }

    /**
     * Save and Publish a template
     * @param {DotTemplate} values
     * @returns Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    saveAndPublish(values: DotTemplate): Observable<DotTemplate | null> {
        return this.http
            .put<DotCMSResponse<DotTemplate>>(`${TEMPLATE_API_URL}_savepublish`, values)
            .pipe(
                map((response) => response.entity),
                catchError((error: HttpErrorResponse) => this.handleError<DotTemplate>(error))
            );
    }

    /**
     * Delete a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    delete(identifiers: string[]): Observable<DotActionBulkResult | null> {
        return this.http
            .delete<DotCMSResponse<DotActionBulkResult>>(TEMPLATE_API_URL, {
                body: identifiers
            })
            .pipe(
                map((response) => response.entity),
                catchError((error: HttpErrorResponse) =>
                    this.handleError<DotActionBulkResult>(error)
                )
            );
    }

    /**
     * Unarchive a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    unArchive(identifiers: string[]): Observable<DotActionBulkResult | null> {
        const url = `${TEMPLATE_API_URL}_unarchive`;

        return this.http.put<DotCMSResponse<DotActionBulkResult>>(url, identifiers).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotActionBulkResult>(error))
        );
    }

    /**
     * Archive a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    archive(identifiers: string[]): Observable<DotActionBulkResult | null> {
        const url = `${TEMPLATE_API_URL}_archive`;

        return this.http.put<DotCMSResponse<DotActionBulkResult>>(url, identifiers).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotActionBulkResult>(error))
        );
    }

    /**
     * Unpublish a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    unPublish(identifiers: string[]): Observable<DotActionBulkResult | null> {
        const url = `${TEMPLATE_API_URL}_unpublish`;

        return this.http.put<DotCMSResponse<DotActionBulkResult>>(url, identifiers).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotActionBulkResult>(error))
        );
    }

    /**
     * Publish a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    publish(identifiers: string[]): Observable<DotActionBulkResult | null> {
        const url = `${TEMPLATE_API_URL}_publish`;

        return this.http.put<DotCMSResponse<DotActionBulkResult>>(url, identifiers).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotActionBulkResult>(error))
        );
    }

    /**
     * Copy a template
     * @param {string} identifier
     * @returns Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    copy(identifier: string): Observable<DotTemplate | null> {
        const url = `${TEMPLATE_API_URL}${identifier}/_copy`;

        return this.http.put<DotCMSResponse<DotTemplate>>(url, {}).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotTemplate>(error))
        );
    }

    private handleError<T>(error: HttpErrorResponse): Observable<T | null> {
        return this.httpErrorManagerService.handle(error).pipe(
            take(1),
            map(() => null)
        );
    }
}
