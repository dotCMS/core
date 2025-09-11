import { Observable, Subject } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, pluck, switchMap } from 'rxjs/operators';

import { DotConfigurationVariables, DotContainer } from '@dotcms/dotcms-models';

import { DotPropertiesService } from '../dot-properties/dot-properties.service';

export const CONTAINER_API_URL = '/api/v1/containers/';
/**
 * Provide util methods to handle containers in the system.
 * @export
 * @class DotContainersService
 */
@Injectable({
    providedIn: 'root'
})
export class DotContainersService {
    private readonly http = inject(HttpClient);
    private readonly dotConfigurationService = inject(DotPropertiesService);

    private readonly DOT_DEFAULT_CONTAINER = DotConfigurationVariables.DOT_DEFAULT_CONTAINER;
    private readonly _defaultContainer$ = new Subject<DotContainer | null>();

    get defaultContainer$() {
        return this._defaultContainer$.asObservable();
    }

    constructor() {
        this.dotConfigurationService
            .getKey(this.DOT_DEFAULT_CONTAINER)
            .pipe(
                switchMap((containerTitle) => {
                    return this.getContainerByTitle(containerTitle);
                })
            )
            .subscribe((container) => {
                this._defaultContainer$.next(container);
            });
    }

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

    /**
     * Get the container filtered by title.
     *
     * @param {string} title
     * @return {*}  {Observable<DotContainer>}
     * @memberof DotContainersService
     */
    getContainerByTitle(title: string): Observable<DotContainer | null> {
        const url = `${CONTAINER_API_URL}?filter=${title}&perPage=1`;

        return this.http.get<{ entity: DotContainer[] }>(url).pipe(
            pluck('entity'),
            map((containers) => containers[0])
        );
    }
}
