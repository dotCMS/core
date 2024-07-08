import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import {
    DotCategoryFieldCategory,
    DotCategoryFieldCategorySearchedItems,
    DotCategoryFieldKeyValueObj
} from '../models/dot-category-field.models';

/**
 * Retrieves selected categories from a contentlet.
 *
 * @param {string} variableName - The name of the variable containing the selected categories.
 * @param {DotCMSContentlet} contentlet - The contentlet from which to retrieve the selected categories.
 * @returns {DotCategoryFieldKeyValueObj[]} - An array of objects representing the selected categories.
 */
export const getSelectedCategories = (
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
 * Add calculated properties to the categories
 * @param categories
 * @param parentPath
 */
export const addMetadata = (
    categories: DotCategoryFieldCategory[],
    parentPath: string[]
): DotCategoryFieldCategory[] => {
    return categories.map((category) => {
        return {
            ...category,
            checked: parentPath.includes(category.inode) && category.childrenCount > 0
        };
    });
};

export const transformedData = (
    categories: DotCategoryFieldCategory[]
): DotCategoryFieldCategorySearchedItems[] => {
    return categories.map((item) => {
        // const path = item.parentList.map((parent) => parent.categoryName).join(' / ');
        const path = item.parentList
            .slice(1)
            .map((parent) => parent.categoryName)
            .join(' / ');

        return {
            categoryName: item.categoryName,
            key: item.key,
            inode: item.inode,
            path: path
        };
    });
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
    array: DotCategoryFieldCategory[][],
    index: number
): DotCategoryFieldCategory[][] => {
    const newArray = categoryDeepCopy<DotCategoryFieldCategory>(array);
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
export const checkIfClickedIsLastItem = (
    index: number,
    categories: DotCategoryFieldCategory[][]
) => {
    return index + 1 === categories.length;
};

/**
 * Update storedSelected from search results.
 *
 * @param {DotCategoryFieldKeyValueObj[]} storedSelected - The array of items currently stored as selected.
 * @param {DotCategoryFieldCategorySearchedItems[]} selected - The array of items that were selected from the search results.
 * @param {DotCategoryFieldCategory[]} searchedItems - The array of items obtained from the search.
 * @returns {DotCategoryFieldKeyValueObj[]} The updated array of selected items.
 */
export const updateSelectedFromSearch = (
    storedSelected: DotCategoryFieldKeyValueObj[],
    selected: DotCategoryFieldCategorySearchedItems[],
    searchedItems: DotCategoryFieldCategory[]
): DotCategoryFieldKeyValueObj[] => {
    // Create a map for quick lookup of searched items
    const searchedItemsMap = new Map(searchedItems.map((item) => [item.key, item]));

    // Remove items from storedSelected that are in searchedItems
    const currentChecked = storedSelected.filter((item) => !searchedItemsMap.has(item.key));

    // Add new selected items to currentChecked
    for (const item of selected) {
        if (!currentChecked.some((checkedItem) => checkedItem.key === item.key)) {
            currentChecked.push({
                value: item.categoryName,
                key: item.key,
                path: item.path
            });
        }
    }

    return currentChecked;
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
    item: DotCategoryFieldCategory
): DotCategoryFieldKeyValueObj[] => {
    let currentChecked = [...storedSelected];

    if (selected.includes(item.key)) {
        if (!currentChecked.some((entry) => entry.key === item.key)) {
            currentChecked = [...currentChecked, { key: item.key, value: item.categoryName }];
        }
    } else {
        currentChecked = currentChecked.filter((entry) => entry.key !== item.key);
    }

    return currentChecked;
};
