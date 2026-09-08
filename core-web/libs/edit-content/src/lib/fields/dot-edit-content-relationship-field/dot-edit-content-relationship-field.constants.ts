import { RelationshipType, RelationshipTypes, TableColumn } from './models/relationship.models';

/**
 * Maps cardinality numbers to relationship type values.
 * Used to determine the type of relationship between content types.
 *
 * `Partial` because callers look this up with an arbitrary `cardinality: number` coming from
 * the content-type field, and both of them already handle the miss — one throws, the other
 * returns false. Typing it as a full `Record<number, ...>` would have claimed every number is
 * a valid cardinality and made those guards look dead.
 *
 * The value type is `RelationshipType`, the alias over the constant's values — `RelationshipTypes`
 * itself is a `const` object, so it cannot be used in type position at all.
 *
 * @constant
 *
 * @property {RelationshipType} 0 - One-to-Many relationship type
 * @property {RelationshipType} 1 - Many-to-Many relationship type
 * @property {RelationshipType} 2 - One-to-One relationship type
 * @property {RelationshipType} 3 - Many-to-One relationship type
 */
export const RELATIONSHIP_OPTIONS: Partial<Record<number, RelationshipType>> = {
    0: RelationshipTypes.ONE_TO_MANY,
    1: RelationshipTypes.MANY_TO_MANY,
    2: RelationshipTypes.ONE_TO_ONE,
    3: RelationshipTypes.MANY_TO_ONE
};

/**
 * Key for the showFields variable in field variables
 */
export const SHOW_FIELDS_VARIABLE_KEY = 'showFields';

export const DEFAULT_RELATIONSHIP_COLUMNS: TableColumn[] = [
    { nameField: 'title', header: 'Title', type: 'title' },
    { nameField: 'language', header: 'Language', type: 'language' },
    { nameField: 'status', header: 'Status', type: 'status' }
];

/**
 * Field names that map straight to a dedicated column renderer. Looked up by arbitrary field
 * variable, hence `Partial`; the value type is pinned to `TableColumn['type']` so the mapping
 * cannot drift away from the renderers the table actually supports.
 */
export const SPECIAL_FIELDS: Partial<Record<string, TableColumn['type']>> = {
    title: 'title',
    language: 'language',
    status: 'status'
};

export const STATIC_COLUMNS = 2;
