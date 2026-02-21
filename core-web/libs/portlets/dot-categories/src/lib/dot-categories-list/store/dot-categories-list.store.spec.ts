import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotCategoriesService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCategory } from '@dotcms/dotcms-models';

import { DotCategoriesListStore } from './dot-categories-list.store';

const MOCK_CATEGORIES: DotCategory[] = [
    {
        categoryName: 'Category 1',
        key: 'cat1',
        categoryVelocityVarName: 'cat1Var',
        sortOrder: 0,
        active: true,
        inode: 'inode-1',
        identifier: 'id-1',
        type: 'Category',
        childrenCount: 3,
        description: '',
        keywords: '',
        iDate: Date.now(),
        owner: 'system'
    } as DotCategory,
    {
        categoryName: 'Category 2',
        key: 'cat2',
        categoryVelocityVarName: 'cat2Var',
        sortOrder: 1,
        active: true,
        inode: 'inode-2',
        identifier: 'id-2',
        type: 'Category',
        childrenCount: 0,
        description: '',
        keywords: '',
        iDate: Date.now(),
        owner: 'system'
    } as DotCategory
];

const MOCK_PAGINATED_RESPONSE = {
    entity: MOCK_CATEGORIES,
    pagination: { currentPage: 1, perPage: 25, totalEntries: 100 }
};

