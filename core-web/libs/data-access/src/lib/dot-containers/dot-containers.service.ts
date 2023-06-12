import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { DotContainerEntity } from '@dotcms/dotcms-models';

export const CONTAINER_API_URL = '/api/v1/containers/';

/**
 * Provide util methods to handle containers in the system.
 * @export
 * @class DotContainersService
 */
@Injectable()
export class DotContainersService {
    constructor(private readonly http: HttpClient) {}

    /**
     * Get the container filtered by tittle or inode .
     *
     * @param {string} filter
     * @param {number} perPage
     *
     * @returns {Observable<DotContainerEntity>}
     * @memberof DotContainersService
     */
    getFiltered(filter: string, perPage: number): Observable<DotContainerEntity[]> {
        const url = `${CONTAINER_API_URL}?filter=${filter}&perPage=${perPage}`;

        return this.http.get(url).pipe(pluck('entity'));
    }
}
