import { deepCopy } from '@angular-devkit/core';

import {
    addMetadata,
    clearCategoriesAfterIndex,
    clearParentPathAfterIndex,
    getSelectedCategories
} from './category-field.utils';

import {
    CATEGORIES_WITH_CHILD_MOCK,
    CATEGORY_FIELD_CONTENTLET_MOCK,
    CATEGORY_FIELD_VARIABLE_NAME
} from '../mocks/category-field.mocks';
import { DotCategoryFieldCategory, DotKeyValueObj } from '../models/dot-category-field.models';

describe('getSelectedCategories', () => {
    it('should return an empty array if contentlet is null', () => {
        const result = getSelectedCategories(CATEGORY_FIELD_VARIABLE_NAME, null);
        expect(result).toEqual([]);
    });
    it('should return an empty array if variable is null', () => {
        const result = getSelectedCategories(null, CATEGORY_FIELD_CONTENTLET_MOCK);
        expect(result).toEqual([]);
    });
    it("should return an empty array if variable is ''", () => {
        const result = getSelectedCategories('', CATEGORY_FIELD_CONTENTLET_MOCK);
        expect(result).toEqual([]);
    });

    it('should return parsed the values', () => {
        const expected: DotKeyValueObj[] = [
            { key: '11111', value: 'Cleaning Supplies' },
            { key: '22222', value: 'Concrete & Cement' }
        ];
        const result = getSelectedCategories(
            CATEGORY_FIELD_VARIABLE_NAME,
            CATEGORY_FIELD_CONTENTLET_MOCK
        );

        expect(result).toEqual(expected);
    });
});

describe('addMetadata', () => {
    it('should `checked: true` when category has children and exist in the parentPath', () => {
        const PARENT_PATH_MOCK = [CATEGORIES_WITH_CHILD_MOCK[0].inode];
        const expected = [{ ...CATEGORIES_WITH_CHILD_MOCK[0], checked: true }];

        const result = addMetadata(CATEGORIES_WITH_CHILD_MOCK, PARENT_PATH_MOCK);
        expect(result).toEqual(expected);
    });
    it('should `checked: false` when category has children and do not exist in the parentPath', () => {
        const PATH_MOCK = [];
        const expected = [{ ...CATEGORIES_WITH_CHILD_MOCK[0], checked: false }];

        const result = addMetadata(CATEGORIES_WITH_CHILD_MOCK, PATH_MOCK);
        expect(result).toEqual(expected);
    });

    it('should `checked: false` when category do not has children and do not exist in the parentPath', () => {
        const PATH_MOCK = [];
        const expected = [{ ...CATEGORIES_WITH_CHILD_MOCK[0], checked: false }];

        const result = addMetadata(CATEGORIES_WITH_CHILD_MOCK, PATH_MOCK);
        expect(result).toEqual(expected);
    });
});

describe('deepCopy', () => {
    it('should create a deep copy of a two-dimensional array of DotCategoryFieldCategory', () => {
        const array: DotCategoryFieldCategory[][] = [
            CATEGORIES_WITH_CHILD_MOCK,
            [{ ...CATEGORIES_WITH_CHILD_MOCK[0], categoryName: 'New Category' }]
        ];
        const copy = deepCopy(array);

        // The copy should be equal to the original
        expect(copy).toEqual(array);

        // Modifying an object in the copy should not affect the original
        copy[0][0].categoryName = 'Modified Category';
        expect(array[0][0].categoryName).toBe('Adhesives & Sealants');
    });

    it('should create a deep copy of an empty array', () => {
        const array: DotCategoryFieldCategory[][] = [];
        const copy = deepCopy(array);

        // The copy should be equal to the original
        expect(copy).toEqual(array);
    });

    it('should handle mixed content arrays correctly', () => {
        const array: DotCategoryFieldCategory[][] = [
            CATEGORIES_WITH_CHILD_MOCK,
            [{ ...CATEGORIES_WITH_CHILD_MOCK[0], categoryName: 'New Category' }]
        ];
        const copy = deepCopy(array);

        // The copy should be equal to the original
        expect(copy).toEqual(array);

        // Modifying an object in the copy should not affect the original
        copy[1][0].categoryName = 'Another Modified Category';
        expect(array[1][0].categoryName).toBe('New Category');
    });
});

describe('clearCategoriesAfterIndex', () => {
    it('should remove all items after the specified index + 1', () => {
        const array: DotCategoryFieldCategory[][] = [
            CATEGORIES_WITH_CHILD_MOCK,
            [{ ...CATEGORIES_WITH_CHILD_MOCK[0], categoryName: 'New Category' }],
            [{ ...CATEGORIES_WITH_CHILD_MOCK[0], categoryName: 'Another Category' }]
        ];
        const index = 1;
        const result = clearCategoriesAfterIndex(array, index);

        // The resulting array should only contain elements up to the specified index
        expect(result.length).toBe(index + 1);
        expect(result).toEqual([
            CATEGORIES_WITH_CHILD_MOCK,
            [{ ...CATEGORIES_WITH_CHILD_MOCK[0], categoryName: 'New Category' }]
        ]);
    });

    it('should handle an empty array', () => {
        const array: DotCategoryFieldCategory[][] = [];
        const index = 0;
        const result = clearCategoriesAfterIndex(array, index);

        // The resulting array should still be empty
        expect(result.length).toBe(0);
        expect(result).toEqual([]);
    });

    it('should handle index greater than array length', () => {
        const array: DotCategoryFieldCategory[][] = [
            CATEGORIES_WITH_CHILD_MOCK,
            [{ ...CATEGORIES_WITH_CHILD_MOCK[0], categoryName: 'New Category' }]
        ];
        const index = 5;
        const result = clearCategoriesAfterIndex(array, index);

        // The resulting array should remain unchanged
        expect(result.length).toBe(array.length);
        expect(result).toEqual(array);
    });
});

describe('clearParentPathAfterIndex', () => {
    it('should remove all items after the specified index', () => {
        const parentPath = ['item1', 'item2', 'item3', 'item4'];
        const index = 2;
        const result = clearParentPathAfterIndex(parentPath, index);

        // The resulting array should only contain elements up to the specified index
        expect(result.length).toBe(index);
        expect(result).toEqual(['item1', 'item2']);
    });

    it('should handle an empty array', () => {
        const parentPath: string[] = [];
        const index = 0;
        const result = clearParentPathAfterIndex(parentPath, index);

        // The resulting array should still be empty
        expect(result.length).toBe(0);
        expect(result).toEqual([]);
    });

    it('should handle index greater than array length', () => {
        const parentPath = ['item1', 'item2'];
        const index = 5;
        const result = clearParentPathAfterIndex(parentPath, index);

        // The resulting array should remain unchanged
        expect(result.length).toBe(parentPath.length);
        expect(result).toEqual(parentPath);
    });

    it('should handle index equal to 0', () => {
        const parentPath = ['item1', 'item2', 'item3'];
        const index = 0;
        const result = clearParentPathAfterIndex(parentPath, index);

        // The resulting array should be empty
        expect(result.length).toBe(0);
        expect(result).toEqual([]);
    });
});
