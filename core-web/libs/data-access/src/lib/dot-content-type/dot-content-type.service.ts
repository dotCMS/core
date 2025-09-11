import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { defaultIfEmpty, filter, flatMap, map, pluck, take, toArray } from 'rxjs/operators';

import {
    DotCMSContentType,
    StructureTypeView,
    ContentTypeView,
    DotCopyContentTypeDialogFormFields,
    DotPagination
} from '@dotcms/dotcms-models';

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
     *Get the content types from the endpoint
     *
     * @param {*} { filter = '', page = 40, type = '' }
     * @return {*}  {Observable<DotCMSContentType[]>}
     * @memberof DotContentTypeService
     */
    getContentTypes({ filter = '', page = 40, type = '' }): Observable<DotCMSContentType[]> {
        return this.#httpClient
            .get<{
                entity: DotCMSContentType[];
            }>(
                `/api/v1/contenttype?filter=${filter}&orderby=name&direction=ASC&per_page=${page}${
                    type
                        ? `${type
                              .split(',')
                              .map((item) => `&type=${item}`)
                              .join('')}`
                        : ''
                }`
            )
            .pipe(pluck('entity'));
    }

    /**
     *Get the content types from the endpoint
     *
     * @param {*} { filter = '', page = 40, type = [] }
     * @return {*}  {Observable<DotCMSContentType[]>}
     * @memberof DotContentTypeService
     */
    getContentTypesWithPagination({ filter = '', page = 40, type = '' }): Observable<{
        contentTypes: DotCMSContentType[];
        pagination: DotPagination;
    }> {
        return this.#httpClient
            .get<{
                entity: DotCMSContentType[];
                pagination: DotPagination;
            }>(
                `/api/v1/contenttype?filter=${filter}&orderby=name&direction=ASC&per_page=${page}${
                    type.length > 0
                        ? type
                              .split(',')
                              .map((item) => `&type=${item}`)
                              .join('')
                        : ''
                }`
            )
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
     * @param {string} type
     * @return {*}  {Observable<DotCMSContentType[]>}
     * @memberof DotContentTypeService
     */
    getByTypes(type: string, per_page = 100): Observable<DotCMSContentType[]> {
        return this.#httpClient
            .get<{
                entity: DotCMSContentType[];
            }>(
                `/api/v1/contenttype?${type
                    .split(',')
                    .map((item) => `type=${item}`)
                    .join('')}&per_page=${per_page}`
            )
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
