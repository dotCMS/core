import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { RELATIONSHIP_OPTIONS } from '../dot-edit-content-relationship-field.constants';
import { RelationshipTypes } from '../models/relationship.models';

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
