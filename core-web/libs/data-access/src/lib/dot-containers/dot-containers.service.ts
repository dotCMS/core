import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { DotContainer } from '@dotcms/dotcms-models';

export const CONTAINER_API_URL = '/api/v1/containers/';
/**
 * Provide util methods to handle containers in the system.
 * @export
 * @class DotContainersService
 */
@Injectable()
export class DotContainersService {
    private readonly http = inject(HttpClient);

    /**
     * Get the container filtered by tittle or inode.
     *
     * @param {string} filter
     * @param {number} perPage
     * @param {boolean} [fetchSystemContainers=false]
     * @return {*}  {Observable<DotContainer[]>}
     * @memberof DotContainersService
     */
    getFiltered(
        filter: string,
        perPage: number,
        fetchSystemContainers = false
    ): Observable<DotContainer[]> {
        const url = `${CONTAINER_API_URL}?filter=${filter}&perPage=${perPage}&system=${fetchSystemContainers}`;

        return this.http.get<{ entity: DotContainer[] }>(url).pipe(pluck('entity'));
    }
}
