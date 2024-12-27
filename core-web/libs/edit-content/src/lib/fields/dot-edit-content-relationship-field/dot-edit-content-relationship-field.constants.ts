import { RelationshipTypes } from './models/relationship.models';

export const MANDATORY_FIELDS = {
    title: 'title',
    language: 'language',
    modDate: 'modDate'
};

export const MANDATORY_FIRST_COLUMNS = [MANDATORY_FIELDS.title];

export const MANDATORY_LAST_COLUMNS = [MANDATORY_FIELDS.language, MANDATORY_FIELDS.modDate];

export const RELATIONSHIP_OPTIONS = {
    0: RelationshipTypes.ONE_TO_MANY,
    1: RelationshipTypes.MANY_TO_MANY,
    2: RelationshipTypes.ONE_TO_ONE,
    3: RelationshipTypes.MANY_TO_ONE
};
