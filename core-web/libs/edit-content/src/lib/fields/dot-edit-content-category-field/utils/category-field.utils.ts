import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotCategoryFieldCategory, DotKeyValueObj } from '../models/dot-category-field.models';

/**
 * Retrieves selected categories from a contentlet.
 *
 * @param {string} variableName - The name of the variable containing the selected categories.
 * @param {DotCMSContentlet} contentlet - The contentlet from which to retrieve the selected categories.
 * @returns {DotKeyValueObj[]} - An array of objects representing the selected categories.
 */
export const getSelectedCategories = (
    variableName: string,
    contentlet: DotCMSContentlet
): DotKeyValueObj[] => {
    if (!contentlet) {
        return [];
    }

    const selectedCategories = contentlet[variableName] || [];

    return selectedCategories.map((obj: DotKeyValueObj) => {
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

/**
 * Deep copy of the matrix
 * @param array
 */
const deepCopy = <T>(array: T[][]): T[][] => {
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
    const newArray = deepCopy<DotCategoryFieldCategory>(array);
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

export const checkIfClickedIsLastItem = (
    index: number,
    categories: DotCategoryFieldCategory[][]
) => {
    return index + 1 === categories.length;
};
