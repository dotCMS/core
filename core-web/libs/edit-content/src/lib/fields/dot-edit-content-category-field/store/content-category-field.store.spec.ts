import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { CategoryFieldStore } from './content-category-field.store';

import {
    CATEGORY_FIELD_CONTENTLET_MOCK,
    CATEGORY_FIELD_MOCK,
    CATEGORY_LEVEL_1,
    CATEGORY_LEVEL_2,
    SELECTED_LIST_MOCK
} from '../mocks/category-field.mocks';
import { DotCategoryFieldKeyValueObj } from '../models/dot-category-field.models';
import { CategoriesService } from '../services/categories.service';

const EMPTY_ARRAY = [];

describe('CategoryFieldStore', () => {
    let spectator: SpectatorService<InstanceType<typeof CategoryFieldStore>>;
    let store: InstanceType<typeof CategoryFieldStore>;
    let categoriesService: SpyObject<CategoriesService>;
    const createService = createServiceFactory({
        service: CategoryFieldStore,
        providers: [
            mockProvider(CategoriesService, {
                getChildren: jest.fn().mockReturnValue(of([]))
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        categoriesService = spectator.inject(CategoriesService);
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should initialize with default state', () => {
        expect(store.categories()).toEqual(EMPTY_ARRAY);
        expect(store.selectedCategoriesValues()).toEqual(EMPTY_ARRAY);
        expect(store.parentPath()).toEqual(EMPTY_ARRAY);
        expect(store.state()).toEqual(ComponentStatus.IDLE);
        // computed
        expect(store.selected()).toEqual(EMPTY_ARRAY);
        expect(store.categoryList()).toEqual(EMPTY_ARRAY);
    });

    describe('withMethods', () => {
        it('should set the correct rootCategoryInode and categoriesValue', () => {
            const expectedCategoryValues: DotCategoryFieldKeyValueObj[] = [
                {
                    key: '33333',
                    value: 'Electrical'
                },
                {
                    key: '22222',
                    value: 'Doors & Windows'
                }
            ];

            store.load(CATEGORY_FIELD_MOCK, CATEGORY_FIELD_CONTENTLET_MOCK);

            expect(store.rootCategoryInode()).toEqual(CATEGORY_FIELD_MOCK.values);
            expect(store.selected()).toEqual(expectedCategoryValues);
        });

        describe('getCategories', () => {
            beforeEach(() => {
                store.load(CATEGORY_FIELD_MOCK, CATEGORY_FIELD_CONTENTLET_MOCK);
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

                store.getCategories({ index: 0, item: CATEGORY_LEVEL_1[0] });

                expect(getChildrenSpy).toHaveBeenCalledWith(CATEGORY_LEVEL_1[0].inode);
            });

            it('should fetch the initial categories and the get by the clicked category with children', () => {
                categoriesService.getChildren.mockReturnValue(of(CATEGORY_LEVEL_1));
                store.getCategories();
                categoriesService.getChildren.mockReturnValue(of(CATEGORY_LEVEL_2));
                store.getCategories({ index: 0, item: CATEGORY_LEVEL_1[0] });

                expect(store.categories().length).toBe(2);
            });
        });
    });

    describe('withComputed', () => {
        it('should show item after load the values', () => {
            const expectedSelectedValues = SELECTED_LIST_MOCK;
            store.load(CATEGORY_FIELD_MOCK, CATEGORY_FIELD_CONTENTLET_MOCK);
            expect(store.selectedCategoriesValues().sort()).toEqual(expectedSelectedValues.sort());

            expect(store.categoryList()).toEqual(EMPTY_ARRAY);
        });
    });
});
