import { Observable } from 'rxjs';

import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, pluck, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotRequestOptionsArgs } from '@dotcms/dotcms-js';
import { DotActionBulkResult, DotTemplate } from '@dotcms/dotcms-models';

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
    get(): Observable<DotTemplate[]> {
        return this.request<DotTemplate[]>({ url: TEMPLATE_API_URL });
    }

    /**
     * Get the template, pass the version default working
     *
     * @param {string} id
     * @param {string} [version='working']
     * @returns {Observable<DotTemplate>}
     * @memberof DotTemplatesService
     */
    getById(id: string, version = 'working'): Observable<DotTemplate> {
        const url = `${TEMPLATE_API_URL}${id}/${version}`;

        return this.request<DotTemplate>({
            url
        });
    }

    /**
     * Get the template filtered by tittle or inode .
     *
     * @param {DotTemplatesRequestOptions} options
     * @returns {Observable<DotTemplate>}
     * @memberof DotTemplatesService
     */
    getFiltered(options: DotTemplatesRequestOptions): Observable<DotTemplate[]> {
        const url = `${TEMPLATE_API_URL}`;
        const per_page = options.per_page ?? DEFAULT_PER_PAGE;
        const page = options.page ?? DEFAULT_PAGE;
        const orderby = options.orderby ?? DEFAULT_ORDERBY;
        const direction = options.direction ?? DEFAULT_DIRECTION;
        const archive = options.archive ?? DEFAULT_ARCHIVE;
        const filter = options.filter;

        const params = new HttpParams()
            .set('per_page', per_page.toString())
            .set('page', page.toString())
            .set('orderby', orderby.toString())
            .set('direction', direction.toString())
            .set('archive', archive.toString())
            .set('filter', filter.toString());

        return this.request<DotTemplate[]>({
            url,
            params
        });
    }

    /**
     * Creates a template
     *
     * @param {DotTemplate} values
     * @return Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    create(values: DotTemplate): Observable<DotTemplate> {
        return this.request<DotTemplate>({
            method: 'POST',
            url: TEMPLATE_API_URL,
            body: values
        });
    }

    /**
     * Updates a template
     * @returns Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    update(values: DotTemplate): Observable<DotTemplate> {
        return this.request<DotTemplate>({
            method: 'PUT',
            url: TEMPLATE_API_URL,
            body: values
        });
    }

    /**
     * Save and Publish a template
     * @param {DotTemplate} values
     * @returns Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    saveAndPublish(values: DotTemplate): Observable<DotTemplate> {
        return this.http
            .put<DotTemplate>(`${TEMPLATE_API_URL}_savepublish`, {
                ...values
            })
            .pipe(pluck('entity'));
    }

    /**
     * Delete a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    delete(identifiers: string[]): Observable<DotActionBulkResult> {
        return this.request<DotActionBulkResult>({
            method: 'DELETE',
            url: TEMPLATE_API_URL,
            body: identifiers
        });
    }

    /**
     * Unarchive a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    unArchive(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${TEMPLATE_API_URL}_unarchive`;

        return this.request<DotActionBulkResult>({
            method: 'PUT',
            url,
            body: identifiers
        });
    }

    /**
     * Archive a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    archive(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${TEMPLATE_API_URL}_archive`;

        return this.request<DotActionBulkResult>({
            method: 'PUT',
            url,
            body: identifiers
        });
    }

    /**
     * Unpublish a template00
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    unPublish(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${TEMPLATE_API_URL}_unpublish`;

        return this.request<DotActionBulkResult>({
            method: 'PUT',
            url,
            body: identifiers
        });
    }

    /**
     * Publish a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    publish(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${TEMPLATE_API_URL}_publish`;

        return this.request<DotActionBulkResult>({
            method: 'PUT',
            url,
            body: identifiers
        });
    }

    /**
     * Copy a template
     * @param {string} identifier
     * @returns Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    copy(identifier: string): Observable<DotTemplate> {
        const url = `${TEMPLATE_API_URL}${identifier}/_copy`;

        return this.request<DotTemplate>({ method: 'PUT', url });
    }

    private request<T>(options: DotRequestOptionsArgs): Observable<T> {
        const response$ = this.http.request<T>(options.method || 'GET', options.url, {
            body: options?.body,
            params: options?.params,
            headers: options?.headers
        });

        return response$.pipe(
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
