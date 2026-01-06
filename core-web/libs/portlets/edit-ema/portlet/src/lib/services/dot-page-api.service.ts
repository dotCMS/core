import { EMPTY, Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, pluck } from 'rxjs/operators';

import { graphqlToPageEntity } from '@dotcms/client/internal';
import { DEFAULT_VARIANT_ID, DotPersona, DotPagination } from '@dotcms/dotcms-models';
import { DotCMSGraphQLPage, DotCMSPageAsset, UVE_MODE } from '@dotcms/types';

import { PERSONA_KEY } from '../shared/consts';
import { DotPageAssetParams, SavePagePayload, SaveStylePropertiesPayload } from '../shared/models';
import { getFullPageURL } from '../utils';

export interface DotPageApiParams {
    url: string;
    depth?: string;
    mode?: UVE_MODE;
    language_id: string;
    [PERSONA_KEY]: string;
    variantName?: string;
    experimentId?: string;
    clientHost?: string;
    publishDate?: string;
    // We need this to allow any other query param to be passed by the user
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    [x: string]: any;
}

export enum DotPageAssetKeys {
    URL = 'url',
    MODE = 'mode',
    DEPTH = 'depth',
    CLIENT_HOST = 'clientHost',
    VARIANT_NAME = 'variantName',
    LANGUAGE_ID = 'language_id',
    EXPERIMENT_ID = 'experimentId',
    PERSONA_ID = 'com.dotmarketing.persona.id',
    PUBLISH_DATE = 'publishDate'
}

export interface GetPersonasParams {
    pageId: string;
    filter?: string;
    page?: number;
    perPage?: number;
}

export interface GetPersonasResponse {
    data: DotPersona[];
    pagination: DotPagination;
}

@Injectable()
export class DotPageApiService {
    private http = inject(HttpClient);

    /**
     * Get a page from the Page API
     *
     * @param {DotPageApiParams} { url, language_id }
     * @return {*}  {Observable<DotCMSPageAsset>}
     * @memberof DotPageApiService
     */
    get(queryParams: DotPageAssetParams): Observable<DotCMSPageAsset> {
        const { clientHost, ...params } = queryParams;
        const pageType = clientHost ? 'json' : 'render';
        const pageURL = getFullPageURL({ url: params.url, params });

        return this.http
            .get<{
                entity: DotCMSPageAsset;
            }>(`/api/v1/page/${pageType}/${pageURL}`)
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
     * Save style properties for a specific contentlet within a container on a page.
     *
     * @param {SaveStylePropertiesPayload} payload - The payload for saving style properties.
     * @param {string} payload.containerIdentifier - Identifier of the container.
     * @param {string} payload.contentletIdentifier - Identifier of the contentlet.
     * @param {Record<string, unknown>} payload.styleProperties - Style properties to apply.
     * @param {string} payload.pageId - The page ID where styles are being saved.
     * @param {string} payload.containerUUID - UUID of the container.
     * @returns {Observable<unknown>} Observable that completes when properties are saved.
     * @memberof DotPageApiService
     */
    saveStyleProperties({
        containerIdentifier,
        contentletIdentifier,
        styleProperties,
        pageId,
        containerUUID
    }: SaveStylePropertiesPayload): Observable<unknown> {
        const payload = {
            identifier: containerIdentifier,
            uuid: containerUUID,
            contentletsId: [contentletIdentifier],
            styleProperties: {
                [contentletIdentifier]: styleProperties
            }
        };

        return this.http
            .post(`/api/v1/page/${pageId}/content`, [payload])
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

        return this.http.get<{ entity: DotPersona[]; pagination: DotPagination }>(url).pipe(
            map((res: { entity: DotPersona[]; pagination: DotPagination }) => ({
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
            .get<{
                entity: { content: { idenfitier: string } };
            }>(`/api/v1/containers/form/${formId}?containerId=${containerId}`)
            .pipe(pluck('entity', 'content', 'identifier'));
    }

    /**
     *
     * @description Save a contentlet in a page
     * @param {{ contentlet: { [fieldName: string]: string; inode: string } }} { contentlet }
     * @return {*}
     * @memberof DotPageApiService
     */
    saveContentlet({ contentlet }: { contentlet: { [fieldName: string]: string; inode: string } }) {
        // indexPolicy=WAIT_FOR ensures the contentlet is indexed before returning, preventing stale data.
        // Note: We'll replace this with optimistic updates for better UX.
        return this.http.put(
            `/api/v1/workflow/actions/default/fire/EDIT?inode=${contentlet.inode}&indexPolicy=WAIT_FOR`,
            { contentlet }
        );
    }

    /**
     *
     * @description Get a page from GraphQL
     * @template T
     * @param {string} query
     * @return {*}  {Observable<T>}
     * @memberof DotPageApiService
     */
    getGraphQLPage({
        query,
        variables
    }: {
        query: string;
        variables: Record<string, string>;
    }): Observable<{
        pageAsset: DotCMSPageAsset;
        content: Record<string, unknown>;
    }> {
        const headers = {
            'Content-Type': 'application/json',
            dotcachettl: '0'
        };

        return this.http
            .post<{
                data: { page: DotCMSGraphQLPage };
            }>('/api/v1/graphql', { query, variables }, { headers })
            .pipe(
                pluck('data'),
                map(({ page, ...content }) => {
                    const pageEntity = graphqlToPageEntity(page);

                    return {
                        pageAsset: pageEntity,
                        content
                    };
                })
            );
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
}
