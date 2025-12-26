import { Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { CoreWebService, DotRequestOptionsArgs } from '@dotcms/dotcms-js';
import {
    DotActionBulkResult,
    DotContainer,
    DotContainerEntity,
    DotContainerPayload
} from '@dotcms/dotcms-models';

export const CONTAINER_API_URL = '/api/v1/containers/';

/**
 * Provide util methods to handle containers in the system.
 * @export
 * @class DotContainersService
 * @deprecated use DotContainersService from the data-access lib instead
 */
@Injectable()
export class DotContainersService {
    private coreWebService = inject(CoreWebService);
    private httpErrorManagerService = inject(DotHttpErrorManagerService);

    /**
     * Return a list of containers.
     * @returns Observable<DotContainer[]>
     * @memberof DotContainersService
     */
    get(): Observable<DotContainerEntity[]> {
        return this.request<DotContainerEntity[]>({ url: CONTAINER_API_URL });
    }

    /**
     * Get the container, pass the version default working, pass the includeContentType default false
     * @param {string} id
     * @param {string} version
     * @param {boolean} includeContentType
     * @returns {Observable<DotContainerEntity>}
     * @memberof DotContainersService
     */
    getById(
        id: string,
        version = 'working',
        includeContentType = false
    ): Observable<DotContainerEntity> {
        const url = `${CONTAINER_API_URL}${version}?containerId=${id}${
            includeContentType ? `&includeContentType=${includeContentType}` : ''
        }`;

        return this.request<DotContainerEntity>({
            url
        });
    }

    /**
     * Get the container filtered by tittle or inode .
     *
     * @param {string} filter
     * @returns {Observable<DotContainerEntity>}
     * @memberof DotContainersService
     */
    getFiltered(filter: string): Observable<DotContainerEntity[]> {
        const url = `${CONTAINER_API_URL}?filter=${filter}`;

        return this.request<DotContainerEntity[]>({
            url
        });
    }

    /**
     * Creates a container
     * @param {DotContainerRequest} values
     * @returns Observable<DotContainer>
     * @memberof DotContainersService
     */

    create(values: DotContainerPayload): Observable<DotContainerEntity> {
        return this.request<DotContainerEntity>({
            method: 'POST',
            url: CONTAINER_API_URL,
            body: values
        });
    }

    /**
     * Updates a container
     *
     * @param {DotContainerPayload} values
     * @returns Observable<DotContainer>
     * @memberof DotContainersService
     */
    update(values: DotContainerPayload): Observable<DotContainerEntity> {
        return this.request<DotContainerEntity>({
            method: 'PUT',
            url: CONTAINER_API_URL,
            body: values
        });
    }

    /**
     * Save and Publish a container
     * @param {DotContainer} values
     * @returns Observable<DotContainer>
     * @memberof DotContainersService
     */
    saveAndPublish(values: DotContainerEntity): Observable<DotContainerEntity> {
        return this.request<DotContainerEntity>({
            method: 'PUT',
            url: `${CONTAINER_API_URL}_savepublish`,
            body: values
        });
    }

    /**
     * Delete a container
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotContainersService
     */
    delete(identifiers: string[]): Observable<DotActionBulkResult> {
        return this.request<DotActionBulkResult>({
            method: 'DELETE',
            url: `${CONTAINER_API_URL}_bulkdelete`,
            body: identifiers
        });
    }

    /**
     * Unarchive a container
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotContainersService
     */
    unArchive(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${CONTAINER_API_URL}_bulkunarchive`;

        return this.request<DotActionBulkResult>({
            method: 'PUT',
            url,
            body: identifiers
        });
    }

    /**
     * Archive a container
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotContainersService
     */
    archive(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${CONTAINER_API_URL}_bulkarchive`;

        return this.request<DotActionBulkResult>({
            method: 'PUT',
            url,
            body: identifiers
        });
    }

    /**
     * Unpublish a container00
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotContainersService
     */
    unPublish(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${CONTAINER_API_URL}_bulkunpublish`;

        return this.request<DotActionBulkResult>({
            method: 'PUT',
            url,
            body: identifiers
        });
    }

    /**
     * Publish a container
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotContainersService
     */
    publish(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${CONTAINER_API_URL}_bulkpublish`;

        return this.request<DotActionBulkResult>({
            method: 'PUT',
            url,
            body: identifiers
        });
    }

    /**
     * Copy a container
     * @param {string} identifier
     * @returns Observable<DotContainer>
     * @memberof DotContainersService
     */
    copy(identifier: string): Observable<DotContainer> {
        const url = `${CONTAINER_API_URL}${identifier}/_copy`;

        return this.request<DotContainer>({ method: 'PUT', url });
    }

    private request<T>(options: DotRequestOptionsArgs): Observable<T> {
        const response$ = this.coreWebService.requestView<T>(options);

        return response$.pipe(
            map((x) => x?.entity),
            catchError((error: HttpErrorResponse) => {
                return this.httpErrorManagerService.handle(error).pipe(
                    take(1),
                    map(() => null)
                );
            })
        );
    }
}
