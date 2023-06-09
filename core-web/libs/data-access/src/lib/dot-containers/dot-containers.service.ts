import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { CoreWebService, DotRequestOptionsArgs } from '@dotcms/dotcms-js';
import { DotContainerEntity } from '@dotcms/dotcms-models';

export const CONTAINER_API_URL = '/api/v1/containers/';

/**
 * Provide util methods to handle containers in the system.
 * @export
 * @class DotContainersService
 */
@Injectable()
export class DotContainersService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get the container filtered by tittle or inode .
     *
     * @param {string} filter
     * @returns {Observable<DotContainerEntity>}
     * @memberof DotContainersService
     */
    getFiltered(filter: string, perPage: number): Observable<DotContainerEntity[]> {
        const url = `${CONTAINER_API_URL}?filter=${filter}&perPage=${perPage}`;

        return this.request<DotContainerEntity[]>({
            url
        });
    }

    private request<T>(options: DotRequestOptionsArgs): Observable<T> {
        const response$ = this.coreWebService.requestView<T>(options);

        return response$.pipe(pluck('entity'));
    }
}
