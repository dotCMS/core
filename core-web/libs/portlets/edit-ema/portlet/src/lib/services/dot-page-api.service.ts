import { EMPTY, Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { catchError, map, pluck } from 'rxjs/operators';

import { graphqlToPageEntity } from '@dotcms/client';
import { Site } from '@dotcms/dotcms-js';
import {
    DEFAULT_VARIANT_ID,
    DotCMSContentlet,
    DotLanguage,
    DotLayout,
    DotPageContainerStructure,
    DotPersona,
    DotTemplate,
    VanityUrl
} from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/uve/types';

import { PERSONA_KEY } from '../shared/consts';
import { DotPage, DotPageAssetParams, SavePagePayload } from '../shared/models';
import { ClientRequestProps } from '../store/features/client/withClient';
import { getFullPageURL } from '../utils';

export interface DotPageApiResponse {
    page: DotPage;
    site: Site;
    viewAs: {
        language: DotLanguage;
        persona?: DotPersona;
        variantId?: string;
    };
    layout: DotLayout;
    template: DotTemplate;
    containers: DotPageContainerStructure;
    urlContentMap?: DotCMSContentlet;
    vanityUrl?: VanityUrl;
    runningExperimentId?: string;
    numberContents: number;
}

export interface DotPageApiParams {
    url: string;
    language_id: string;
    [PERSONA_KEY]: string;
    variantName?: string;
    experimentId?: string;
    mode?: UVE_MODE;
    clientHost?: string;
    depth?: string;
    publishDate?: string;
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
    get(queryParams: DotPageAssetParams): Observable<DotPageApiResponse> {
        const { clientHost, ...params } = queryParams;
        const pageType = clientHost ? 'json' : 'render';
        const pageURL = getFullPageURL({ url: params.url, params });

        // Obtener la zona horaria del cliente
        const clientTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
        const timeZoneOffset = new Date().getTimezoneOffset();

        // Crear los headers con la información de zona horaria
        const headers = new HttpHeaders({
            'X-Client-TimeZone': clientTimeZone, // Por ejemplo: "America/New_York"
            'X-Client-TimeZone-Offset': timeZoneOffset.toString() // Offset en minutos
        });

        // Pasar los headers como parte de las opciones
        return this.http
            .get<{
                entity: DotPageApiResponse;
            }>(`/api/v1/page/${pageType}/${pageURL}`, { headers })
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
            .get<{
                entity: { content: { idenfitier: string } };
            }>(`/api/v1/containers/form/${formId}?containerId=${containerId}`)
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

    /**
     *
     * @description Save a contentlet in a page
     * @param {{ contentlet: { [fieldName: string]: string; inode: string } }} { contentlet }
     * @return {*}
     * @memberof DotPageApiService
     */
    saveContentlet({ contentlet }: { contentlet: { [fieldName: string]: string; inode: string } }) {
        return this.http.put(
            `/api/v1/workflow/actions/default/fire/EDIT?inode=${contentlet.inode}`,
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
    getGraphQLPage(query: string): Observable<DotPageApiResponse> {
        const headers = {
            'Content-Type': 'application/json',
            dotcachettl: '0' // Bypasses GraphQL cache
        };

        return this.http
            .post<{
                data: { page: Record<string, unknown> };
            }>('/api/v1/graphql', { query }, { headers })
            .pipe(
                pluck('data'),
                map((data) => graphqlToPageEntity(data) as DotPageApiResponse)
            );
    }

    /**
     *
     * @description Get Client Page from the Page API or GraphQL
     * @return {*}  {Observable<DotPageApiResponse>}
     * @memberof DotPageApiService
     */
    getClientPage(
        params: DotPageApiParams,
        clientProps: ClientRequestProps
    ): Observable<DotPageApiResponse> {
        const { query, params: clientParams } = clientProps;

        if (!query) {
            return this.get({
                ...(clientParams || {}),
                ...params
            });
        }

        return this.getGraphQLPage(query);
    }
}
