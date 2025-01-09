import { RelationshipTypes } from './models/relationship.models';

export const RELATIONSHIP_OPTIONS = {
    0: RelationshipTypes.ONE_TO_MANY,
    1: RelationshipTypes.MANY_TO_MANY,
    2: RelationshipTypes.ONE_TO_ONE,
    3: RelationshipTypes.MANY_TO_ONE
};
