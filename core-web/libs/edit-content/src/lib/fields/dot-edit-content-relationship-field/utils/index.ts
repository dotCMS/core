import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import {
    DEFAULT_RELATIONSHIP_COLUMNS,
    RELATIONSHIP_OPTIONS,
    SHOW_FIELDS_VARIABLE_KEY,
    SPECIAL_FIELDS
} from '../dot-edit-content-relationship-field.constants';
import { RelationshipTypes, TableColumn } from '../models/relationship.models';

/**
 * Get the selection mode by cardinality.
 *
 * @param cardinality - The cardinality of the relationship.
 * @returns The selection mode.
 */
export function getSelectionModeByCardinality(cardinality: number) {
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
 * Extracts relationship data from a contentlet based on the provided variable name.
 *
 * @param params - The parameters object
 * @param params.contentlet - The DotCMS contentlet object containing the relationship data
 * @param params.variable - The variable name that identifies the relationship field in the contentlet
 * @returns An array of related DotCMS contentlets. Returns empty array if:
 * - The contentlet is null/undefined
 * - The variable name is empty
 * - The relationship field (contentlet[variable]) is null/undefined
 * - The relationship field is not an array or single contentlet
 */
export function getRelationshipFromContentlet({
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

/**
 * Extracts the content type ID from a relationship field.
 *
 * @param field - The DotCMS content type field object containing the relationship data
 * @returns The content type ID, or null if not found
 */
export function getContentTypeIdFromRelationship(field: DotCMSContentTypeField): string | null {
    if (!field?.relationships?.velocityVar) {
        return null;
    }

    const [contentTypeId] = field.relationships.velocityVar.split('.');

    return contentTypeId || null;
}

export function extractShowFields(field: DotCMSContentTypeField | null): string[] | null {
    if (!field?.fieldVariables) {
        return null;
    }

    const showFieldsVar = field.fieldVariables.find(({ key }) => key === SHOW_FIELDS_VARIABLE_KEY);

    if (!showFieldsVar?.value) {
        return null;
    }

    return showFieldsVar.value
        .split(',')
        .map((field) => field.trim())
        .filter((field) => field.length > 0);
}

export function getTypeField(fieldName: string, data: DotCMSContentlet[]): TableColumn['type'] {
    const isSpecialField = SPECIAL_FIELDS[fieldName];

    if (isSpecialField) {
        return isSpecialField;
    }

    if (data.length > 0) {
        return isImage(fieldName, data[0]) ? 'image' : 'text';
    }

    return 'text';
}

export function getFieldHeader(fieldName: string): string {
    return fieldName.replace(/([A-Z])/g, ' $1').trim();
}

export function getColumns(field: DotCMSContentTypeField, data: DotCMSContentlet[]): TableColumn[] {
    const showFields = extractShowFields(field);

    // Dynamic columns
    if (showFields?.length > 0) {
        return showFields.map((fieldName) => ({
            nameField: fieldName,
            header: getFieldHeader(fieldName),
            type: getTypeField(fieldName, data)
        }));
    } else {
        return DEFAULT_RELATIONSHIP_COLUMNS;
    }
}

export function isImage(fieldName: string, contentlet: DotCMSContentlet): boolean {
    const metadata = contentlet[`${fieldName}MetaData`];

    if (!metadata) {
        return false;
    }

    return metadata?.isImage ? true : false;
}
