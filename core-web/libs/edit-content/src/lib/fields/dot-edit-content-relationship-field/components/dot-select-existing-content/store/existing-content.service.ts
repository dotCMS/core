import { forkJoin, Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, switchMap } from 'rxjs/operators';

import {
    DotContentSearchService,
    DotFieldService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotContentSearchParams
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField, DotLanguage } from '@dotcms/dotcms-models';

import { Column } from '../../../models/column.model';

type LanguagesMap = Record<number, DotLanguage>;

const EXCLUDED_COLUMNS = ['title', 'language', 'modDate'];

export type RelationshipFieldQueryParams = DotContentSearchParams & {
    contentTypeId: string;
};

export interface RelationshipFieldSearchResponse {
    contentlets: DotCMSContentlet[];
    totalResults: number;
}

@Injectable({
    providedIn: 'root'
})
export class ExistingContentService {
    readonly #fieldService = inject(DotFieldService);
    readonly #contentSearchService = inject(DotContentSearchService);
    readonly #dotLanguagesService = inject(DotLanguagesService);
    readonly #httpErrorManagerService = inject(DotHttpErrorManagerService);

    /**
     * Searches for relationship content items using the content search API
     *
     * @param queryParams - Object containing search parameters
     * @param queryParams.contentTypeId - The content type ID to search within
     * @param queryParams.page - Page number for pagination
     * @param queryParams.perPage - Number of items per page
     * @param queryParams.searchableFieldsByContentType - Configures which fields are searchable by content type
     * @param queryParams.globalSearch - Optional. Global search term for filtering content
     * @param queryParams.systemSearchableFields - Optional. Additional system fields to include in search
     *
     * @returns Observable<DotCMSContentlet[]> - Stream of content items with language information attached
     *
     * @example
     * // Basic search with pagination
     * service.search({
     *   contentTypeId: 'news',
     *   page: 1,
     *   perPage: 10,
     *   searchableFieldsByContentType: { news: {} }
     * });
     *
     * // Search with global term
     * service.search({
     *   contentTypeId: 'news',
     *   page: 1,
     *   perPage: 10,
     *   searchableFieldsByContentType: { news: {} },
     *   globalSearch: 'latest'
     * });
     */
    search(queryParams: RelationshipFieldQueryParams): Observable<RelationshipFieldSearchResponse> {
        const params: DotContentSearchParams = {
            searchableFieldsByContentType: {
                [queryParams.contentTypeId]: {}
            },
            page: queryParams.page,
            perPage: queryParams.perPage
        };

        if (queryParams.globalSearch ?? null) {
            params.globalSearch = queryParams.globalSearch;
        }

        if (queryParams.systemSearchableFields ?? null) {
            params.systemSearchableFields = { ...queryParams.systemSearchableFields };
        }

        return this.#contentSearchService.search(params).pipe(
            switchMap(({ jsonObjectView: { contentlets }, resultsSize }) => {
                if (!contentlets.length) {
                    return of({
                        contentlets: [],
                        totalResults: 0
                    });
                }

                return this.#getLanguages().pipe(
                    map((languages) => ({
                        contentlets: this.#prepareContent(contentlets, languages),
                        totalResults: resultsSize
                    }))
                );
            }),
            catchError((error: HttpErrorResponse) => {
                return this.#httpErrorManagerService.handle(error).pipe(
                    map(() => ({
                        contentlets: [],
                        totalResults: 0
                    }))
                );
            })
        );
    }

    /**
     * Gets the columns and content for the relationship field
     * @param contentTypeId The content type ID
     * @param showFields The fields to show in the relationship field
     * @returns Observable of [Column[], RelationshipFieldItem[]]
     */
    getColumnsAndContent(
        contentTypeId: string
    ): Observable<[Column[], RelationshipFieldSearchResponse] | null> {
        return forkJoin([
            this.getColumns(contentTypeId),
            this.search({ contentTypeId }),
            this.#getLanguages()
        ]).pipe(
            map(([columns, searchResponse, languages]) => [
                columns,
                {
                    contentlets: this.#prepareContent(searchResponse.contentlets, languages),
                    totalResults: searchResponse.totalResults
                }
            ]),
            catchError((error: HttpErrorResponse) => {
                return this.#httpErrorManagerService.handle(error).pipe(map(() => null));
            })
        );
    }

    /**
     * Gets the columns for the relationship field
     * @param contentTypeId The content type ID
     * @param showFields The fields to show in the relationship field
     * @returns Observable of Column array
     */
    getColumns(contentTypeId: string): Observable<Column[]> {
        return this.#fieldService
            .getFields(contentTypeId, 'SHOW_IN_LIST')
            .pipe(map((fields) => this.#buildColumns(fields)));
    }

    /**
     * Gets the languages for the relationship field
     * @returns Observable of Record<number, DotLanguageWithLabel>
     */
    #getLanguages(): Observable<LanguagesMap> {
        return this.#dotLanguagesService.get().pipe(
            map((languages) =>
                languages.reduce((acc, lang) => {
                    acc[lang.id] = { ...lang };

                    return acc;
                }, {})
            )
        );
    }

    /**
     * Builds the columns for the relationship field
     * @param columns The columns to build
     * @param showFields The fields to show in the relationship field
     * @returns Array of Column
     */
    #buildColumns(columns: DotCMSContentTypeField[]): Column[] {
        return columns
            .filter(
                (column) =>
                    column.variable &&
                    column.name &&
                    !EXCLUDED_COLUMNS.includes(column.name.toLowerCase())
            )
            .map((column) => ({
                field: column.variable,
                header: column.name
            }));
    }

    /**
     * Prepares the content for the relationship field
     * @param content The contentlets to prepare
     * @param languages The languages to prepare
     * @returns Array of DotCMSContentlet
     */
    #prepareContent(content: DotCMSContentlet[], languages: LanguagesMap): DotCMSContentlet[] {
        return content.map((item) => ({
            ...item,
            title: item.title || item.identifier,
            language: languages[item.languageId]
        }));
    }
}
