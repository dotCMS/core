import { DotCMSContentlet } from '@dotcms/dotcms-models';

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
        throw new Error('Invalid relationship type');
    }

    return relationshipType === RelationshipTypes.ONE_TO_ONE ? 'single' : 'multiple';
}

/**
 * Get the relationship from the contentlet.
 *
 * @param contentlet - The contentlet.
 * @returns The relationship.
 */
export function getRelationshipFromContentlet({
    contentlet,
    variable
}: {
    contentlet: DotCMSContentlet;
    variable: string;
}): DotCMSContentlet[] {
    if (!contentlet || !variable) {
        return [];
    }
    

    const relationship = contentlet[variable] || [];

    return relationship;
}
