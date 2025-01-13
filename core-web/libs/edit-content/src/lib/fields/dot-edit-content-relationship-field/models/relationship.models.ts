export enum RelationshipTypes {
    ONE_TO_ONE = '1-1',
    ONE_TO_MANY = '1-n',
    MANY_TO_ONE = 'n-1',
    MANY_TO_MANY = 'n-n'
}

export type RelationshipType = `${RelationshipTypes}`;

export type SelectionMode = 'single' | 'multiple';
