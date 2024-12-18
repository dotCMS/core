import { forkJoin, Observable } from 'rxjs';

import { formatDate } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, pluck } from 'rxjs/operators';


import { DotContentSearchService, DotFieldService, DotHttpErrorManagerService, DotLanguagesService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { RelationshipFieldItem } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/relationship.models';

import {
    MANDATORY_FIRST_COLUMNS,
    MANDATORY_LAST_COLUMNS
} from '../dot-edit-content-relationship-field.constants';
import { Column } from '../models/column.model';


type LanguagesMap = Record<number, string>;

@Injectable({
    providedIn: 'root'
})
export class RelationshipFieldService {
    readonly #fieldService = inject(DotFieldService);
    readonly #contentSearchService = inject(DotContentSearchService);
    readonly #dotLanguagesService = inject(DotLanguagesService);
    readonly #httpErrorManagerService = inject(DotHttpErrorManagerService);

    /**
     * Gets relationship content items
     * @returns Observable of RelationshipFieldItem array
     */
    getContent(contentTypeId: string): Observable<DotCMSContentlet[]> {
        const query = `+contentType:${contentTypeId} +deleted:false +working:true`;

        return this.#contentSearchService
            .get({
                query,
                limit: 100
            })
            .pipe(pluck('jsonObjectView', 'contentlets'));
    }

    /**
     * Gets the columns and content for the relationship field
     * @param contentTypeId The content type ID
     * @returns Observable of [Column[], RelationshipFieldItem[]]
     */
    getColumnsAndContent(contentTypeId: string): Observable<[Column[], RelationshipFieldItem[]] | null> {
        return forkJoin([
            this.getColumns(contentTypeId),
            this.getContent(contentTypeId),
            this.#getLanguages()
        ]).pipe(
            map(([columns, content, languages]) => [
                columns,
                this.#matchColumnsWithContent(columns, content, languages)
            ]),
            catchError((error: HttpErrorResponse) => {
                return this.#httpErrorManagerService.handle(error).pipe(
                    map(() => null)
                );
            })
        );
    }

    /**
     * Gets the columns for the relationship field
     * @param contentTypeId The content type ID
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
                    const code = lang.isoCode || lang.languageCode;
                    acc[lang.id] = `${lang.language} (${code})`;

                    return acc;
                }, {})
            )
        );
    }

    /**
     * Builds the columns for the relationship field
     * @param columns The columns to build
     * @returns Array of Column
     */
    #buildColumns(columns: DotCMSContentTypeField[]): Column[] {
        const firstColumnsMap = new Map(
            MANDATORY_FIRST_COLUMNS.map((field) => [field, { field, header: field }])
        );

        const lastColumnsMap = new Map(
            MANDATORY_LAST_COLUMNS.map((field) => [field, { field, header: field }])
        );

        const contentColumnsMap = new Map(
            columns
                .filter((column) => column.variable && column.name)
                .map((column) => [
                    column.variable,
                    {
                        field: column.variable,
                        header: column.name
                    }
                ])
        );

        // Merge maps while preserving order and removing duplicates
        const uniqueColumns = new Map([
            ...firstColumnsMap,
            ...contentColumnsMap,
            ...lastColumnsMap
        ]);

        return Array.from(uniqueColumns.values());
    }

    /**
     * Maps contentlets to relationship field items
     * @param columns The columns to map
     * @param content The contentlets to map
     * @returns Array of RelationshipFieldItem
     */
    #matchColumnsWithContent(
        columns: Column[],
        content: DotCMSContentlet[],
        languages: LanguagesMap
    ): RelationshipFieldItem[] {
        return content.map((item) => {
            const dynamicColumns = columns.reduce((acc, column) => {
                const key = column.field;
                const value = item[key];

                acc[key] = value ?? '';

                return acc;
            }, {});

            const relationshipItem: RelationshipFieldItem = {
                ...dynamicColumns,
                id: item.identifier,
                title: item.title || item.identifier,
                language: languages[item.languageId] || '',
                modDate: formatDate(item.modDate, 'short', 'en-US')
            };

            return relationshipItem;
        });
    }
}
