import { EMPTY, Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map } from 'rxjs/operators';

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
        // `params` is optional on the payload and its only caller passes `pageParams ?? undefined`,
        // so this read threw a TypeError whenever the store had no params yet. The
        // `?? DEFAULT_VARIANT_ID` fallback beside it was already the answer for that case.
        const variantName = params?.variantName ?? DEFAULT_VARIANT_ID;

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
                entity: { content: { identifier: string } };
            }>(`/api/v1/containers/form/${formId}?containerId=${containerId}`)
            .pipe(map((x) => x?.entity?.content?.identifier));
    }

    /**
     *
     * @description Get a page from GraphQL
     * @template T
     * @param {string} query
     * @return {*}  {Observable<T>}
     * @memberof DotPageApiService
     */
    /**
     * GraphQL variables for the page request. Values are optional because `mode` and `variantName`
     * genuinely can be absent, and `JSON.stringify` — which is what `HttpClient` applies to the body —
     * omits keys whose value is `undefined`. So this is what has always gone over the wire; the old
     * `Record<string, string>` simply did not say so.
     */
    getGraphQLPage({
        query,
        variables
    }: {
        query: string;
        variables: Record<string, string | undefined>;
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
                // `& Record<string, unknown>`: the query asks for the page *and* whatever else the
                // caller put in it, and the `...content` rest below is exactly that remainder. The
                // narrower response type made it an empty object, which is not a `Record`.
                data: { page: DotCMSGraphQLPage } & Record<string, unknown>;
            }>('/api/v1/graphql', { query, variables }, { headers })
            .pipe(
                map((x) => x?.data),
                map(({ page, ...content }) => {
                    const pageAsset = graphqlToPageEntity(page);

                    // `graphqlToPageEntity` returns null when the response carries no page — a URL
                    // that does not resolve. All four consumers feed this straight into the store as
                    // the current page, so a null poisoned it silently. They already have
                    // `catchError` paths for a page that failed to load, and this is one of those.
                    if (!pageAsset) {
                        throw new Error(`GraphQL response contained no page`);
                    }

                    return {
                        pageAsset,
                        content
                    };
                })
            );
    }

    // `perPage` is required here even though it is optional on `GetPersonasParams`: the only caller
    // is `getPersonas`, which defaults it to 10 before calling — the parameter type was re-widening
    // a value that had already been resolved.
    private getPersonasURL({
        pageId,
        filter,
        page,
        perPage
    }: GetPersonasParams & { perPage: number }): string {
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
