import { catchError, map, pluck, take } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { Observable } from 'rxjs';
import { CoreWebService, DotRequestOptionsArgs } from 'dotcms-js';

import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotTemplate } from '@shared/models/dot-edit-layout-designer';

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
        const url = '/api/v1/templates';
        return this.request<DotTemplate[]>({ url });
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
        const url = `/api/v1/templates/${id}/${version}`;

        return this.request<DotTemplate>({
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
        const url = '/api/v1/templates';
        return this.request<DotTemplate>({ method: 'POST', url, body: values });
    }

    /**
     * Updates a template
     * @returns Observable<DotTemplate>
     * @memberof DotTemplatesService
     */
    update(values: DotTemplate): Observable<DotTemplate> {
        const url = '/api/v1/templates';

        return this.request<DotTemplate>({ method: 'PUT', url, body: values });
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
