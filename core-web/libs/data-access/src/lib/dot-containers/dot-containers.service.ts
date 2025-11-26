import { Observable, of, BehaviorSubject } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, switchMap, filter } from 'rxjs/operators';

import { DotConfigurationVariables, DotContainer } from '@dotcms/dotcms-models';

import { DotPropertiesService } from '../dot-properties/dot-properties.service';

/**
 * The id of the system container.
 */
export const SYSTEM_CONTAINER_ID = 'SYSTEM_CONTAINER';

/**
 * Constants for default container configuration values
 */
const DEFAULT_CONTAINER_CONFIG = {
    NOT_FOUND: 'NOT_FOUND',
    NULL_VALUE: 'null'
};

interface DefaultContainerSubject {
    container: DotContainer | null;
    isInitial: boolean;
}

/**
 * Provide util methods to handle containers in the system.
 * @export
 * @class DotContainersService
 */
@Injectable({
    providedIn: 'root'
})
export class DotContainersService {
    readonly #http = inject(HttpClient);
    readonly #dotConfigurationService = inject(DotPropertiesService);

    readonly #DEFAULT_CONTAINER_KEY = DotConfigurationVariables.DEFAULT_CONTAINER;
    readonly #CONTAINER_API_URL = '/api/v1/containers/';

    readonly #defaultContainer$ = new BehaviorSubject<DefaultContainerSubject>({
        container: null,
        isInitial: true
    });

    get defaultContainer$(): Observable<DotContainer | null> {
        return this.#defaultContainer$.asObservable().pipe(
            filter(({ isInitial }) => !isInitial),
            map(({ container }) => container)
        );
    }

    constructor() {
        this.#dotConfigurationService
            .getKey(this.#DEFAULT_CONTAINER_KEY)
            .pipe(
                switchMap((title) => {
                    const isNotSet = title === DEFAULT_CONTAINER_CONFIG.NOT_FOUND;

                    if (!title || title === DEFAULT_CONTAINER_CONFIG.NULL_VALUE) {
                        return of(null);
                    }

                    const searchTitle = isNotSet ? SYSTEM_CONTAINER_ID : title;
                    return this.getContainerByTitle(searchTitle, isNotSet);
                })
            )
            .subscribe((container) => {
                this.#defaultContainer$.next({ container, isInitial: false });
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
        const url = `${this.#CONTAINER_API_URL}?filter=${filter}&perPage=${perPage}&system=${fetchSystemContainers}`;

        return this.#http
            .get<{ entity: DotContainer[] }>(url)
            .pipe(map((response) => response.entity));
    }

    /**
     * Get the container filtered by title.
     *
     * @param {string} title
     * @return {*}  {Observable<DotContainer>}
     * @memberof DotContainersService
     */
    getContainerByTitle(title: string, system = false): Observable<DotContainer | null> {
        const url = `${this.#CONTAINER_API_URL}?filter=${title}&perPage=1&system=${system}`;

        return this.#http.get<{ entity: DotContainer[] }>(url).pipe(
            map((response) => response.entity),
            map((containers) => containers?.[0] || null)
        );
    }
}
