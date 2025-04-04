import { Observable } from 'rxjs';

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { defaultIfEmpty, filter, flatMap, map, pluck, take, toArray } from 'rxjs/operators';

import {
    DotCMSContentType,
    StructureTypeView,
    ContentTypeView,
    DotCopyContentTypeDialogFormFields
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
            }>(`/api/v1/contenttype?filter=${filter}&orderby=name&direction=ASC&per_page=${page}${type ? `&type=${type}` : ''}`)
            .pipe(pluck('entity'));
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
            }>(`/api/v1/contenttype?type=${type}&per_page=${per_page}`)
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
