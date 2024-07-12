/**
 * Object representing a key-value pair.
 * @interface
 */
export interface DotCategoryFieldKeyValueObj {
    key: string;
    value: string;
    inode?: string;
    path?: string;
    clicked?: boolean;
    hasChildren?: boolean;
}

/**
 * Represents an clicked item in a DotCategoryField.
 */
export type DotCategoryFieldItem = { index: number; item: DotCategoryFieldKeyValueObj };

/**
 * Represents an event when a row is selected in a table.
 *
 * @template T - The type of the data associated with the selected row.
 */
export interface DotTableRowSelectEvent<T = never> {
    originalEvent?: Event;
    data?: T;
    type?: string;
    index?: number;
}

/**
 * Represents an event emitted when the header checkbox of a table is selected.
 */
export interface DotTableHeaderCheckboxSelectEvent {
    originalEvent?: Event;
    checked: boolean;
}

/**
 * Represents the view mode for a category field.
 */
export type CategoryFieldViewMode = 'list' | 'search';
