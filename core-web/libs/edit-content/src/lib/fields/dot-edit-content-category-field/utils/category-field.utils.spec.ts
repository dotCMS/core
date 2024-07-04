import {
    addMetadata,
    categoryDeepCopy,
    clearCategoriesAfterIndex,
    clearParentPathAfterIndex,
    getSelectedCategories,
    updateChecked
} from './category-field.utils';

import {
    CATEGORY_FIELD_CONTENTLET_MOCK,
    CATEGORY_FIELD_MOCK,
    CATEGORY_LEVEL_1
} from '../mocks/category-field.mocks';
import {
    DotCategoryFieldCategory,
    DotCategoryFieldKeyValueObj
} from '../models/dot-category-field.models';

describe('CategoryFieldUtils', () => {
    describe('getSelectedCategories', () => {
        it('should return an empty array if contentlet is null', () => {
            const result = getSelectedCategories(CATEGORY_FIELD_MOCK, null);
            expect(result).toEqual([]);
        });

        it('should return parsed the values', () => {
            const expected: DotCategoryFieldKeyValueObj[] = [
                { key: '33333', value: 'Electrical' },
                { key: '22222', value: 'Doors & Windows' }
            ];
            const result = getSelectedCategories(
                CATEGORY_FIELD_MOCK,
                CATEGORY_FIELD_CONTENTLET_MOCK
            );

            expect(result).toEqual(expected);
        });
    });

    describe('addMetadata', () => {
        it('should `checked: true` when category has children and exist in the parentPath', () => {
            const PARENT_PATH_MOCK = [CATEGORY_LEVEL_1[0].inode];
            const expected = CATEGORY_LEVEL_1.map((item, index) =>
                index === 0 ? { ...item, checked: true } : { ...item, checked: false }
            );

            const result = addMetadata(CATEGORY_LEVEL_1, PARENT_PATH_MOCK);
            expect(result).toEqual(expected);
        });

        it('should `checked: false` when category has children and do not exist in the parentPath', () => {
            const PATH_MOCK = [];
            const expected = CATEGORY_LEVEL_1.map((item) => {
                return { ...item, checked: false };
            });

            const result = addMetadata(CATEGORY_LEVEL_1, PATH_MOCK);
            expect(result).toEqual(expected);
        });

        it('should `checked: false` when category do not has children and do not exist in the parentPath', () => {
            const PATH_MOCK = [];
            const expected = CATEGORY_LEVEL_1.map((item) => {
                return { ...item, checked: false };
            });

            const result = addMetadata(CATEGORY_LEVEL_1, PATH_MOCK);
            expect(result).toEqual(expected);
        });
    });

    describe('categoryDeepCopy', () => {
        it('should create a deep copy of a two-dimensional array of DotCategoryFieldCategory', () => {
            const array: DotCategoryFieldCategory[][] = [
                CATEGORY_LEVEL_1,
                [{ ...CATEGORY_LEVEL_1[0], categoryName: 'New Category' }]
            ];
            const copy = categoryDeepCopy(array);

            // The copy should be equal to the original
            expect(copy).toEqual(array);

            // Modifying an object in the copy should not affect the original
            copy[0][0].categoryName = 'Modified Category';
            expect(array[0][0].categoryName).toBe('Cleaning Supplies');
        });

        it('should create a deep copy of an empty array', () => {
            const array: DotCategoryFieldCategory[][] = [];
            const copy = categoryDeepCopy(array);

            // The copy should be equal to the original
            expect(copy).toEqual(array);
        });

        it('should handle mixed content arrays correctly', () => {
            const array: DotCategoryFieldCategory[][] = [
                CATEGORY_LEVEL_1,
                [{ ...CATEGORY_LEVEL_1[0], categoryName: 'New Category' }]
            ];
            const copy = categoryDeepCopy(array);

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
                CATEGORY_LEVEL_1,
                [{ ...CATEGORY_LEVEL_1[0], categoryName: 'New Category' }],
                [{ ...CATEGORY_LEVEL_1[0], categoryName: 'Another Category' }]
            ];
            const index = 1;
            const result = clearCategoriesAfterIndex(array, index);

            // The resulting array should only contain elements up to the specified index
            expect(result.length).toBe(index + 1);
            expect(result).toEqual([
                CATEGORY_LEVEL_1,
                [{ ...CATEGORY_LEVEL_1[0], categoryName: 'New Category' }]
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
                CATEGORY_LEVEL_1,
                [{ ...CATEGORY_LEVEL_1[0], categoryName: 'New Category' }]
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

    describe('updateChecked', () => {
        it('should add a new item if it is selected and not already in storedSelected', () => {
            const storedSelected: DotCategoryFieldKeyValueObj[] = [
                {
                    key: CATEGORY_LEVEL_1[0].key,
                    value: CATEGORY_LEVEL_1[0].categoryName
                }
            ];
            const selected = [storedSelected[0].key, CATEGORY_LEVEL_1[1].key];
            const item: DotCategoryFieldCategory = { ...CATEGORY_LEVEL_1[1] };

            const expected: DotCategoryFieldKeyValueObj[] = [
                ...storedSelected,
                {
                    key: CATEGORY_LEVEL_1[1].key,
                    value: CATEGORY_LEVEL_1[1].categoryName
                }
            ];

            const result = updateChecked(storedSelected, selected, item);

            expect(result).toEqual(expected);
        });

        it('should not add a duplicate item if it is already in storedSelected', () => {
            const storedSelected: DotCategoryFieldKeyValueObj[] = [
                {
                    key: CATEGORY_LEVEL_1[0].key,
                    value: CATEGORY_LEVEL_1[0].categoryName
                }
            ];
            const selected = [storedSelected[0].key];
            const item: DotCategoryFieldCategory = { ...CATEGORY_LEVEL_1[0] };

            const expected: DotCategoryFieldKeyValueObj[] = [...storedSelected];

            const result = updateChecked(storedSelected, selected, item);

            expect(result).toEqual(expected);
        });

        it('should remove an item if it is not selected', () => {
            const storedSelected: DotCategoryFieldKeyValueObj[] = [
                {
                    key: CATEGORY_LEVEL_1[0].key,
                    value: CATEGORY_LEVEL_1[0].categoryName
                },
                {
                    key: CATEGORY_LEVEL_1[1].key,
                    value: CATEGORY_LEVEL_1[1].categoryName
                }
            ];
            const selected = [storedSelected[0].key];
            const item: DotCategoryFieldCategory = { ...CATEGORY_LEVEL_1[1] };

            const expected: DotCategoryFieldKeyValueObj[] = [
                {
                    key: CATEGORY_LEVEL_1[0].key,
                    value: CATEGORY_LEVEL_1[0].categoryName
                }
            ];

            const result = updateChecked(storedSelected, selected, item);

            expect(result).toEqual(expected);
        });

        it('should not remove an item that is not in storedSelected', () => {
            const storedSelected: DotCategoryFieldKeyValueObj[] = [
                {
                    key: CATEGORY_LEVEL_1[0].key,
                    value: CATEGORY_LEVEL_1[0].categoryName
                }
            ];
            const selected = [storedSelected[0].key];
            const item: DotCategoryFieldCategory = { ...CATEGORY_LEVEL_1[1] };

            const expected: DotCategoryFieldKeyValueObj[] = [...storedSelected];

            const result = updateChecked(storedSelected, selected, item);

            expect(result).toEqual(expected);
        });
    });
});
