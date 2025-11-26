import { DotCategory, DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import {
    addSelected,
    categoryDeepCopy,
    clearCategoriesAfterIndex,
    clearParentPathAfterIndex,
    getSelectedFromContentlet,
    removeEmptyArrays,
    removeItemByKey,
    transformCategories,
    updateChecked,
    getMenuItemsFromKeyParentPath
} from './category-field.utils';

import {
    CATEGORY_FIELD_CONTENTLET_MOCK,
    CATEGORY_FIELD_MOCK,
    CATEGORY_FIELD_VARIABLE_NAME,
    CATEGORY_LEVEL_1
} from '../mocks/category-field.mocks';
import { DotCategoryFieldKeyValueObj } from '../models/dot-category-field.models';

const MOCK_SELECTED_OBJECT: DotCategoryFieldKeyValueObj[] = [
    { key: '1f208488057007cedda0e0b5d52ee3b3', value: 'Cleaning Supplies' },
    { key: 'cb83dc32c0a198fd0ca427b3b587f4ce', value: 'Doors & Windows' }
];
describe('CategoryFieldUtils', () => {
    describe('getSelectedCategories', () => {
        it('should return an empty array if contentlet is null', () => {
            const result = getSelectedFromContentlet(CATEGORY_FIELD_MOCK, null);
            expect(result).toEqual([]);
        });

        it('should return parsed the values', () => {
            const expected: DotCategoryFieldKeyValueObj[] = MOCK_SELECTED_OBJECT;
            const result = getSelectedFromContentlet(
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
                inode: CATEGORY_LEVEL_1[1].inode,
                path: ''
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

    describe('removeItemByKey', () => {
        let array: DotCategoryFieldKeyValueObj[];

        beforeEach(() => {
            array = [
                { key: '1', value: 'Category 1', inode: 'inode1', path: 'path1' },
                { key: '2', value: 'Category 2', inode: 'inode2', path: 'path2' },
                { key: '3', value: 'Category 3', inode: 'inode3', path: 'path3' }
            ];
        });

        it('should remove item with a single key', () => {
            const key = '2';
            const result = removeItemByKey(array, key);
            expect(result).toEqual([
                { key: '1', value: 'Category 1', inode: 'inode1', path: 'path1' },
                { key: '3', value: 'Category 3', inode: 'inode3', path: 'path3' }
            ]);
        });

        it('should remove items with an array of keys', () => {
            const keys = ['1', '3'];
            const result = removeItemByKey(array, keys);
            expect(result).toEqual([
                { key: '2', value: 'Category 2', inode: 'inode2', path: 'path2' }
            ]);
        });

        it('should return the same array if key is not found', () => {
            const key = '4';
            const result = removeItemByKey(array, key);
            expect(result).toEqual(array);
        });

        it('should return the same array if keys array is empty', () => {
            const keys: string[] = [];
            const result = removeItemByKey(array, keys);
            expect(result).toEqual(array);
        });

        it('should handle an empty array input', () => {
            const emptyArray: DotCategoryFieldKeyValueObj[] = [];
            const key = '1';
            const result = removeItemByKey(emptyArray, key);
            expect(result).toEqual([]);
        });

        it('should handle an empty array input with keys array', () => {
            const emptyArray: DotCategoryFieldKeyValueObj[] = [];
            const keys = ['1', '2'];
            const result = removeItemByKey(emptyArray, keys);
            expect(result).toEqual([]);
        });
    });
    describe('addSelected', () => {
        let array: DotCategoryFieldKeyValueObj[];

        beforeEach(() => {
            array = [
                { key: '1', value: 'Category 1', inode: 'inode1', path: 'path1' },
                { key: '2', value: 'Category 2', inode: 'inode2', path: 'path2' }
            ];
        });

        it('should add a single item to the array', () => {
            const newItem = { key: '3', value: 'Category 3', inode: 'inode3', path: 'path3' };
            const result = addSelected(array, newItem);
            expect(result).toEqual([
                { key: '1', value: 'Category 1', inode: 'inode1', path: 'path1' },
                { key: '2', value: 'Category 2', inode: 'inode2', path: 'path2' },
                { key: '3', value: 'Category 3', inode: 'inode3', path: 'path3' }
            ]);
        });

        it('should add multiple items to the array', () => {
            const newItems = [
                { key: '3', value: 'Category 3', inode: 'inode3', path: 'path3' },
                { key: '4', value: 'Category 4', inode: 'inode4', path: 'path4' }
            ];
            const result = addSelected(array, newItems);
            expect(result).toEqual([
                { key: '1', value: 'Category 1', inode: 'inode1', path: 'path1' },
                { key: '2', value: 'Category 2', inode: 'inode2', path: 'path2' },
                { key: '3', value: 'Category 3', inode: 'inode3', path: 'path3' },
                { key: '4', value: 'Category 4', inode: 'inode4', path: 'path4' }
            ]);
        });

        it('should not add duplicate items to the array', () => {
            const newItem = { key: '2', value: 'Category 2', inode: 'inode2', path: 'path2' };
            const result = addSelected(array, newItem);
            expect(result).toEqual([
                { key: '1', value: 'Category 1', inode: 'inode1', path: 'path1' },
                { key: '2', value: 'Category 2', inode: 'inode2', path: 'path2' }
            ]);
        });

        it('should handle adding items to an empty array', () => {
            const emptyArray: DotCategoryFieldKeyValueObj[] = [];
            const newItem = { key: '1', value: 'Category 1', inode: 'inode1', path: 'path1' };
            const result = addSelected(emptyArray, newItem);
            expect(result).toEqual([
                { key: '1', value: 'Category 1', inode: 'inode1', path: 'path1' }
            ]);
        });

        it('should handle adding an empty array of items', () => {
            const newItems: DotCategoryFieldKeyValueObj[] = [];
            const result = addSelected(array, newItems);
            expect(result).toEqual(array);
        });

        it('should add items correctly when array is empty', () => {
            const emptyArray: DotCategoryFieldKeyValueObj[] = [];
            const newItems = [
                { key: '1', value: 'Category 1', inode: 'inode1', path: 'path1' },
                { key: '2', value: 'Category 2', inode: 'inode2', path: 'path2' }
            ];
            const result = addSelected(emptyArray, newItems);
            expect(result).toEqual(newItems);
        });
    });

    describe('transformCategories', () => {
        const keyParentPath = ['1']; // make true clicked

        it('should transform a single category', () => {
            const category: DotCategory = {
                key: '1',
                inode: 'inode1',
                categoryName: 'Category 1',
                childrenCount: 2,
                active: true,
                categoryVelocityVarName: '',
                description: null,
                iDate: 0,
                identifier: null,
                keywords: null,
                modDate: 0,
                owner: '',
                sortOrder: 0,
                type: '',
                parentList: [
                    { key: 'root', name: 'Root Parent', inode: 'rootInode' },
                    {
                        key: 'parent1',
                        name: 'Parent 1',
                        inode: 'parentInode1'
                    }
                ]
            };

            const result = transformCategories(category, keyParentPath);

            expect(result).toEqual({
                key: '1',
                inode: 'inode1',
                value: 'Category 1',
                hasChildren: true,
                clicked: true, // from keyParentPath
                path: 'Parent 1'
            });
        });

        it('should transform an array of categories', () => {
            const categories: DotCategory[] = [
                {
                    key: '1',
                    inode: 'inode1',
                    categoryName: 'Category 1',
                    childrenCount: 2,
                    active: true,
                    categoryVelocityVarName: '',
                    description: null,
                    iDate: 0,
                    identifier: null,
                    keywords: null,
                    modDate: 0,
                    owner: '',
                    sortOrder: 0,
                    type: '',
                    parentList: [
                        { key: 'root', name: 'Root Parent', inode: 'rootInode' },
                        {
                            key: 'parent1',
                            name: 'Parent 1',
                            inode: 'parentInode1'
                        }
                    ]
                },
                {
                    key: '2',
                    inode: 'inode2',
                    categoryName: 'Category 2',
                    childrenCount: 0,
                    active: true,
                    categoryVelocityVarName: '',
                    description: null,
                    iDate: 0,
                    identifier: null,
                    keywords: null,
                    modDate: 0,
                    owner: '',
                    sortOrder: 0,
                    type: '',
                    parentList: [
                        { key: 'root', name: 'Root Parent', inode: 'rootInode' },
                        {
                            key: 'parent1',
                            name: 'Parent 1',
                            inode: 'parentInode1'
                        }
                    ]
                }
            ];

            const result = transformCategories(categories, keyParentPath);

            expect(result).toEqual([
                {
                    key: '1',
                    inode: 'inode1',
                    value: 'Category 1',
                    hasChildren: true,
                    clicked: true,
                    path: 'Parent 1'
                },
                {
                    key: '2',
                    inode: 'inode2',
                    value: 'Category 2',
                    hasChildren: false,
                    clicked: false,
                    path: 'Parent 1'
                }
            ]);
        });

        it('should handle category with no parentList', () => {
            const category: DotCategory = {
                key: '1',
                inode: 'inode1',
                categoryName: 'Category 1',
                childrenCount: 0,
                active: true,
                categoryVelocityVarName: '',
                description: null,
                iDate: 0,
                identifier: null,
                keywords: null,
                modDate: 0,
                owner: '',
                sortOrder: 0,
                type: ''
            };

            const result = transformCategories(category, keyParentPath);

            expect(result).toEqual({
                key: '1',
                inode: 'inode1',
                value: 'Category 1',
                hasChildren: false,
                clicked: false,
                path: ''
            });
        });

        it('should handle empty array of categories', () => {
            const categories: DotCategory[] = [];

            const result = transformCategories(categories, keyParentPath);

            expect(result).toEqual([]);
        });

        //
        it('should build the breadcrumb according to categories', () => {
            const array: DotCategory[][] = [
                [
                    {
                        key: '1',
                        inode: 'inode1',
                        categoryName: 'Category 1',
                        childrenCount: 0,
                        active: true,
                        categoryVelocityVarName: '',
                        description: null,
                        iDate: 0,
                        identifier: null,
                        keywords: null,
                        modDate: 0,
                        owner: '',
                        sortOrder: 0,
                        type: ''
                    }
                ]
            ];
            const result = getMenuItemsFromKeyParentPath(array, keyParentPath);
            expect(result).toEqual([
                {
                    key: '1',
                    inode: 'inode1',
                    value: 'Category 1',
                    hasChildren: false,
                    clicked: false,
                    path: ''
                }
            ]);
        });
    });
    describe('transformSelectedCategories', () => {
        it('should return an empty array if contentlet is not provided', () => {
            const result = getSelectedFromContentlet(CATEGORY_FIELD_MOCK, null as never);
            expect(result).toEqual([]);
        });

        it('should return an empty array if variable is not provided', () => {
            const variableField: DotCMSContentTypeField = { ...CATEGORY_FIELD_MOCK, variable: '' };
            const result = getSelectedFromContentlet(variableField, CATEGORY_FIELD_CONTENTLET_MOCK);
            expect(result).toEqual([]);
        });

        it('should return an empty array if selected categories are not present in contentlet', () => {
            const variableField: DotCMSContentTypeField = {
                ...CATEGORY_FIELD_MOCK,
                variable: 'nonexistentField'
            };
            const result = getSelectedFromContentlet(variableField, CATEGORY_FIELD_CONTENTLET_MOCK);
            expect(result).toEqual([]);
        });

        it('should transform selected categories correctly', () => {
            const result = getSelectedFromContentlet(
                CATEGORY_FIELD_MOCK,
                CATEGORY_FIELD_CONTENTLET_MOCK
            );
            expect(result).toEqual(MOCK_SELECTED_OBJECT);
        });

        it('should handle empty selected categories in contentlet', () => {
            const contentletWithEmptyCategories: DotCMSContentlet = {
                ...CATEGORY_FIELD_CONTENTLET_MOCK,
                [CATEGORY_FIELD_VARIABLE_NAME]: []
            };
            const result = getSelectedFromContentlet(
                CATEGORY_FIELD_MOCK,
                contentletWithEmptyCategories
            );
            expect(result).toEqual([]);
        });

        it('should return the same array if there are no empty arrays', () => {
            const array: DotCategory[][] = [
                [{ key: '1', categoryName: 'Category 1' } as DotCategory],
                [{ key: '2', categoryName: 'Category 2' } as DotCategory]
            ];
            const result = removeEmptyArrays(array);
            expect(result).toEqual(array);
        });

        it('should return an empty array if all arrays are empty', () => {
            const array: DotCategory[][] = [[], [], []];
            const result = removeEmptyArrays(array);
            expect(result).toEqual([]);
        });
    });
});
