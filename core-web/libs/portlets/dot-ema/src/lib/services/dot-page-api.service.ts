import { BehaviorSubject, Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { pluck } from 'rxjs/operators';

import { AddContentletPayload } from '../shared/models';

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
    readonly currentPageContainers$: BehaviorSubject<AddContentletPayload> = new BehaviorSubject({
        pageContainers: []
    });

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
     * Modifies the current page and saves it
     *
     * @param {string} contentletID
     * @memberof DotPageApiService
     */
    save(contentletID: string, pageID: string) {
        const { pageContainers, container } = this.currentPageContainers$.getValue();

        const newPage = pageContainers.map((currentContainer) => {
            if (
                container.uuid === currentContainer.uuid &&
                container.identifier === currentContainer.identifier
            ) {
                currentContainer.contentletsId.find((id) => id === contentletID)
                    ? console.error('this already exists')
                    : currentContainer.contentletsId.push(contentletID);
            }

            return currentContainer;
        });

        return this.http.post(`/api/v1/page/${pageID}/content?variantName=DEFAULT`, newPage);
    }
}
