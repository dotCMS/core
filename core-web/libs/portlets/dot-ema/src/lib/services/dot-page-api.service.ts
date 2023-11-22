import { EMPTY, Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { catchError, pluck } from 'rxjs/operators';

import { Container, SavePagePayload } from '../shared/models';

export interface DotPageApiResponse {
    page: {
        title: string;
        identifier: string;
    };
}

export interface DotPageApiParams {
    url: string;
    language_id: string;
}

@Injectable()
export class DotPageApiService {
    constructor(private http: HttpClient) {}

    /**
     * Get a page from the Page API
     *
     * @param {DotPageApiParams} { url, language_id }
     * @return {*}  {Observable<DotPageApiResponse>}
     * @memberof DotPageApiService
     */
    get({ url, language_id }: DotPageApiParams): Observable<DotPageApiResponse> {
        const apiUrl = `/api/v1/page/json/${url}?language_id=${language_id}`;

        return this.http
            .get<{
                entity: DotPageApiResponse;
            }>(apiUrl)
            .pipe(pluck('entity'));
    }

    /**
     * Save a contentlet in a page
     *
     * @param {SavePagePayload} { pageContainers, container, contentletID, pageID }
     * @return {*}
     * @memberof DotPageApiService
     */
    save({
        pageContainers,
        container,
        contentletID,
        pageID
    }: SavePagePayload): Observable<unknown> {
        const newPage = this.insertContentletInContainer({
            pageContainers,
            container,
            contentletID
        });

        return this.http
            .post(`/api/v1/page/${pageID}/content`, newPage)
            .pipe(catchError(() => EMPTY));
    }

    /**
     * Insert a contentlet in a container
     *
     * @private
     * @param {{
     *         pageContainers: Container[];
     *         container: Container;
     *         contentletID: string;
     *     }} {
     *         pageContainers,
     *         container,
     *         contentletID
     *     }
     * @return {*}
     * @memberof DotPageApiService
     */
    private insertContentletInContainer({
        pageContainers,
        container,
        contentletID
    }: {
        pageContainers: Container[];
        container: Container;
        contentletID: string;
    }): Container[] {
        return pageContainers.map((currentContainer) => {
            if (
                container.identifier === currentContainer.identifier &&
                container.uuid === currentContainer.uuid
            ) {
                !currentContainer.contentletsId.find((id) => id === contentletID) &&
                    currentContainer.contentletsId.push(contentletID);
            }

            return currentContainer;
        });
    }
}
