import { Observable, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map, switchMap } from 'rxjs/operators';

import { DotFieldService, DotContentTypeService } from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField,
    FeaturedFlags,
    DotCMSClazzes
} from '@dotcms/dotcms-models';

import { RelationshipFieldState } from './relationship-field.store';

import { getRelationshipFromContentlet } from '../../../utils/relationshipFromContentlet';
import {
    DEFAULT_RELATIONSHIP_COLUMNS,
    RELATIONSHIP_OPTIONS,
    SHOW_FIELDS_VARIABLE_KEY,
    SPECIAL_FIELDS
} from '../dot-edit-content-relationship-field.constants';
import { RelationshipTypes, TableColumn } from '../models/relationship.models';

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

        const cardinality = field?.relationships?.cardinality ?? null;
        const contentTypeId = this.#getContentTypeIdFromRelationship(field);

        if (cardinality === null || !field?.variable || !contentTypeId) {
            throw new Error('Invalid field');
        }

        const data = getRelationshipFromContentlet({ contentlet, variable: field.variable });
        const selectionMode = this.#getSelectionModeByCardinality(cardinality);

        const showFields = this.#extractShowFields(field);
        const hasShowFields = showFields?.length > 0;

        return this.#getContentType(contentTypeId).pipe(
            map((contentType) => {
                const isNewEditorEnabled = this.#getIsNewEditorEnabled(contentType);

                return {
                    field,
                    isNewEditorEnabled,
                    selectionMode,
                    contentType,
                    contentTypeId,
                    data
                };
            }),
            switchMap((newState) => {
                if (!hasShowFields) {
                    return of({
                        ...newState,
                        columns: DEFAULT_RELATIONSHIP_COLUMNS
                    });
                }
                return this.#buildDynamicColumns(contentTypeId, showFields).pipe(
                    map((columns) => {
                        return { ...newState, columns };
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
                    header: fieldName.replace(/([A-Z])/g, ' $1').trim(),
                    type: this.#getTypeField(fieldName, dataColumns)
                }));
            })
        );
    }

    /**
     * Retrieves a content type by its ID.
     *
     * @param contentTypeId - The unique identifier of the content type
     * @returns Observable that emits the content type data
     */
    #getContentType(contentTypeId: string) {
        return this.#dotContentTypeService.getContentType(contentTypeId);
    }

    /**
     * Determines if the new content editor is enabled for a given content type.
     *
     * @param contentType - The content type to check for editor feature flags
     * @returns True if the new editor is enabled, false otherwise
     */
    #getIsNewEditorEnabled(contentType: DotCMSContentType): boolean {
        return contentType.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] === true;
    }

    /**
     * Extracts the content type ID from a relationship field's velocity variable.
     *
     * @param field - The field containing relationship configuration
     * @returns The content type ID if found, null otherwise
     *
     * @example
     * ```typescript
     * // For a field with velocityVar: "contentType123.fieldName"
     * const id = this.#getContentTypeIdFromRelationship(field); // Returns "contentType123"
     * ```
     */
    #getContentTypeIdFromRelationship(field: DotCMSContentTypeField): string | null {
        if (!field?.relationships?.velocityVar) {
            return null;
        }

        const [contentTypeId] = field.relationships.velocityVar.split('.');

        return contentTypeId || null;
    }

    /**
     * Determines the selection mode (single or multiple) based on the relationship cardinality.
     *
     * @param cardinality - The relationship cardinality number
     * @returns The selection mode: 'single' for one-to-one/many-to-one, 'multiple' for one-to-many/many-to-many
     * @throws Error when the cardinality doesn't match any known relationship type
     *
     * @example
     * ```typescript
     * const mode = this.#getSelectionModeByCardinality(1); // Returns 'single'
     * const mode2 = this.#getSelectionModeByCardinality(2); // Returns 'multiple'
     * ```
     */
    #getSelectionModeByCardinality(cardinality: number): RelationshipFieldState['selectionMode'] {
        const relationshipType = RELATIONSHIP_OPTIONS[cardinality];

        if (!relationshipType) {
            throw new Error(`Invalid relationship type for cardinality: ${cardinality}`);
        }

        const isSingleMode =
            relationshipType === RelationshipTypes.ONE_TO_ONE ||
            relationshipType === RelationshipTypes.MANY_TO_ONE;

        return isSingleMode ? 'single' : 'multiple';
    }

    /**
     * Extracts the show fields configuration from a field's variables.
     *
     * @param field - The field containing field variables configuration
     * @returns Array of field names to display, or null if no show fields are configured
     *
     * @example
     * ```typescript
     * // For a field with showFields variable: "title,description,image"
     * const fields = this.#extractShowFields(field); // Returns ["title", "description", "image"]
     * ```
     */
    #extractShowFields(field: DotCMSContentTypeField | null): string[] | null {
        if (!field?.fieldVariables) {
            return null;
        }

        const showFieldsVar = field.fieldVariables.find(
            ({ key }) => key === SHOW_FIELDS_VARIABLE_KEY
        );

        if (!showFieldsVar?.value) {
            return null;
        }

        return showFieldsVar.value
            .split(',')
            .map((field) => field.trim())
            .filter((field) => field.length > 0);
    }

    /**
     * Determines the display type for a field based on its name and content type configuration.
     *
     * @param fieldName - The name of the field to determine the type for
     * @param dataColumns - Array of content type fields to search through
     * @returns The display type: 'image', 'text', or other special types
     *
     * @example
     * ```typescript
     * const type = this.#getTypeField('title', contentTypeFields); // Returns 'text'
     * const imageType = this.#getTypeField('image', contentTypeFields); // Returns 'image'
     * ```
     */
    #getTypeField(fieldName: string, dataColumns: DotCMSContentTypeField[]): TableColumn['type'] {
        const isSpecialField = SPECIAL_FIELDS[fieldName];

        if (isSpecialField) {
            return isSpecialField;
        }

        if (dataColumns.length > 0) {
            const field = dataColumns.find((field) => field.variable === fieldName);
            return field && this.#isImageField(field.clazz) ? 'image' : 'text';
        }

        return 'text';
    }

    /**
     * Checks if a field class represents an image, file, or binary field.
     *
     * @param clazz - The field class to check
     * @returns True if the field is an image, file, or binary field, false otherwise
     *
     * @example
     * ```typescript
     * const isImage = this.#isImageField(DotCMSClazzes.IMAGE); // Returns true
     * const isText = this.#isImageField(DotCMSClazzes.TEXT); // Returns false
     * ```
     */
    #isImageField(clazz: DotCMSClazzes): boolean {
        return (
            clazz === DotCMSClazzes.IMAGE ||
            clazz === DotCMSClazzes.FILE ||
            clazz === DotCMSClazzes.BINARY
        );
    }
}
