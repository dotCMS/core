import {
    DotCategory,
    DotCategoryParent,
    DotCMSContentlet,
    DotCMSContentTypeField
} from '@dotcms/dotcms-models';

import { DotCategoryFieldKeyValueObj, HierarchyParent } from '../models/dot-category-field.models';

/**
 * Retrieves and convert selected categories from a contentlet.
 *
 * @param {string} variableName - The name of the variable containing the selected categories.
 * @param {DotCMSContentlet} contentlet - The contentlet from which to retrieve the selected categories.
 * @returns {DotCategoryFieldKeyValueObj[]} - An array of objects representing the selected categories.
 */
export const getSelectedFromContentlet = (
    { variable }: DotCMSContentTypeField,
    contentlet: DotCMSContentlet
): DotCategoryFieldKeyValueObj[] => {
    if (!contentlet || !variable) {
        return [];
    }

    const selectedCategories = contentlet[variable] || [];

    return selectedCategories.map((obj: DotCategoryFieldKeyValueObj) => {
        const key = Object.keys(obj)[0];

        return { key, value: obj[key] };
    });
};

/**
 * Transforms an array of selected HierarchyParent objects into an array of DotCategoryFieldKeyValueObj objects.
 *
 * @param {HierarchyParent[]} selected - The array of selected HierarchyParent objects.
 * @returns {DotCategoryFieldKeyValueObj[]} - The transformed array of DotCategoryFieldKeyValueObj objects.
 */
export const transformToSelectedObject = (
    selected: HierarchyParent[]
): DotCategoryFieldKeyValueObj[] => {
    return selected.map((obj: HierarchyParent) => {
        return {
            key: obj.key,
            value: obj.name,
            inode: obj.inode,
            path: getParentPath(obj.parentList)
        };
    });
};

/**
 * Add calculated properties to the categories
 * @param categories - Single category or array of categories to transform
 * @param keyParentPath - Path of keys to determine clicked state
 * @returns Transformed category or array of transformed categories with additional properties
 */

export const transformCategories = (
    categories: DotCategory | DotCategory[],
    keyParentPath: string[] = []
): DotCategoryFieldKeyValueObj | DotCategoryFieldKeyValueObj[] => {
    const transformCategory = (category: DotCategory): DotCategoryFieldKeyValueObj => {
        const { key, inode, categoryName, childrenCount } = category;
        const hasChildren = childrenCount > 0;

        const path = category.parentList ? getParentPath(category.parentList) : '';

        return {
            key,
            inode,
            value: categoryName || category?.name,
            hasChildren,
            clicked: hasChildren && keyParentPath.includes(key),
            path
        };
    };

    if (Array.isArray(categories)) {
        return categories.map(transformCategory);
    } else {
        return transformCategory(categories);
    }
};

/**
 * Deep copy of the matrix
 * @param array
 */
export const categoryDeepCopy = <T>(array: T[][]): T[][] => {
    return array.map((items) =>
        items.map((item) => (typeof item === 'object' ? { ...item } : item))
    );
};

/**
 *  Remove all the items over of the selected index first level
 * @param array
 * @param index
 */
export const clearCategoriesAfterIndex = (
    array: DotCategory[][],
    index: number
): DotCategory[][] => {
    const newArray = categoryDeepCopy<DotCategory>(array);
    newArray.splice(index + 1);

    return newArray;
};

/**
 * Remove all the items over of the selected index of parentPath
 * @param parentPath
 * @param index
 */
export const clearParentPathAfterIndex = (parentPath: string[], index: number): string[] => {
    return parentPath.slice(0, index);
};

/**
 * Check if the index clicked is the last column
 * @param index
 * @param categories
 */
export const checkIfClickedIsLastItem = (index: number, categories: DotCategory[][]) => {
    return index + 1 === categories.length;
};

/**
 * Updates the array of selected items based on the current selection and most recently interacted with item.
 *
 * @param {DotCategoryFieldKeyValueObj[]} storedSelected - An array of objects currently marked as selected.
 * @param {string[]} selected - An array of 'keys' representing currently selected items as indicated by checkbox inputs.
 * @param {DotCategoryFieldCategory} item - The most recent item interacted with (clicked). Can be already checked (if it is, it needs to be removed from the array upon unchecking).
 * @returns {DotCategoryFieldKeyValueObj[]} - An updated array reflecting the current selected items after considering the status of the 'item'.
 */
export const updateChecked = (
    storedSelected: DotCategoryFieldKeyValueObj[],
    selected: string[],
    item: DotCategoryFieldKeyValueObj
): DotCategoryFieldKeyValueObj[] => {
    let currentChecked = [...storedSelected];

    // If the item is included in the array of selected
    if (selected.includes(item.key)) {
        if (!currentChecked.some((entry) => entry.key === item.key)) {
            currentChecked = [
                ...currentChecked,
                { key: item.key, value: item.value, inode: item.inode }
            ];
        }
    } else {
        // get only the
        currentChecked = currentChecked.filter((entry) => entry.key !== item.key);
    }

    return currentChecked;
};

/**
 * Retrieves the parent path of a given category item.
 *
 * @returns {string} - The parent path of the category item.
 * @param parentList
 */
export const getParentPath = (parentList: DotCategoryParent[]): string => {
    if (parentList) {
        return parentList
            .slice(1)
            .map((parent) => parent.name)
            .join(' / ');
    }

    return '';
};

/**
 * Removes items from an array of objects based on a specified key or an array of keys.
 *
 * @param {DotCategoryFieldKeyValueObj[]} array - The array of objects to remove items from.
 * @param {string | string[]} key - The key (or keys if an array) used to identify the items to remove.
 * @returns {DotCategoryFieldKeyValueObj[]} The updated array without the removed items.
 */
export const removeItemByKey = (
    array: DotCategoryFieldKeyValueObj[],
    key: string | string[]
): DotCategoryFieldKeyValueObj[] => {
    if (Array.isArray(key)) {
        const keysSet = new Set(key);

        return array.filter((item) => !keysSet.has(item.key));
    } else {
        return array.filter((item) => item.key !== key);
    }
};

/**
 * Adds selected items to the existing array of DotCategoryFieldKeyValueObj.
 *
 * @param {DotCategoryFieldKeyValueObj[]} array - The original array.
 * @param {DotCategoryFieldKeyValueObj | DotCategoryFieldKeyValueObj[]} items - The item(s) to be added to the array.
 * @returns {DotCategoryFieldKeyValueObj[]} - The updated array with the selected items added.
 */
export const addSelected = (
    array: DotCategoryFieldKeyValueObj[],
    items: DotCategoryFieldKeyValueObj | DotCategoryFieldKeyValueObj[]
): DotCategoryFieldKeyValueObj[] => {
    const itemsArray = Array.isArray(items) ? items : [items];
    const itemSet = new Set(array.map((item) => item.key));

    const newItems = itemsArray.filter((item) => !itemSet.has(item.key));

    return [...array, ...newItems];
};
