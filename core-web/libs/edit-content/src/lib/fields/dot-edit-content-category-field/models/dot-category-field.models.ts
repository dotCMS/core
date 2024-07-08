import { DotCategory } from '@dotcms/dotcms-models';

/**
 * Object representing a key-value pair.
 * @interface
 */
export interface DotCategoryFieldKeyValueObj {
    key: string;
    value: string;
    path?: string;
}

/**
 * Represents an clicked item in a DotCategoryField.
 */
export type DotCategoryFieldItem = { index: number; item: DotCategory };

/**
 * Represents a category for a Dot field with a checkbox.
 *
 * @interface
 * @extends DotCategory
 */
export interface DotCategoryFieldCategory extends DotCategory {
    checked?: boolean;
}

export interface DotCategoryFieldCategorySearchedItems
    extends Pick<DotCategory, 'categoryName' | 'key' | 'inode'> {
    path: string;
}
