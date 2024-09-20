import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { CategoryFieldStore } from './content-category-field.store';

import {
    CATEGORY_FIELD_CONTENTLET_MOCK,
    CATEGORY_FIELD_MOCK,
    CATEGORY_HIERARCHY_MOCK,
    CATEGORY_LEVEL_1,
    CATEGORY_LEVEL_2,
    MOCK_SELECTED_CATEGORIES_OBJECT
} from '../mocks/category-field.mocks';
import { DotCategoryFieldKeyValueObj } from '../models/dot-category-field.models';
import { CategoriesService } from '../services/categories.service';
import { transformCategories } from '../utils/category-field.utils';

const EMPTY_ARRAY = [];

describe('CategoryFieldStore', () => {
    let spectator: SpectatorService<InstanceType<typeof CategoryFieldStore>>;
    let store: InstanceType<typeof CategoryFieldStore>;
    let categoriesService: SpyObject<CategoriesService>;
    const createService = createServiceFactory({
        service: CategoryFieldStore,
        providers: [mockProvider(CategoriesService), mockProvider(DotHttpErrorManagerService)]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        categoriesService = spectator.inject(CategoriesService);

        categoriesService.getChildren.mockReturnValue(of(CATEGORY_LEVEL_1));
        categoriesService.getSelectedHierarchy.mockReturnValue(of(CATEGORY_HIERARCHY_MOCK));
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should initialize with default state', () => {
        expect(store.categories()).toEqual(EMPTY_ARRAY);
        expect(store.keyParentPath()).toEqual(EMPTY_ARRAY);
        expect(store.state()).toEqual(ComponentStatus.INIT);
        expect(store.selected()).toEqual(EMPTY_ARRAY);
        expect(store.dialog.selected()).toEqual(EMPTY_ARRAY);
        expect(store.dialog.state()).toEqual('closed');
        expect(store.mode()).toEqual('list');
    });

    describe('withMethods', () => {
        it('should set the correct rootCategoryInode and categoriesValue', () => {
            const expectedCategoryValues: DotCategoryFieldKeyValueObj[] = [
                ...MOCK_SELECTED_CATEGORIES_OBJECT
            ];

            store.load({ field: CATEGORY_FIELD_MOCK, contentlet: CATEGORY_FIELD_CONTENTLET_MOCK });
            expect(store.selected()).toEqual(expectedCategoryValues);
            expect(store.rootCategoryInode()).toEqual(CATEGORY_FIELD_MOCK.values);
        });

        describe('getCategories', () => {
            beforeEach(() => {
                store.load({
                    field: CATEGORY_FIELD_MOCK,
                    contentlet: CATEGORY_FIELD_CONTENTLET_MOCK
                });
            });

            it('should fetch the categories with the rootCategoryInode', () => {
                const rootCategoryInode = CATEGORY_FIELD_MOCK.values;

                const getChildrenSpy = jest.spyOn(categoriesService, 'getChildren');

                store.getCategories();
                expect(getChildrenSpy).toHaveBeenCalled();
                expect(getChildrenSpy).toHaveBeenCalledWith(rootCategoryInode);
            });

            it('should fetch the categories with the inode sent', () => {
                const getChildrenSpy = jest
                    .spyOn(categoriesService, 'getChildren')
                    .mockReturnValue(of(CATEGORY_LEVEL_2));

                const item = transformCategories(
                    CATEGORY_LEVEL_1[0]
                ) as DotCategoryFieldKeyValueObj;
                store.getCategories({ index: 0, item });

                expect(getChildrenSpy).toHaveBeenCalledWith(CATEGORY_LEVEL_1[0].inode);
            });

            it('should fetch the initial categories and the get by the clicked category with children', () => {
                categoriesService.getChildren.mockReturnValue(of(CATEGORY_LEVEL_1));
                store.getCategories();
                categoriesService.getChildren.mockReturnValue(of(CATEGORY_LEVEL_2));

                const item = transformCategories(
                    CATEGORY_LEVEL_1[0]
                ) as DotCategoryFieldKeyValueObj;

                store.getCategories({ index: 0, item });

                expect(store.categories().length).toBe(2);
            });
        });
    });

    describe('Dialog', () => {
        beforeEach(() => {
            store.load({ field: CATEGORY_FIELD_MOCK, contentlet: CATEGORY_FIELD_CONTENTLET_MOCK });
            store.openDialog();
        });

        describe('openDialog', () => {
            it('should set the dialog state to open and copy selected items', () => {
                expect(store.dialog.state()).toBe('open');
                expect(store.dialog.selected()).toEqual(MOCK_SELECTED_CATEGORIES_OBJECT);
            });
        });

        describe('closeDialog', () => {
            it('should set the dialog state to closed and clear selected items', () => {
                store.closeDialog();
                expect(store.dialog.state()).toBe('closed');
                expect(store.dialog.selected()).toEqual(EMPTY_ARRAY);
                expect(store.dialog.selected()).toEqual(EMPTY_ARRAY);
            });
        });

        describe('updateSelected', () => {
            it('should add a new item to the dialog selected items', () => {
                expect(store.dialog.selected().length).toBe(MOCK_SELECTED_CATEGORIES_OBJECT.length);

                const newItem: DotCategoryFieldKeyValueObj = {
                    key: CATEGORY_LEVEL_2[0].key,
                    value: CATEGORY_LEVEL_2[0].categoryName,
                    inode: CATEGORY_LEVEL_2[0].inode,
                    path: CATEGORY_LEVEL_2[0].categoryName
                };
                const selectedKeys = [
                    ...MOCK_SELECTED_CATEGORIES_OBJECT.map((item) => item.key),
                    newItem.key
                ];
                store.updateSelected(selectedKeys, newItem);

                const expectedItems = [...MOCK_SELECTED_CATEGORIES_OBJECT, newItem];

                expect(store.dialog.selected()).toEqual(expectedItems);
                expect(store.dialog.selected().length).toBe(
                    MOCK_SELECTED_CATEGORIES_OBJECT.length + 1
                );
            });
        });

        describe('applyDialogSelection', () => {
            it("should update the store's selected items with the dialog's selected items", () => {
                const newItem: DotCategoryFieldKeyValueObj = {
                    key: CATEGORY_LEVEL_2[0].key,
                    value: CATEGORY_LEVEL_2[0].categoryName,
                    inode: CATEGORY_LEVEL_2[0].inode,
                    path: CATEGORY_LEVEL_2[0].categoryName
                };

                store.updateSelected(
                    [...MOCK_SELECTED_CATEGORIES_OBJECT.map((item) => item.key), newItem.key],
                    newItem
                );
                store.applyDialogSelection();

                expect(store.selected()).toEqual([...MOCK_SELECTED_CATEGORIES_OBJECT, newItem]);
            });
        });

        describe('removeSelected', () => {
            it('should remove a single item by key from the dialog selected items', () => {
                store.removeSelected(MOCK_SELECTED_CATEGORIES_OBJECT[0].key);

                expect(store.dialog.selected().length).toBe(
                    MOCK_SELECTED_CATEGORIES_OBJECT.length - 1
                );
                expect(
                    store.dialog
                        .selected()
                        .find((item) => item.key === MOCK_SELECTED_CATEGORIES_OBJECT[0].key)
                ).toBeUndefined();
            });

            it('should remove multiple items by keys from the dialog selected items', () => {
                store.removeSelected(MOCK_SELECTED_CATEGORIES_OBJECT.map((item) => item.key));

                expect(store.dialog.selected()).toEqual(EMPTY_ARRAY);
            });
        });
    });
});
