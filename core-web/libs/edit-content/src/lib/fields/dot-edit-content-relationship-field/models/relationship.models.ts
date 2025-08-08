/**
 * Constants defining the different types of relationships that can exist between content types.
 * These represent the cardinality of relationships in dotCMS.
 */
export const RelationshipTypes = {
    ONE_TO_ONE: '1-1',
    ONE_TO_MANY: '1-n',
    MANY_TO_ONE: 'n-1',
    MANY_TO_MANY: 'n-n'
} as const;

/**
 * TypeScript type representing the valid relationship type values.
 * Extracted from the RelationshipTypes constant to ensure type safety.
 */
export type RelationshipType = (typeof RelationshipTypes)[keyof typeof RelationshipTypes];

/**
 * Constants defining the selection modes available for relationship fields.
 * These determine how users can interact with relationship field inputs.
 */
export const SelectionModes = {
    SINGLE: 'single',
    MULTIPLE: 'multiple'
} as const;

/**
 * TypeScript type representing the valid selection mode values.
 * Extracted from the SelectionModes constant to ensure type safety.
 */
export type SelectionMode = (typeof SelectionModes)[keyof typeof SelectionModes];
