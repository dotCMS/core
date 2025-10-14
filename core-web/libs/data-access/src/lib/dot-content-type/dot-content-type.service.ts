import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { defaultIfEmpty, filter, flatMap, map, pluck, take, toArray } from 'rxjs/operators';

import {
    DotCMSContentType,
    StructureTypeView,
    ContentTypeView,
    DotCopyContentTypeDialogFormFields,
    DotPagination,
    DotContentTypePaginationOptions
} from '@dotcms/dotcms-models';

/**
 * Creates HttpParams for content type filtering
 * @param type Comma-separated string of content types
 * @param baseParams Optional base HttpParams to append to
 * @returns HttpParams with type filters added
 */
const generateContentTypeFilter = (type: string, baseParams?: HttpParams): HttpParams => {
    let params = baseParams || new HttpParams();

    if (type && type.length > 0) {
        const types = type.split(',').filter((t) => t.trim());
        types.forEach((typeValue) => {
            params = params.append('type', typeValue.trim());
        });
    }

    return params;
};

@Injectable()
export class DotContentTypeService {
    readonly #httpClient = inject(HttpClient);

    /**
     * Get a content type by id or variable name
     * @param idOrVar content type's id or variable name
     * @returns Content Type
     */
    getContentType(idOrVar: string): Observable<DotCMSContentType> {
        return this.#httpClient
            .get<{ entity: DotCMSContentType }>(`/api/v1/contenttype/id/${idOrVar}`)
            .pipe(take(1), pluck('entity'));
    }

    /**
     * Creates HttpParams for retrieving content types with optional parameters
     * Only includes parameters that have meaningful values (not empty, null, or undefined)
     */
    private contentTypePaginationParams(options: DotContentTypePaginationOptions = {}): HttpParams {
        let params = new HttpParams();

        // Default parameters
        params = params.set('orderby', 'name');
        params = params.set('direction', 'ASC');
        params = params.set('per_page', (options.page ?? 40).toString());

        // Add optional parameters if they have meaningful values
        if (this.hasValue(options.filter)) {
            params = params.set('filter', options.filter);
        }

        if (this.hasValue(options.ensure)) {
            params = params.set('ensure', options.ensure);
        }

        if (this.hasValue(options.type)) {
            params = generateContentTypeFilter(options.type, params);
        }

        return params;
    }

    /**
     * Checks if a value is meaningful (not empty string, null, or undefined)
     * @param value The value to check
     * @returns {boolean} True if the value is meaningful, false otherwise
     */
    private hasValue<T>(value: T | undefined | null): value is T {
        return value !== null && value !== undefined && value !== '';
    }

    /**
     *Get the content types from the endpoint
     *
     * @param options Optional parameters for filtering and pagination
     * @return {Observable<DotCMSContentType[]>} Observable containing content types info
     * @memberof DotContentTypeService
     */
    getContentTypes(
        options: DotContentTypePaginationOptions = {}
    ): Observable<DotCMSContentType[]> {
        return this.#httpClient
            .get<{
                entity: DotCMSContentType[];
            }>('/api/v1/contenttype', { params: this.contentTypePaginationParams(options) })
            .pipe(pluck('entity'));
    }

    /**
     * Get the content types from the endpoint with pagination
     *
     * @param options Optional parameters for filtering and pagination
     * @return {Observable<{contentTypes: DotCMSContentType[];pagination: DotPagination;}>}
     * Observable containing content types and pagination info
     * @memberof DotContentTypeService
     */
    getContentTypesWithPagination(options: DotContentTypePaginationOptions = {}): Observable<{
        contentTypes: DotCMSContentType[];
        pagination: DotPagination;
    }> {
        return this.#httpClient
            .get<{
                entity: DotCMSContentType[];
                pagination: DotPagination;
            }>('/api/v1/contenttype', { params: this.contentTypePaginationParams(options) })
            .pipe(map((data) => ({ contentTypes: data.entity, pagination: data.pagination })));
    }

