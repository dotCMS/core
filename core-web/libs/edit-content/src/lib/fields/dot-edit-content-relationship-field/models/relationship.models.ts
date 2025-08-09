/**
 * Enum representing the types of relationships between content types.
 * - ONE_TO_ONE: One-to-one relationship (1-1)
 * - ONE_TO_MANY: One-to-many relationship (1-n)
 * - MANY_TO_ONE: Many-to-one relationship (n-1)
 * - MANY_TO_MANY: Many-to-many relationship (n-n)
 */
export enum RelationshipTypes {
    ONE_TO_ONE = '1-1',
    ONE_TO_MANY = '1-n',
    MANY_TO_ONE = 'n-1',
    MANY_TO_MANY = 'n-n'
}

/**
 * String literal type representing a relationship type value.
 * Example: '1-1', '1-n', 'n-1', 'n-n'
 */
export type RelationshipType = `${RelationshipTypes}`;

/**
 * Selection mode for relationship fields.
 * - 'single': Only one item can be selected.
 * - 'multiple': Multiple items can be selected.
 */
export type SelectionMode = 'single' | 'multiple';

/**
 * Interface representing a table column configuration for the relationship field table.
 *
 * @property field - The field name associated with the column.
 * @property header - The display header for the column.
 * @property width - (Optional) The width of the column (e.g., '10rem').
 * @property frozen - (Optional) Whether the column is frozen (fixed position).
 * @property alignFrozen - (Optional) Alignment of the frozen column ('left' or 'right').
 * @property type - The type of data in the column ('string' or 'image').
 */
export interface TableColumn {
    field: string;
    header: string;
    width?: string;
    frozen?: boolean;
    alignFrozen?: 'left' | 'right';
    type: 'string' | 'image';
}
