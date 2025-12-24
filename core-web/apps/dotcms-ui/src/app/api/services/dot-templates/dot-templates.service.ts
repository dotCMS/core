import { Observable } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotActionBulkResult, DotCMSResponse, DotTemplate } from '@dotcms/dotcms-models';

export const TEMPLATE_API_URL = '/api/v1/templates/';

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
        return this.request<DotTemplate[]>('GET', TEMPLATE_API_URL);
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

        return this.request<DotTemplate>('GET', url);
    }

    /**
     * Get the template filtered by tittle or inode .
     *
     * @param {string} filter
     * @returns {Observable<DotTemplate>}
     * @memberof DotTemplatesService
     */
    getFiltered(filter: string): Observable<DotTemplate[]> {
        const url = `${TEMPLATE_API_URL}?filter=${filter}`;

        return this.request<DotTemplate[]>('GET', url);
    }

    /**
     * Creates a template
     *
     * @param {DotTemplate} values
     * @return Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    create(values: DotTemplate): Observable<DotTemplate> {
        return this.request<DotTemplate>('POST', TEMPLATE_API_URL, values);
    }

    /**
     * Updates a template
     * @returns Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    update(values: DotTemplate): Observable<DotTemplate> {
        return this.request<DotTemplate>('PUT', TEMPLATE_API_URL, values);
    }

    /**
     * Save and Publish a template
     * @param {DotTemplate} values
     * @returns Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    saveAndPublish(values: DotTemplate): Observable<DotTemplate> {
        return this.http
            .put<DotCMSResponse<DotTemplate>>(`${TEMPLATE_API_URL}_savepublish`, {
                ...values
            })
            .pipe(map((response) => response.entity));
    }

    /**
     * Delete a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    delete(identifiers: string[]): Observable<DotActionBulkResult> {
        return this.request<DotActionBulkResult>('DELETE', TEMPLATE_API_URL, identifiers);
    }

    /**
     * Unarchive a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    unArchive(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${TEMPLATE_API_URL}_unarchive`;

        return this.request<DotActionBulkResult>('PUT', url, identifiers);
    }

    /**
     * Archive a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    archive(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${TEMPLATE_API_URL}_archive`;

        return this.request<DotActionBulkResult>('PUT', url, identifiers);
    }

    /**
     * Unpublish a template00
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    unPublish(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${TEMPLATE_API_URL}_unpublish`;

        return this.request<DotActionBulkResult>('PUT', url, identifiers);
    }

    /**
     * Publish a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    publish(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${TEMPLATE_API_URL}_publish`;

        return this.request<DotActionBulkResult>('PUT', url, identifiers);
    }

    /**
     * Copy a template
     * @param {string} identifier
     * @returns Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    copy(identifier: string): Observable<DotTemplate> {
        const url = `${TEMPLATE_API_URL}${identifier}/_copy`;

        return this.request<DotTemplate>('PUT', url);
    }

    private request<T>(method: string, url: string, body?: unknown): Observable<T> {
        let response$: Observable<DotCMSResponse<T>>;

        switch (method) {
            case 'GET':
                response$ = this.http.get<DotCMSResponse<T>>(url);
                break;
            case 'POST':
                response$ = this.http.post<DotCMSResponse<T>>(url, body);
                break;
            case 'PUT':
                response$ = this.http.put<DotCMSResponse<T>>(url, body || {});
                break;
            case 'DELETE':
                response$ = this.http.request<DotCMSResponse<T>>('DELETE', url, { body });
                break;
            default:
                response$ = this.http.get<DotCMSResponse<T>>(url);
        }

        return response$.pipe(
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
