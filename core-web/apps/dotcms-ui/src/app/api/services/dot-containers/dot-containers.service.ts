import { Observable } from 'rxjs';

import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import {
    DotActionBulkResult,
    DotCMSResponse,
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
    private http = inject(HttpClient);
    private httpErrorManagerService = inject(DotHttpErrorManagerService);

    /**
     * Return a list of containers.
     * @returns Observable<DotContainer[]>
     * @memberof DotContainersService
     */
    get(): Observable<DotContainerEntity[]> {
        return this.http.get<DotCMSResponse<DotContainerEntity[]>>(CONTAINER_API_URL).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotContainerEntity[]>(error))
        );
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

        return this.http.get<DotCMSResponse<DotContainerEntity>>(url).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotContainerEntity>(error))
        );
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

        return this.http.get<DotCMSResponse<DotContainerEntity[]>>(url).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotContainerEntity[]>(error))
        );
    }

    /**
     * Creates a container
     * @param {DotContainerRequest} values
     * @returns Observable<DotContainer>
     * @memberof DotContainersService
     */
    create(values: DotContainerPayload): Observable<DotContainerEntity> {
        return this.http.post<DotCMSResponse<DotContainerEntity>>(CONTAINER_API_URL, values).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotContainerEntity>(error))
        );
    }

    /**
     * Updates a container
     *
     * @param {DotContainerPayload} values
     * @returns Observable<DotContainer>
     * @memberof DotContainersService
     */
    update(values: DotContainerPayload): Observable<DotContainerEntity> {
        return this.http.put<DotCMSResponse<DotContainerEntity>>(CONTAINER_API_URL, values).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotContainerEntity>(error))
        );
    }

    /**
     * Save and Publish a container
     * @param {DotContainer} values
     * @returns Observable<DotContainer>
     * @memberof DotContainersService
     */
    saveAndPublish(values: DotContainerEntity): Observable<DotContainerEntity> {
        return this.http
            .put<DotCMSResponse<DotContainerEntity>>(`${CONTAINER_API_URL}_savepublish`, values)
            .pipe(
                map((response) => response.entity),
                catchError((error: HttpErrorResponse) =>
                    this.handleError<DotContainerEntity>(error)
                )
            );
    }

    /**
     * Delete a container
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotContainersService
     */
    delete(identifiers: string[]): Observable<DotActionBulkResult> {
        return this.http
            .delete<DotCMSResponse<DotActionBulkResult>>(`${CONTAINER_API_URL}_bulkdelete`, {
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
     * Unarchive a container
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotContainersService
     */
    unArchive(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${CONTAINER_API_URL}_bulkunarchive`;

        return this.http.put<DotCMSResponse<DotActionBulkResult>>(url, identifiers).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotActionBulkResult>(error))
        );
    }

    /**
     * Archive a container
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotContainersService
     */
    archive(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${CONTAINER_API_URL}_bulkarchive`;

        return this.http.put<DotCMSResponse<DotActionBulkResult>>(url, identifiers).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotActionBulkResult>(error))
        );
    }

    /**
     * Unpublish a container
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotContainersService
     */
    unPublish(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${CONTAINER_API_URL}_bulkunpublish`;

        return this.http.put<DotCMSResponse<DotActionBulkResult>>(url, identifiers).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotActionBulkResult>(error))
        );
    }

    /**
     * Publish a container
     * @param {string[]} identifiers
     * @returns Observable<DotActionBulkResult>
     * @memberof DotContainersService
     */
    publish(identifiers: string[]): Observable<DotActionBulkResult> {
        const url = `${CONTAINER_API_URL}_bulkpublish`;

        return this.http.put<DotCMSResponse<DotActionBulkResult>>(url, identifiers).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotActionBulkResult>(error))
        );
    }

    /**
     * Copy a container
     * @param {string} identifier
     * @returns Observable<DotContainer>
     * @memberof DotContainersService
     */
    copy(identifier: string): Observable<DotContainer> {
        const url = `${CONTAINER_API_URL}${identifier}/_copy`;

        return this.http.put<DotCMSResponse<DotContainer>>(url, {}).pipe(
            map((response) => response.entity),
            catchError((error: HttpErrorResponse) => this.handleError<DotContainer>(error))
        );
    }

    private handleError<T>(error: HttpErrorResponse): Observable<T> {
        return this.httpErrorManagerService.handle(error).pipe(
            take(1),
            map(() => null)
        );
    }
}
