import {
    DotCMSClazz,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSClazzes,
    FeaturedFlags
} from '@dotcms/dotcms-models';

import {
    RELATIONSHIP_OPTIONS,
    SHOW_FIELDS_VARIABLE_KEY,
    SPECIAL_FIELDS
} from '../dot-edit-content-relationship-field.constants';
import { RelationshipTypes, TableColumn } from '../models/relationship.models';
import { RelationshipFieldState } from '../store/relationship-field.store';

/**
 * Get the selection mode by cardinality and parent/child role.
 *
 * Mirrors the backend logic in ContentletRelationshipRecords.doesAllowOnlyOne():
 *   - Child side (isParentField=false): only MANY_TO_MANY allows multiple
 *   - Parent side (isParentField=true): only ONE_TO_ONE allows single
 *
 * @param cardinality - The cardinality of the relationship (0-3).
 * @param isParentField - Whether this content type is the parent in the relationship.
 * @returns The selection mode.
 */
export function getSelectionModeByCardinality(
    cardinality: number,
    isParentField?: boolean
): RelationshipFieldState['selectionMode'] {
    const relationshipType = RELATIONSHIP_OPTIONS[cardinality];

    if (!relationshipType) {
        throw new Error(`Invalid relationship type for cardinality: ${cardinality}`);
    }

    if (isParentField === false) {
        // Child side: only MANY_TO_MANY allows multiple selection
        return relationshipType === RelationshipTypes.MANY_TO_MANY ? 'multiple' : 'single';
    }

    // Parent side (or isParentField not provided - backward compatible)
    const isSingleMode =
        relationshipType === RelationshipTypes.ONE_TO_ONE ||
        relationshipType === RelationshipTypes.MANY_TO_ONE;

    return isSingleMode ? 'single' : 'multiple';
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

/**
 * Get the header for a field.
 *
 * @param fieldName - The name of the field.
 * @returns The header for the field.
 */
export const getFieldHeader = (fieldName: string): string => {
    return (
        fieldName
            // Insert space before capital letters that are followed by lowercase letters (e.g., "APIResponse" -> "API Response")
            .replace(/([A-Z]+)([A-Z][a-z])/g, '$1 $2')
            // Insert space between lowercase and uppercase (e.g., "fieldName" -> "field Name")
            .replace(/([a-z\d])([A-Z])/g, '$1 $2')
            // Insert space between letters and numbers (e.g., "field1Name" -> "field 1 Name")
            .replace(/([a-zA-Z])(\d)/g, '$1 $2')
            .replace(/(\d)([a-zA-Z])/g, '$1 $2')
            .trim()
    );
};

/**
 * Extracts the show fields from a field.
 *
 * @param field - The field to extract the show fields from.
 * @returns The show fields.
 */
export const extractShowFields = (field: DotCMSContentTypeField): string[] | null => {
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
};

/**
 * Get the type of a field.
 *
 * @param fieldName - The name of the field.
 * @param dataColumns - The data columns.
 * @returns The type of the field.
 */
export const getTypeField = (
    fieldName: string,
    dataColumns: DotCMSContentTypeField[]
): TableColumn['type'] => {
    const isSpecialField = SPECIAL_FIELDS[fieldName];

    if (isSpecialField) {
        return isSpecialField;
    }

    if (dataColumns.length > 0) {
        const field = dataColumns.find((field) => field.variable === fieldName);
        return field && isImageField(field.clazz) ? 'image' : 'text';
    }

    return 'text';
};

/**
 * Check if a field is an image field.
 *
 * @param clazz - The class of the field.
 * @returns True if the field is an image field, false otherwise.
 */
export const isImageField = (clazz: DotCMSClazz): boolean => {
    return (
        clazz === DotCMSClazzes.IMAGE ||
        clazz === DotCMSClazzes.FILE ||
        clazz === DotCMSClazzes.BINARY
    );
};

/**
 * Check if the new editor is enabled for a content type.
 *
 * @param contentType - The content type to check.
 * @returns True if the new editor is enabled, false otherwise.
 */
export const isNewEditorEnabled = (contentType: DotCMSContentType): boolean => {
    return contentType.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] === true;
};

/**
 * Determines if the relationship field requires a cardinality constraint check
 * in the "Select Existing Content" dialog.
 *
 * This is needed when the current content is the parent side of a ONE_TO_ONE
 * or ONE_TO_MANY relationship, because each child can only belong to one parent.
 *
 * @param cardinality - The cardinality value of the relationship (0-3).
 * @param isParentField - Whether the current content type is the parent in the relationship.
 * @returns True if constraint check is needed, false otherwise.
 */
export function needsCardinalityConstraintCheck(
    cardinality: number,
    isParentField: boolean
): boolean {
    if (!isParentField) {
        return false;
    }

    const type = RELATIONSHIP_OPTIONS[cardinality];

    return type === RelationshipTypes.ONE_TO_ONE || type === RelationshipTypes.ONE_TO_MANY;
}
