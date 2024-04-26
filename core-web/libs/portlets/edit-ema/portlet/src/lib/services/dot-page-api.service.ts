import { EMPTY, Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { catchError, map, pluck } from 'rxjs/operators';

import { Site } from '@dotcms/dotcms-js';
import {
    DEFAULT_VARIANT_ID,
    DotCMSContentlet,
    DotLanguage,
    DotLayout,
    DotPageContainerStructure,
    DotPersona,
    DotTemplate
} from '@dotcms/dotcms-models';

import { SavePagePayload } from '../shared/models';
import { createPageApiUrlWithQueryParams } from '../utils';

export interface DotPageApiResponse {
    page: {
        title: string;
        identifier: string;
        inode: string;
        canEdit: boolean;
        canRead: boolean;
        canLock?: boolean;
        locked?: boolean;
        lockedBy?: string;
        lockedByName?: string;
        pageURI: string;
        rendered?: string;
        contentType: string;
    };
    site: Site;
    viewAs: {
        language: DotLanguage;
        persona?: DotPersona;
    };
    layout: DotLayout;
    template: DotTemplate;
    containers: DotPageContainerStructure;
    urlContentMap?: DotCMSContentlet;
}

export interface DotPageApiParams {
    url: string;
    language_id: string;
    'com.dotmarketing.persona.id': string;
    variantName?: string;
    experimentId?: string;
    mode?: string;
}

export interface GetPersonasParams {
    pageId: string;
    filter?: string;
    page?: number;
    perPage?: number;
}

export interface GetPersonasResponse {
    data: DotPersona[];
    pagination: PaginationData;
}

export interface PaginationData {
    currentPage: number;
    perPage: number;
    totalEntries: number;
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
    get(params: DotPageApiParams & { clientHost?: string }): Observable<DotPageApiResponse> {
        // Remove trailing and leading slashes
        const url = params.url.replace(/^\/+|\/+$/g, '');

        const pageType = params.clientHost ? 'json' : 'render';

        const pageApiUrl = createPageApiUrlWithQueryParams(url, {
            language_id: params.language_id,
            'com.dotmarketing.persona.id': params['com.dotmarketing.persona.id'],
            variantName: params.variantName,
            experimentId: params.experimentId
        });

        const apiUrl = `/api/v1/page/${pageType}/${pageApiUrl}`;

        return this.http
            .get<{
                entity: DotPageApiResponse;
            }>(apiUrl)
            .pipe(pluck('entity'));
    }

    /**
     * Save a contentlet in a page
     *
     * @param {SavePagePayload} { pageContainers, pageId }
     * @return {*}  {Observable<unknown>}
     * @memberof DotPageApiService
     */
    save({ pageContainers, pageId, params }: SavePagePayload): Observable<unknown> {
        const variantName = params.variantName ?? DEFAULT_VARIANT_ID;

        return this.http
            .post(`/api/v1/page/${pageId}/content?variantName=${variantName}`, pageContainers)
            .pipe(catchError(() => EMPTY));
    }

    /**
     * Get the personas from the Page API
     *
     * @param null {}
     * @return {*}  {Observable<DotPersona[]>}
     * @memberof DotPageApiService
     */
    getPersonas({
        pageId,
        filter,
        page,
        perPage = 10
    }: GetPersonasParams): Observable<GetPersonasResponse> {
        const url = this.getPersonasURL({ pageId, filter, page, perPage });

        return this.http.get<{ entity: DotPersona[]; pagination: PaginationData }>(url).pipe(
            map((res: { entity: DotPersona[]; pagination: PaginationData }) => ({
                data: res.entity,
                pagination: res.pagination
            }))
        );
    }

    /**
     * Get form information to add to the page
     *
     * @param {string} containerId
     * @param {string} formId
     * @return {*}  {Observable<{ render: string; content: { [key: string]: string } }>}
     * @memberof DotPageApiService
     */
    getFormIndetifier(containerId: string, formId: string): Observable<string> {
        return this.http
            .get<{ entity: { content: { idenfitier: string } } }>(
                `/api/v1/containers/form/${formId}?containerId=${containerId}`
            )
            .pipe(pluck('entity', 'content', 'identifier'));
    }

    private getPersonasURL({ pageId, filter, page, perPage }: GetPersonasParams): string {
        const apiUrl = `/api/v1/page/${pageId}/personas?`;

        const queryParams = new URLSearchParams({
            perper_page: perPage.toString(),
            respectFrontEndRoles: 'true',
            variantName: 'DEFAULT'
        });

        if (filter) {
            queryParams.set('filter', filter);
        }

        if (page) {
            queryParams.set('page', page.toString());
        }

        return apiUrl + queryParams.toString();
    }

    saveContentlet({ contentlet }: { contentlet: { [fieldName: string]: string; inode: string } }) {
        return this.http.put(
            `/api/v1/workflow/actions/default/fire/EDIT?inode=${contentlet.inode}`,
            { contentlet }
        );
    }
}
