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

const CONSTRAINED_QUERY_LIMIT = 5000;

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
        contentTypeId: string,
        systemSearchableFields?: Record<string, unknown>
    ): Observable<[Column[], RelationshipFieldSearchResponse] | null> {
        // TODO: search() already calls #getLanguages() and #prepareContent() internally,
        // causing duplicate language HTTP calls and double-processing of contentlets.
        // Refactor to fetch languages once and pass them through.
        return forkJoin([
            this.getColumns(contentTypeId),
            this.search({ contentTypeId, systemSearchableFields }),
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
     * Fetches identifiers of contentlets that are already related to OTHER parents
     * through the given relationship, excluding children of the current contentlet.
     *
     * Uses the ES search API to find parent contentlets and extract their related child identifiers.
     *
     * @param params.parentContentTypeId - The ID (inode) of the parent content type
     * @param params.fieldVariable - The relationship field variable (e.g., "relation")
     * @param params.currentContentIdentifier - The identifier of the contentlet being edited (to exclude its own children)
     * @returns Observable<Set<string>> - Set of child identifiers that are already "taken" by other parents
     */
    getConstrainedIdentifiers(params: {
        parentContentTypeId: string;
        fieldVariable: string;
        currentContentIdentifier: string | null;
    }): Observable<Set<string>> {
        const { parentContentTypeId, fieldVariable } = params;

        if (!parentContentTypeId || !fieldVariable) {
            return of(new Set<string>());
        }

        return this.#contentSearchService
            .get<{ jsonObjectView: { contentlets: DotCMSContentlet[] } }>({
                query: `+structureInode:${parentContentTypeId} +working:true +deleted:false`,
                sort: 'modDate desc',
                limit: CONSTRAINED_QUERY_LIMIT,
                offset: 0,
                depth: 0
            })
            .pipe(
                map(({ jsonObjectView: { contentlets } }) => {
                    const constrainedIds = new Set<string>();

                    for (const parent of contentlets) {
                        if (parent.identifier === params.currentContentIdentifier) {
                            continue;
                        }

                        const relatedChildren = parent[fieldVariable] as unknown;

                        // ONE_TO_ONE returns a single value; ONE_TO_MANY returns an array
                        const children = Array.isArray(relatedChildren)
                            ? relatedChildren
                            : relatedChildren != null
                              ? [relatedChildren]
                              : [];

                        for (const child of children as unknown[]) {
                            const childId =
                                typeof child === 'string'
                                    ? child
                                    : child != null &&
                                        typeof child === 'object' &&
                                        'identifier' in child
                                      ? (child as { identifier: string }).identifier
                                      : null;
                            if (childId) {
                                constrainedIds.add(childId);
                            }
                        }
                    }

                    return constrainedIds;
                }),
                catchError(() => of(new Set<string>()))
            );
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
