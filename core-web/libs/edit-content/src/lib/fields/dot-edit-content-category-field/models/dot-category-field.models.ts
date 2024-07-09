import { DotCategory } from '@dotcms/dotcms-models';

/**
 * Object representing a key-value pair.
 * @interface
 */
export interface DotCategoryFieldKeyValueObj {
    key: string;
    inode: string;
    value: string;
    path?: string;
    clicked?: boolean;
    hasChildren?: boolean;
}

/**
 * Represents an clicked item in a DotCategoryField.
 */
export type DotCategoryFieldItem = { index: number; item: DotCategoryFieldKeyValueObj };

/**
 * Represents a category for a Dot field with a checkbox.
 *
 * @interface
 * @extends DotCategory
 */
export interface DotCategoryFieldCategory extends DotCategory {
    checked?: boolean;
}

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
