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

import {
    DEFAULT_RELATIONSHIP_COLUMNS,
    RELATIONSHIP_OPTIONS,
    SHOW_FIELDS_VARIABLE_KEY,
    SPECIAL_FIELDS
} from '../dot-edit-content-relationship-field.constants';
import { RelationshipTypes, TableColumn } from '../models/relationship.models';

@Injectable({
    providedIn: 'root'
})
export class RelationshipFieldService {
    readonly #dotContentTypeService = inject(DotContentTypeService);
    readonly #dotFieldService = inject(DotFieldService);

    prepareField(params: { field: DotCMSContentTypeField; contentlet: DotCMSContentlet }) {
        const { field, contentlet } = params;

        const cardinality = field?.relationships?.cardinality ?? null;
        const contentTypeId = this.#getContentTypeIdFromRelationship(field);

        if (cardinality === null || !field?.variable || !contentTypeId) {
            throw new Error('Invalid field');
        }

        const data = this.#getRelationshipFromContentlet({ contentlet, variable: field.variable });
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

    #getContentType(contentTypeId: string) {
        return this.#dotContentTypeService.getContentType(contentTypeId);
    }

    #getIsNewEditorEnabled(contentType: DotCMSContentType): boolean {
        return contentType.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] === true;
    }

    #getContentTypeIdFromRelationship(field: DotCMSContentTypeField): string | null {
        if (!field?.relationships?.velocityVar) {
            return null;
        }

        const [contentTypeId] = field.relationships.velocityVar.split('.');

        return contentTypeId || null;
    }

    #getRelationshipFromContentlet({
        contentlet,
        variable
    }: {
        contentlet: DotCMSContentlet;
        variable: string;
    }): DotCMSContentlet[] {
        if (!contentlet || !variable || !contentlet[variable]) {
            return [];
        }

        const relationship = contentlet[variable];
        const isArray = Array.isArray(relationship);

        if (!isArray && typeof relationship !== 'object') {
            return [];
        }

        return isArray ? relationship : [relationship];
    }

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

    #isImageField(clazz: DotCMSClazzes): boolean {
        return (
            clazz === DotCMSClazzes.IMAGE ||
            clazz === DotCMSClazzes.FILE ||
            clazz === DotCMSClazzes.BINARY
        );
    }
}