    /**
     * Gets all content types excluding the RECENT ones
     *
     * @returns Observable<StructureTypeView[]>
     */
    getAllContentTypes(): Observable<StructureTypeView[]> {
        return this.getBaseTypes()
            .pipe(
                flatMap((structures: StructureTypeView[]) => structures),
                filter((structure: StructureTypeView) => !this.isRecentContentType(structure))
            )
            .pipe(toArray());
    }

    /**
     * Gets an array of allowerdType of DotCMSContentType[]
     *
     * @param {string} [filter='']
     * @param {string} [allowedTypes='']
     * @return {*}  {Observable<DotCMSContentType[]>}
     * @memberof DotContentTypeService
     */
    filterContentTypes(filter = '', allowedTypes = ''): Observable<DotCMSContentType[]> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        return this.#httpClient
            .post<{ entity: DotCMSContentType[] }>(
                `/api/v1/contenttype/_filter`,
                {
                    filter: {
                        types: allowedTypes,
                        query: filter
                    },
                    orderBy: 'name',
                    direction: 'ASC',
                    perPage: 40
                },
                { headers }
            )
            .pipe(pluck('entity'));
    }

    /**
     * Get url by id
     *
     * @param string id
     * @returns Observable<string>
     * @memberof ContentletService
     */
    getUrlById(id: string): Observable<string> {
        return this.getBaseTypes().pipe(
            flatMap((structures: StructureTypeView[]) => structures),
            pluck('types'),
            flatMap((contentTypeViews: ContentTypeView[]) => contentTypeViews),
            filter(
                (contentTypeView: ContentTypeView) =>
                    contentTypeView.variable.toLocaleLowerCase() === id
            ),
            pluck('action')
        );
    }

    /**
     * Check is the content types is present in the object
     *
     * @param string id
     * @returns Observable<boolean>
     * @memberof ContentletService
     */
    isContentTypeInMenu(id: string): Observable<boolean> {
        return this.getUrlById(id).pipe(
            map((url: string) => !!url),
            defaultIfEmpty(false)
        );
    }

    /**
     * Creates a copy of a content type
     * @param variable
     * @param copyFormFields
     * @returns Observable<DotCMSContentType>
     * @memberof DotContentTypeService
     */
    saveCopyContentType(
        variable: string,
        copyFormFields: DotCopyContentTypeDialogFormFields
    ): Observable<DotCMSContentType> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        return this.#httpClient
            .post<{
                entity: DotCMSContentType;
            }>(`/api/v1/contenttype/${variable}/_copy`, copyFormFields, { headers })
            .pipe(pluck('entity'));
    }

    /**
     * Get content type by types
     *
     * @param {string} type Comma-separated string of content types
     * @param {number} per_page Number of items per page (default: 100)
     * @return {Observable<DotCMSContentType[]>} Observable containing content types info
     * @memberof DotContentTypeService
     */
    getByTypes(type: string, per_page = 100): Observable<DotCMSContentType[]> {
        let params = new HttpParams().set('per_page', per_page.toString());
        params = generateContentTypeFilter(type, params);

        return this.#httpClient
            .get<{ entity: DotCMSContentType[] }>('/api/v1/contenttype', { params })
            .pipe(pluck('entity'));
    }

    /**
     * Updates a content type by its ID with the provided payload.
     *
     * This method allows updating any property of a content type by sending a partial or full payload.
     * The payload should match the expected structure for the content type update API.
     *
     * @param id The unique identifier of the content type to update.
     * @param payload The data to update the content type with. This can be a partial or full content type object.
     * @returns Observable<DotCMSContentType> The updated content type.
     * @memberof DotContentTypeService
     */
    updateContentType(id: string, payload: unknown): Observable<DotCMSContentType> {
        return this.#httpClient
            .put<{ entity: DotCMSContentType }>(`/api/v1/contenttype/id/${id}`, payload)
            .pipe(pluck('entity'));
    }

    private isRecentContentType(type: StructureTypeView): boolean {
        return type.name.startsWith('RECENT');
    }

    private getBaseTypes(): Observable<StructureTypeView[]> {
        return this.#httpClient
            .get<{ entity: StructureTypeView[] }>('/api/v1/contenttype/basetypes')
            .pipe(pluck('entity'));
    }
}
