import {
    DotCategory,
    DotCategoryParent,
    DotCMSContentlet,
    DotCMSContentTypeField
} from '@dotcms/dotcms-models';

import { ROOT_CATEGORY_KEY } from '../dot-edit-content-category-field.const';
import {
    DotCategoryFieldItem,
    DotCategoryFieldKeyValueObj,
    HierarchyParent
} from '../models/dot-category-field.models';

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
 * Transforms a DotCategory object into a DotCategoryFieldKeyValueObj object.
 *
 * @param {DotCategory} category
 * @param {string[]} [keyParentPath=[]]
 * @return {*}  {DotCategoryFieldKeyValueObj}
 */
const transformCategory = (
    category: DotCategory,
    keyParentPath: string[] = []
): DotCategoryFieldKeyValueObj => {
    const { key, inode, categoryName, childrenCount } = category;
    const hasChildren = childrenCount > 0;

    const path = getParentPath(category.parentList ?? []);

    return {
        key,
        inode,
        value: categoryName || category?.name,
        hasChildren,
        clicked: hasChildren && keyParentPath.includes(key),
        path
    };
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
    if (Array.isArray(categories)) {
        return categories.map((category) => transformCategory(category, keyParentPath));
    } else {
        return transformCategory(categories, keyParentPath);
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

    return newArray.slice(0, index + 1);
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
 * Check if the clicked item is already loaded
 *
 * @param {DotCategoryFieldItem} event
 * @param {string[]} keyParentPath
 * @return {*}
 */
export const checkIfClickedIsLoaded = (
    event: DotCategoryFieldItem,
    keyParentPath: string[]
): boolean => {
    const categoryKey = event.item.key;
    const lastCategoryKey = keyParentPath[keyParentPath.length - 1];

    if (categoryKey === ROOT_CATEGORY_KEY) {
        return true;
    }

    return categoryKey !== lastCategoryKey;
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
                { key: item.key, value: item.value, inode: item.inode, path: item?.path ?? '' }
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
    if (parentList.length === 0) {
        return '';
    }

    return parentList
        .slice(1)
        .map((parent) => parent.name)
        .join(' / ');
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

/**
 * Retrieves the menu items from a key parent path.
 *
 * @param {DotCategory[][]} array
 * @param {string[]} keyParentPath
 * @return {*}  {MenuItem[]}
 */
export const getMenuItemsFromKeyParentPath = (
    array: DotCategory[][],
    keyParentPath: string[]
): DotCategoryFieldKeyValueObj[] => {
    const flatArray = array.flat();

    return keyParentPath.reduce((array, key) => {
        const category = flatArray.find((item) => item.key === key);

        if (category) {
            return [...array, transformCategory(category, keyParentPath)];
        }

        return array;
    }, []);
};

/***
 * Remove all the empty arrays from the matrix
 * @param {DotCategory[][]} array
 */
export const removeEmptyArrays = (array: DotCategory[][]): DotCategory[][] => {
    return array.filter((item) => item.length > 0);
};
