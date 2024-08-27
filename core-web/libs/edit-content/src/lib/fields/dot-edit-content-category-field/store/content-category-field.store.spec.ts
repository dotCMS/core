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
    SELECTED_LIST_MOCK
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
        expect(store.mode()).toEqual('list');
    });

    describe('withMethods', () => {
        it('should set the correct rootCategoryInode and categoriesValue', () => {
            const expectedCategoryValues: DotCategoryFieldKeyValueObj[] = [
                {
                    key: '1f208488057007cedda0e0b5d52ee3b3',
                    value: 'Cleaning Supplies',
                    inode: '111111',
                    path: 'Cleaning Supplies'
                },
                {
                    key: 'cb83dc32c0a198fd0ca427b3b587f4ce',
                    value: 'Doors & Windows',
                    inode: '22222',
                    path: 'Cleaning Supplies'
                }
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

    describe('withComputed', () => {
        it('should show item after load the values', () => {
            const expectedSelectedValues = SELECTED_LIST_MOCK;
            store.load({ field: CATEGORY_FIELD_MOCK, contentlet: CATEGORY_FIELD_CONTENTLET_MOCK });
            expect(store.selectedCategoriesValues().sort()).toEqual(expectedSelectedValues.sort());

            expect(store.categoryList()).toEqual(EMPTY_ARRAY);
        });
    });
});
