import { Observable, of, BehaviorSubject } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, switchMap, filter } from 'rxjs/operators';

import { DotConfigurationVariables, DotContainer } from '@dotcms/dotcms-models';

import { DotPropertiesService } from '../dot-properties/dot-properties.service';
import { PaginatorService } from '../paginator/paginator.service';

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
 * The containers that use a content type: the page of containers returned and how many use it in
 * all, so a caller can tell when the page is not the whole list.
 */
export interface DotContainersByContentType {
    /** The containers returned, at most the page size asked for. */
    containers: DotContainer[];
    /** How many containers use the content type in all. */
    total: number;
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
                    if (typeof title !== 'string') {
                        return of(null);
                    }

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
        const url = `${this.#CONTAINER_API_URL}?filter=${filter}&per_page=${perPage}&system=${fetchSystemContainers}`;

        return this.#http
            .get<{ entity: DotContainer[] }>(url)
            .pipe(map((response) => response.entity));
    }

    /**
     * Get the containers that use the given content type, archived ones included.
     *
     * The total comes from the `X-Pagination-Total-Entries` header; when the header is missing
     * the total is the number of containers returned.
     *
     * @param {string} contentTypeId id of the content type
     * @param {number} [perPage=100] maximum number of containers to return
     * @return {*}  {Observable<DotContainersByContentType>} the containers (an empty list when none
     * use it) and how many use it in all
     * @memberof DotContainersService
     */
    getByContentType(contentTypeId: string, perPage = 100): Observable<DotContainersByContentType> {
        const url = `${this.#CONTAINER_API_URL}?content_type=${encodeURIComponent(contentTypeId)}&per_page=${perPage}&archive=true`;

        return this.#http.get<{ entity?: DotContainer[] }>(url, { observe: 'response' }).pipe(
            map((response) => {
                const containers = response.body?.entity ?? [];
                const total = Number(
                    response.headers.get(PaginatorService.PAGINATION_TOTAL_ENTRIES_HEADER_NAME)
                );

                return {
                    containers,
                    total: Number.isFinite(total) && total > 0 ? total : containers.length
                };
            })
        );
    }

    /**
     * Get the container filtered by title.
     *
     * @param {string} title
     * @return {*}  {Observable<DotContainer>}
     * @memberof DotContainersService
     */
    getContainerByTitle(title: string, system = false): Observable<DotContainer | null> {
        const url = `${this.#CONTAINER_API_URL}?filter=${title}&per_page=1&system=${system}`;

        return this.#http.get<{ entity: DotContainer[] }>(url).pipe(
            map((response) => response.entity),
            map((containers) => containers?.[0] || null)
        );
    }
}
