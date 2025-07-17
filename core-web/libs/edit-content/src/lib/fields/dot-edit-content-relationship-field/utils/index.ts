import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { FIELD_WIDTHS, IMAGE_EXTENSIONS, IMAGE_FIELD_PATTERNS, RELATIONSHIP_OPTIONS, SHOW_FIELDS_VARIABLE_KEY } from '../dot-edit-content-relationship-field.constants';
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
 * @returns The content type ID
 * @throws An error if the content type ID is not found
 */
export function getContentTypeIdFromRelationship(field: DotCMSContentTypeField): string {
    if (!field?.relationships?.velocityVar) {
        throw new Error('Content type ID not found in relationship field');
    }

    const [contentTypeId] = field.relationships.velocityVar.split('.');

    return contentTypeId;
}


/**
 * Get the header text for a dynamic field
 */
export function getFieldHeader(fieldName: string): string {
    // Use a readable version of the field name
    return fieldName.charAt(0).toUpperCase() + fieldName.slice(1).replace(/([A-Z])/g, ' $1');
}

/**
 * Get the width for a dynamic field
 */
export function getFieldWidth(fieldName: string): string {
    return FIELD_WIDTHS[fieldName] || ''; // Default width for unknown fields
}

/**
 * Determine the column type based on field name
 */
export function getFieldType(fieldName: string): 'string' | 'image' {
    // Check if field name contains common image patterns
    const isImageField = IMAGE_FIELD_PATTERNS.some(pattern =>
        fieldName.toLowerCase().includes(pattern.toLowerCase())
    );

    return isImageField ? 'image' : 'string';
}

/**
 * Create a table column with automatic type detection
 */
export function createColumn(field: string, header: string, width?: string, options: Partial<TableColumn> = {}): TableColumn {
    return {
        field,
        header,
        width,
        type: getFieldType(field),
        ...options
    };
}

/**
 * Extract showFields from field configuration
 */
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
        .map(field => field.trim())
        .filter(field => field.length > 0);
}

/**
 * Check if a value is an image URL based on file extension
 */
export function isImageUrl(value: string): boolean {
    if (!value || typeof value !== 'string') {
        return false;
    }

    const lowercaseValue = value.toLowerCase();

    return IMAGE_EXTENSIONS.some(ext => lowercaseValue.includes(ext));
}
