import { EMPTY, Observable, iif, throwError, timer } from 'rxjs';

import { HttpClient, HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, retry } from 'rxjs/operators';

import { graphqlToPageEntity } from '@dotcms/client/internal';
import { DEFAULT_VARIANT_ID, DotPagination, DotPersona } from '@dotcms/dotcms-models';
import { DotCMSGraphQLPage, DotCMSPageAsset, UVE_MODE } from '@dotcms/types';

import { DEFAULT_PAGE_DEPTH, PERSONA_KEY } from '../../shared/consts';
import {
    DotPageAssetParams,
    SavePagePayload,
    SaveStylePropertiesPayload
} from '../../shared/models';
import { getFullPageURL } from '../../utils';

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

/** What the container form endpoints answer: the form placed in a container, with its identifier. */
interface DotFormInContainerResponse {
    entity: { content: { identifier: string } };
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
        const { clientHost, depth, ...rest } = queryParams;
        const pageType = clientHost ? 'json' : 'render';
        // The backend skips relationship expansion entirely when `depth` is
        // absent from the request (as opposed to falling back to a default),
        // so we must always send a value — see DEFAULT_PAGE_DEPTH.
        const params = { ...rest, depth: depth ?? DEFAULT_PAGE_DEPTH };
        const pageURL = getFullPageURL({ url: params.url, params });

        return this.http
            .get<{
                entity: DotCMSPageAsset;
            }>(`/api/v1/page/${pageType}/${pageURL}`)
            .pipe(map((x) => x?.entity));
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
     * @param {string} [payload.personaTag] - Persona key tag to personalize this style update for.
     * @returns {Observable<unknown>} Observable that completes when properties are saved.
     * @memberof DotPageApiService
     */
    saveStyleProperties({
        containerIdentifier,
        contentletIdentifier,
        styleProperties,
        pageId,
        containerUUID,
        personaTag
    }: SaveStylePropertiesPayload): Observable<unknown> {
        const payload = {
            identifier: containerIdentifier,
            uuid: containerUUID,
            personaTag,
            [contentletIdentifier]: styleProperties
        };

        return this.http.put(`/api/v1/page/${pageId}/styles`, [payload]);
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
     * Gets the identifier a form is placed in containers with. A form placed for the first time has
     * none yet, so when the lookup answers 404 the identifier is asked to be created. Looking up
     * first keeps this working against a server whose lookup still creates it.
     *
     * @param {string} containerId the container the form goes into
     * @param {string} formId the form's content type id
     * @return {*}  {Observable<string>} the identifier to place in the container
     * @memberof DotPageApiService
     */
    getFormIndetifier(containerId: string, formId: string): Observable<string> {
        const url = `/api/v1/containers/form/${formId}?containerId=${containerId}`;

        // A 409 means another request is still placing the same form; asking again shortly after
        // gets the identifier it created.
        const place$ = this.http.post<DotFormInContainerResponse>(url, null).pipe(
            retry({
                count: 1,
                delay: (error: HttpErrorResponse) =>
                    error.status === HttpStatusCode.Conflict ? timer(1000) : throwError(() => error)
            })
        );

        return this.http.get<DotFormInContainerResponse>(url).pipe(
            catchError((error: HttpErrorResponse) =>
                iif(
                    () => error.status === HttpStatusCode.NotFound,
                    place$,
                    throwError(() => error)
                )
            ),
            map((response) => response?.entity?.content?.identifier)
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
                map((x) => x?.data),
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
