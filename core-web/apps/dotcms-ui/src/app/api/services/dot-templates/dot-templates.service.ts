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
    getById(id: string, version = 'working'): Observable<DotTemplate> {
        const url = `${TEMPLATE_API_URL}${id}/${version}`;

        return this.http.get<DotCMSResponse<DotTemplate>>(url).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotTemplate>(error))
        );
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

        return this.http.get<DotCMSResponse<DotTemplate[]>>(url).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotTemplate[]>(error))
        );
    }

    /**
     * Creates a template
     *
     * @param {DotTemplate} values
     * @return Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    create(values: DotTemplate): Observable<DotTemplate> {
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
    update(values: DotTemplate): Observable<DotTemplate> {
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
    saveAndPublish(values: DotTemplate): Observable<DotTemplate> {
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
    delete(identifiers: string[]): Observable<DotActionBulkResult> {
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
    unArchive(identifiers: string[]): Observable<DotActionBulkResult> {
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
    archive(identifiers: string[]): Observable<DotActionBulkResult> {
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
    unPublish(identifiers: string[]): Observable<DotActionBulkResult> {
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
    publish(identifiers: string[]): Observable<DotActionBulkResult> {
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
    copy(identifier: string): Observable<DotTemplate> {
        const url = `${TEMPLATE_API_URL}${identifier}/_copy`;

        return this.http.put<DotCMSResponse<DotTemplate>>(url, {}).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotTemplate>(error))
        );
    }

    private handleError<T>(error: HttpErrorResponse): Observable<T> {
        return this.httpErrorManagerService.handle(error).pipe(
            take(1),
            map(() => null)
        );
    }
}
