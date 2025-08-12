import { Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map, switchMap } from 'rxjs/operators';

import { DotFieldService, DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { getRelationshipFromContentlet } from '../../../utils/relationshipFromContentlet';
import { DEFAULT_RELATIONSHIP_COLUMNS } from '../dot-edit-content-relationship-field.constants';
import { TableColumn } from '../models/relationship.models';
import {
    getContentTypeIdFromRelationship,
    getFieldHeader,
    getSelectionModeByCardinality,
    extractShowFields,
    getTypeField,
    isNewEditorEnabled
} from '../utils';

/**
 * Service responsible for managing relationship field operations in the edit content module.
 *
 * This service handles the preparation and configuration of relationship fields,
 * including dynamic column building, content type validation, and relationship type determination.
 *
 * @example
 * ```typescript
 * // Inject the service
 * constructor(private relationshipFieldService: RelationshipFieldService) {}
 *
 * // Prepare a relationship field
 * this.relationshipFieldService.prepareField({ field, contentlet })
 *   .subscribe(result => {
 *     // Handle the prepared field data
 *   });
 * ```
 */
@Injectable({
    providedIn: 'root'
})
export class RelationshipFieldService {
    readonly #dotContentTypeService = inject(DotContentTypeService);
    readonly #dotFieldService = inject(DotFieldService);

    /**
     * Prepares a relationship field for editing by validating the field configuration,
     * determining the selection mode, and building dynamic columns if needed.
     *
     * @param params - Object containing the field and contentlet data
     * @param params.field - The content type field definition containing relationship configuration
     * @param params.contentlet - The contentlet instance containing the relationship data
     * @returns Observable that emits the prepared field state with all necessary configuration
     * @throws Error when the field is invalid (missing cardinality, variable, or content type ID)
     *
     * @example
     * ```typescript
     * this.relationshipFieldService.prepareField({
     *   field: contentTypeField,
     *   contentlet: contentletInstance
     * }).subscribe(preparedField => {
     *   console.log('Field prepared:', preparedField);
     * });
     * ```
     */
    prepareField(params: { field: DotCMSContentTypeField; contentlet: DotCMSContentlet }) {
        const { field, contentlet } = params;

        return of({ field, contentlet }).pipe(
            switchMap(({ field, contentlet }) => {
                const cardinality = field?.relationships?.cardinality ?? null;
                const contentTypeId = getContentTypeIdFromRelationship(field);

                if (cardinality === null) {
                    throw new Error('Invalid field: missing cardinality');
                }
                if (!field?.variable) {
                    throw new Error('Invalid field: missing variable');
                }
                if (!contentTypeId) {
                    throw new Error('Invalid field: missing contentTypeId');
                }

                const data = getRelationshipFromContentlet({
                    contentlet,
                    variable: field.variable
                });
                const selectionMode = getSelectionModeByCardinality(cardinality);
                const showFields = extractShowFields(field);

                return of({ cardinality, contentTypeId, data, selectionMode, showFields });
            }),
            switchMap(({ contentTypeId, data, selectionMode, showFields }) => {
                return this.#dotContentTypeService.getContentType(contentTypeId).pipe(
                    map((contentType) => ({
                        field,
                        isNewEditorEnabled: isNewEditorEnabled(contentType),
                        selectionMode,
                        contentType,
                        contentTypeId,
                        data
                    })),
                    switchMap((newState) => {
                        const hasShowFields = showFields?.length > 0;

                        if (!hasShowFields) {
                            return of({
                                ...newState,
                                columns: DEFAULT_RELATIONSHIP_COLUMNS
                            });
                        }
                        return this.#buildDynamicColumns(contentTypeId, showFields).pipe(
                            map((columns) => ({ ...newState, columns }))
                        );
                    })
                );
            })
        );
    }

    /**
     * Builds dynamic table columns based on the specified show fields for a content type.
     *
     * @param contentTypeId - The ID of the content type to build columns for
     * @param showFields - Array of field names to display as columns
     * @returns Observable that emits an array of table column configurations
     *
     * @example
     * ```typescript
     * this.#buildDynamicColumns('contentType123', ['title', 'description'])
     *   .subscribe(columns => {
     *     console.log('Dynamic columns:', columns);
     *   });
     * ```
     */
    #buildDynamicColumns(contentTypeId: string, showFields: string[]): Observable<TableColumn[]> {
        return this.#dotFieldService.getFields(contentTypeId).pipe(
            map((dataColumns) => {
                return showFields.map((fieldName) => ({
                    nameField: fieldName,
                    header: getFieldHeader(fieldName),
                    type: getTypeField(fieldName, dataColumns)
                }));
            })
        );
    }
}
