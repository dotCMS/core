import { DotCategory } from '@dotcms/dotcms-models';

import {
    categoryDeepCopy,
    clearCategoriesAfterIndex,
    clearParentPathAfterIndex,
    transformSelectedCategories,
    updateChecked
} from './category-field.utils';

import {
    CATEGORY_FIELD_CONTENTLET_MOCK,
    CATEGORY_FIELD_MOCK,
    CATEGORY_LEVEL_1
} from '../mocks/category-field.mocks';
import { DotCategoryFieldKeyValueObj } from '../models/dot-category-field.models';

describe('CategoryFieldUtils', () => {
    describe('getSelectedCategories', () => {
        it('should return an empty array if contentlet is null', () => {
            const result = transformSelectedCategories(CATEGORY_FIELD_MOCK, null);
            expect(result).toEqual([]);
        });

        it('should return parsed the values', () => {
            const expected: DotCategoryFieldKeyValueObj[] = [
                { key: '1f208488057007cedda0e0b5d52ee3b3', value: 'Electrical' },
                { key: 'cb83dc32c0a198fd0ca427b3b587f4ce', value: 'Doors & Windows' }
            ];
            const result = transformSelectedCategories(
                CATEGORY_FIELD_MOCK,
                CATEGORY_FIELD_CONTENTLET_MOCK
            );

            expect(result).toEqual(expected);
        });
    });

    describe('categoryDeepCopy', () => {
        it('should create a deep copy of a two-dimensional array of DotCategoryFieldCategory', () => {
            const array: DotCategory[][] = [
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
            const array: DotCategory[][] = [];
            const copy = categoryDeepCopy(array);

            // The copy should be equal to the original
            expect(copy).toEqual(array);
        });

        it('should handle mixed content arrays correctly', () => {
            const array: DotCategory[][] = [
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
            const array: DotCategory[][] = [
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
            const array: DotCategory[][] = [];
            const index = 0;
            const result = clearCategoriesAfterIndex(array, index);

            // The resulting array should still be empty
            expect(result.length).toBe(0);
            expect(result).toEqual([]);
        });

        it('should handle index greater than array length', () => {
            const array: DotCategory[][] = [
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
                    value: CATEGORY_LEVEL_1[0].categoryName,
                    inode: CATEGORY_LEVEL_1[0].inode
                }
            ];
            const selected = [storedSelected[0].key, CATEGORY_LEVEL_1[1].key];
            const item: DotCategoryFieldKeyValueObj = {
                key: CATEGORY_LEVEL_1[1].key,
                value: CATEGORY_LEVEL_1[1].categoryName,
                inode: CATEGORY_LEVEL_1[1].inode
            };

            const expected: DotCategoryFieldKeyValueObj[] = [...storedSelected, item];

            const result = updateChecked(storedSelected, selected, item);

            expect(result).toEqual(expected);
        });

        it('should not add a duplicate item if it is already in storedSelected', () => {
            const storedSelected: DotCategoryFieldKeyValueObj[] = [
                {
                    key: CATEGORY_LEVEL_1[0].key,
                    value: CATEGORY_LEVEL_1[0].categoryName,
                    inode: CATEGORY_LEVEL_1[1].inode
                }
            ];
            const selected = [storedSelected[0].key];
            const item: DotCategoryFieldKeyValueObj = {
                key: CATEGORY_LEVEL_1[0].key,
                value: CATEGORY_LEVEL_1[0].categoryName,
                inode: CATEGORY_LEVEL_1[1].inode
            };

            const expected: DotCategoryFieldKeyValueObj[] = [...storedSelected];

            const result = updateChecked(storedSelected, selected, item);

            expect(result).toEqual(expected);
        });

        it('should remove an item if it is not selected', () => {
            const storedSelected: DotCategoryFieldKeyValueObj[] = [
                {
                    key: CATEGORY_LEVEL_1[0].key,
                    value: CATEGORY_LEVEL_1[0].categoryName,
                    inode: CATEGORY_LEVEL_1[0].inode
                },
                {
                    key: CATEGORY_LEVEL_1[1].key,
                    value: CATEGORY_LEVEL_1[1].categoryName,
                    inode: CATEGORY_LEVEL_1[1].inode
                }
            ];
            const selected = [storedSelected[0].key];
            const item: DotCategoryFieldKeyValueObj = {
                key: CATEGORY_LEVEL_1[1].key,
                value: CATEGORY_LEVEL_1[1].categoryName,
                inode: CATEGORY_LEVEL_1[1].inode
            };

            const expected: DotCategoryFieldKeyValueObj[] = [
                {
                    key: CATEGORY_LEVEL_1[0].key,
                    value: CATEGORY_LEVEL_1[0].categoryName,
                    inode: CATEGORY_LEVEL_1[0].inode
                }
            ];

            const result = updateChecked(storedSelected, selected, item);

            expect(result).toEqual(expected);
        });

        it('should not remove an item that is not in storedSelected', () => {
            const storedSelected: DotCategoryFieldKeyValueObj[] = [
                {
                    key: CATEGORY_LEVEL_1[0].key,
                    value: CATEGORY_LEVEL_1[0].categoryName,
                    inode: CATEGORY_LEVEL_1[0].inode
                }
            ];
            const selected = [storedSelected[0].key];
            const item: DotCategoryFieldKeyValueObj = {
                key: CATEGORY_LEVEL_1[1].key,
                value: CATEGORY_LEVEL_1[1].categoryName,
                inode: CATEGORY_LEVEL_1[1].inode
            };

            const expected: DotCategoryFieldKeyValueObj[] = [...storedSelected];

            const result = updateChecked(storedSelected, selected, item);

            expect(result).toEqual(expected);
        });
    });
});
