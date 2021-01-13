import { catchError, map, pluck, take } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { Observable } from 'rxjs';
import { CoreWebService, DotRequestOptionsArgs } from 'dotcms-js';

import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotTemplate } from '@shared/models/dot-edit-layout-designer';
import { DotActionBulkResult } from '@models/dot-action-bulk-result/dot-action-bulk-result.model';

export const TEMPLATE_API_URL = '/api/v1/templates/';
/**
 * Provide util methods to handle templates in the system.
 * @export
 * @class DotTemplatesService
 */
@Injectable()
export class DotTemplatesService {
    constructor(
        private coreWebService: CoreWebService,
        private httpErrorManagerService: DotHttpErrorManagerService
    ) {}

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
     * @param {string} filter
     * @returns {Observable<DotTemplate>}
     * @memberof DotTemplatesService
     */
    getFiltered(filter: string): Observable<DotTemplate[]> {
        const url = `${TEMPLATE_API_URL}?filter=${filter}`;
        return this.request<DotTemplate[]>({
            url
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
        return this.request<DotTemplate>({ method: 'POST', url: TEMPLATE_API_URL, body: values });
    }

    /**
     * Updates a template
     * @returns Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    update(values: DotTemplate): Observable<DotTemplate> {
        return this.request<DotTemplate>({ method: 'PUT', url: TEMPLATE_API_URL, body: values });
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
        return this.request<DotActionBulkResult>({ method: 'PUT', url, body: identifiers });
    }

    /**
     * Archive a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    archive(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${TEMPLATE_API_URL}_archive`;
        return this.request<DotActionBulkResult>({ method: 'PUT', url, body: identifiers });
    }

    /**
     * Unpublish a template00
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    unPublish(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${TEMPLATE_API_URL}_unpublish`;
        return this.request<DotActionBulkResult>({ method: 'PUT', url, body: identifiers });
    }

    /**
     * Publish a template
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotTemplatesService
     */
    publish(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${TEMPLATE_API_URL}_publish`;
        return this.request<DotActionBulkResult>({ method: 'PUT', url, body: identifiers });
    }

    /**
     * Copy a template
     * @param {string} identifier
     * @returns Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    copy(identifier: string): Observable<DotTemplate> {
        const url = `${TEMPLATE_API_URL}${identifier}/_copy`;
        return this.request<any>({ method: 'PUT', url });
    }

    private request<T>(options: DotRequestOptionsArgs): Observable<T> {
        const response$ = this.coreWebService.requestView<T>(options);
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