describe('DotCategoriesListStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotCategoriesListStore>>;
    let store: InstanceType<typeof DotCategoriesListStore>;
    let categoriesService: jest.Mocked<DotCategoriesService>;

    const createService = createServiceFactory({
        service: DotCategoriesListStore,
        providers: [
            mockProvider(DotCategoriesService, {
                getCategoriesPaginated: jest.fn().mockReturnValue(of(MOCK_PAGINATED_RESPONSE)),
                getChildrenPaginated: jest.fn().mockReturnValue(of(MOCK_PAGINATED_RESPONSE)),
                createCategory: jest.fn().mockReturnValue(of({ entity: MOCK_CATEGORIES[0] })),
                updateCategory: jest.fn().mockReturnValue(of({ entity: MOCK_CATEGORIES[0] })),
                deleteCategories: jest
                    .fn()
                    .mockReturnValue(of({ entity: { successCount: 2, fails: [] } })),
                exportCategories: jest.fn().mockReturnValue(of(undefined)),
                importCategories: jest.fn().mockReturnValue(of({ entity: {} }))
            }),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        categoriesService = spectator.inject(
            DotCategoriesService
        ) as jest.Mocked<DotCategoriesService>;
        spectator.flushEffects();
    });

    describe('Initial State', () => {
        it('should have default initial state values after effect triggers loadCategories', () => {
            expect(store.categories()).toEqual(MOCK_CATEGORIES);
            expect(store.selectedCategories()).toEqual([]);
            expect(store.breadcrumbs()).toEqual([]);
            expect(store.parentInode()).toBeNull();
            expect(store.totalRecords()).toBe(100);
            expect(store.page()).toBe(1);
            expect(store.rows()).toBe(25);
            expect(store.filter()).toBe('');
            expect(store.sortField()).toBe('category_name');
            expect(store.sortOrder()).toBe('ASC');
            expect(store.status()).toBe('loaded');
        });
    });

    describe('loadCategories', () => {
        it('should call getCategoriesPaginated when parentInode is null', () => {
            expect(categoriesService.getCategoriesPaginated).toHaveBeenCalledWith({
                filter: undefined,
                page: 1,
                per_page: 25,
                orderby: 'category_name',
                direction: 'ASC'
            });
        });

        it('should call getChildrenPaginated when parentInode is set', () => {
            categoriesService.getCategoriesPaginated.mockClear();
            categoriesService.getChildrenPaginated.mockClear();

            store.navigateToChildren(MOCK_CATEGORIES[0]);
            spectator.flushEffects();

            expect(categoriesService.getChildrenPaginated).toHaveBeenCalledWith(
                'inode-1',
                expect.objectContaining({
                    page: 1,
                    per_page: 25
                })
            );
        });

        it('should set categories and totalRecords on success', () => {
            expect(store.categories()).toEqual(MOCK_CATEGORIES);
            expect(store.totalRecords()).toBe(100);
            expect(store.status()).toBe('loaded');
        });

        it('should handle error and set status to error', () => {
            categoriesService.getCategoriesPaginated.mockReturnValue(
                throwError(() => new Error('fail'))
            );
            store.loadCategories();

            expect(store.status()).toBe('error');
            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
        });
    });

    describe('setFilter', () => {
        it('should update filter and reset page to 1', () => {
            store.setPagination(3, 25);
            store.setFilter('test-filter');

            expect(store.filter()).toBe('test-filter');
            expect(store.page()).toBe(1);
        });
    });

    describe('setPagination', () => {
        it('should update page and rows', () => {
            store.setPagination(5, 50);

            expect(store.page()).toBe(5);
            expect(store.rows()).toBe(50);
        });
    });

    describe('setSort', () => {
        it('should update sortField and sortOrder', () => {
            store.setSort('key', 'DESC');

            expect(store.sortField()).toBe('key');
            expect(store.sortOrder()).toBe('DESC');
        });
    });

    describe('setSelectedCategories', () => {
        it('should update selectedCategories', () => {
            store.setSelectedCategories(MOCK_CATEGORIES);

            expect(store.selectedCategories()).toEqual(MOCK_CATEGORIES);
        });
    });

    describe('navigateToChildren', () => {
        it('should push breadcrumb and set parentInode', () => {
            store.navigateToChildren(MOCK_CATEGORIES[0]);

            expect(store.breadcrumbs()).toEqual([{ label: 'Category 1', id: 'inode-1' }]);
            expect(store.parentInode()).toBe('inode-1');
            expect(store.page()).toBe(1);
            expect(store.filter()).toBe('');
            expect(store.selectedCategories()).toEqual([]);
        });

        it('should append to breadcrumbs on nested navigation', () => {
            store.navigateToChildren(MOCK_CATEGORIES[0]);
            store.navigateToChildren(MOCK_CATEGORIES[1]);

            expect(store.breadcrumbs()).toEqual([
                { label: 'Category 1', id: 'inode-1' },
                { label: 'Category 2', id: 'inode-2' }
            ]);
            expect(store.parentInode()).toBe('inode-2');
        });
    });

    describe('navigateToBreadcrumb', () => {
        beforeEach(() => {
            store.navigateToChildren(MOCK_CATEGORIES[0]);
            store.navigateToChildren(MOCK_CATEGORIES[1]);
        });

        it('should truncate breadcrumbs to given index', () => {
            store.navigateToBreadcrumb(0);

            expect(store.breadcrumbs()).toEqual([{ label: 'Category 1', id: 'inode-1' }]);
            expect(store.parentInode()).toBe('inode-1');
        });

        it('should navigate to root when index is -1', () => {
            store.navigateToBreadcrumb(-1);

            expect(store.breadcrumbs()).toEqual([]);
            expect(store.parentInode()).toBeNull();
        });
    });

    describe('createCategory', () => {
        it('should call categoriesService.createCategory and reload', () => {
            categoriesService.getCategoriesPaginated.mockClear();

            store.createCategory({ categoryName: 'New Category', key: 'new-cat' });

            expect(categoriesService.createCategory).toHaveBeenCalledWith({
                categoryName: 'New Category',
                key: 'new-cat'
            });
            expect(categoriesService.getCategoriesPaginated).toHaveBeenCalled();
        });

        it('should add parent inode when drilling into children', () => {
            store.navigateToChildren(MOCK_CATEGORIES[0]);
            spectator.flushEffects();

            store.createCategory({ categoryName: 'Child Category' });

            expect(categoriesService.createCategory).toHaveBeenCalledWith({
                categoryName: 'Child Category',
                parent: 'inode-1'
            });
        });

        it('should handle create error', () => {
            categoriesService.createCategory.mockReturnValue(
                throwError(() => new Error('create fail'))
            );

            store.createCategory({ categoryName: 'new' });

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
            expect(store.status()).toBe('loaded');
        });
    });

    describe('updateCategory', () => {
        it('should call categoriesService.updateCategory and reload', () => {
            categoriesService.getCategoriesPaginated.mockClear();

            store.updateCategory({
                inode: 'inode-1',
                categoryName: 'Updated',
                key: 'updated-key'
            });

            expect(categoriesService.updateCategory).toHaveBeenCalledWith({
                inode: 'inode-1',
                categoryName: 'Updated',
                key: 'updated-key'
            });
            expect(categoriesService.getCategoriesPaginated).toHaveBeenCalled();
        });

        it('should handle update error', () => {
            categoriesService.updateCategory.mockReturnValue(
                throwError(() => new Error('update fail'))
            );

            store.updateCategory({ inode: 'inode-1', categoryName: 'Updated' });

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
            expect(store.status()).toBe('loaded');
        });
    });

    describe('deleteCategories', () => {
        it('should call categoriesService.deleteCategories with selected inodes, clear selection, and reload', () => {
            categoriesService.getCategoriesPaginated.mockClear();
            store.setSelectedCategories(MOCK_CATEGORIES);

            store.deleteCategories();

            expect(categoriesService.deleteCategories).toHaveBeenCalledWith(['inode-1', 'inode-2']);
            expect(store.selectedCategories()).toEqual([]);
            expect(categoriesService.getCategoriesPaginated).toHaveBeenCalled();
        });

        it('should handle delete error', () => {
            categoriesService.deleteCategories.mockReturnValue(
                throwError(() => new Error('delete fail'))
            );
            store.setSelectedCategories(MOCK_CATEGORIES);

            store.deleteCategories();

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
            expect(store.status()).toBe('loaded');
        });
    });

    describe('exportCategories', () => {
        it('should call categoriesService.exportCategories with parentInode', () => {
            store.exportCategories();

            expect(categoriesService.exportCategories).toHaveBeenCalledWith(null);
        });

        it('should call exportCategories with parentInode when navigated', () => {
            store.navigateToChildren(MOCK_CATEGORIES[0]);
            spectator.flushEffects();
            categoriesService.exportCategories.mockClear();

            store.exportCategories();

            expect(categoriesService.exportCategories).toHaveBeenCalledWith('inode-1');
        });

        it('should handle export error', () => {
            categoriesService.exportCategories.mockReturnValue(
                throwError(() => new Error('export fail'))
            );

            store.exportCategories();

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
            expect(store.status()).toBe('loaded');
        });
    });

    describe('importCategories', () => {
        it('should call categoriesService.importCategories and reload categories on success', () => {
            categoriesService.getCategoriesPaginated.mockClear();
            const file = new File(['data'], 'test.csv', { type: 'text/csv' });

            store.importCategories(file, 'merge');

            expect(categoriesService.importCategories).toHaveBeenCalledWith(file, 'merge', null);
            expect(categoriesService.getCategoriesPaginated).toHaveBeenCalled();
        });

        it('should handle import error', () => {
            categoriesService.importCategories.mockReturnValue(
                throwError(() => new Error('import fail'))
            );
            const file = new File(['data'], 'test.csv', { type: 'text/csv' });

            store.importCategories(file, 'replace');

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
            expect(store.status()).toBe('loaded');
        });
    });

    describe('Effect (auto-reload)', () => {
        it('should trigger loadCategories when filter changes', () => {
            categoriesService.getCategoriesPaginated.mockClear();
            store.setFilter('new-filter');
            spectator.flushEffects();

            expect(categoriesService.getCategoriesPaginated).toHaveBeenCalledWith(
                expect.objectContaining({ filter: 'new-filter' })
            );
        });

        it('should trigger loadCategories when pagination changes', () => {
            categoriesService.getCategoriesPaginated.mockClear();
            store.setPagination(2, 50);
            spectator.flushEffects();

            expect(categoriesService.getCategoriesPaginated).toHaveBeenCalledWith(
                expect.objectContaining({ page: 2, per_page: 50 })
            );
        });

        it('should trigger loadCategories when parentInode changes', () => {
            categoriesService.getChildrenPaginated.mockClear();
            store.navigateToChildren(MOCK_CATEGORIES[0]);
            spectator.flushEffects();

            expect(categoriesService.getChildrenPaginated).toHaveBeenCalled();
        });
    });
});
