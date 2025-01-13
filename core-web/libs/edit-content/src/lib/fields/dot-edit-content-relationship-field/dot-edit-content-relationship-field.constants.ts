import { RelationshipTypes } from './models/relationship.models';

/**
 * Maps cardinality numbers to RelationshipTypes enum values.
 * Used to determine the type of relationship between content types.
 *
 * @constant
 * @type {Record<number, RelationshipTypes>}
 *
 * @property {RelationshipTypes} 0 - One-to-Many relationship type
 * @property {RelationshipTypes} 1 - Many-to-Many relationship type
 * @property {RelationshipTypes} 2 - One-to-One relationship type
 * @property {RelationshipTypes} 3 - Many-to-One relationship type
 */

export const RELATIONSHIP_OPTIONS = {
    0: RelationshipTypes.ONE_TO_MANY,
    1: RelationshipTypes.MANY_TO_MANY,
    2: RelationshipTypes.ONE_TO_ONE,
    3: RelationshipTypes.MANY_TO_ONE
};
