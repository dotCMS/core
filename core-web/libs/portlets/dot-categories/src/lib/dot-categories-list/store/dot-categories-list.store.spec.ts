import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { BehaviorSubject, of, throwError } from 'rxjs';

import { ActivatedRoute, Router } from '@angular/router';

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
    let router: jest.Mocked<Router>;
    let queryParams$: BehaviorSubject<Record<string, string>>;

    const createService = createServiceFactory({
        service: DotCategoriesListStore,
        providers: [
            mockProvider(DotCategoriesService, {
                getCategoriesPaginated: jest
                    .fn()
                    .mockReturnValue(of(MOCK_PAGINATED_RESPONSE)),
                getChildrenPaginated: jest
                    .fn()
                    .mockReturnValue(of(MOCK_PAGINATED_RESPONSE)),
                createCategory: jest
                    .fn()
                    .mockReturnValue(of({ entity: MOCK_CATEGORIES[0] })),
                updateCategory: jest
                    .fn()
                    .mockReturnValue(of({ entity: MOCK_CATEGORIES[0] })),
                deleteCategories: jest
                    .fn()
                    .mockReturnValue(of({ entity: { successCount: 2, fails: [] } }))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(Router, {
                navigate: jest.fn().mockReturnValue(Promise.resolve(true))
            }),
            {
                provide: ActivatedRoute,
                useFactory: () => {
                    queryParams$ = new BehaviorSubject<Record<string, string>>({});

                    return { queryParams: queryParams$.asObservable() };
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        categoriesService = spectator.inject(
            DotCategoriesService
        ) as jest.Mocked<DotCategoriesService>;
        router = spectator.inject(Router) as jest.Mocked<Router>;
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

        it('should call getChildrenPaginated when inode query param is set', () => {
            categoriesService.getCategoriesPaginated.mockClear();
            categoriesService.getChildrenPaginated.mockClear();

            const mockWithParentList = {
                entity: [
                    {
                        ...MOCK_CATEGORIES[0],
                        parentList: [{ name: 'Category 1', key: 'cat1', inode: 'inode-1' }]
                    }
                ],
                pagination: { currentPage: 1, perPage: 25, totalEntries: 10 }
            };
            categoriesService.getChildrenPaginated.mockReturnValue(of(mockWithParentList));

            queryParams$.next({ inode: 'inode-1' });
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

        it('should rebuild breadcrumbs from parentList when navigated to children', () => {
            const mockWithParentList = {
                entity: [
                    {
                        ...MOCK_CATEGORIES[0],
                        parentList: [
                            { name: 'Root Cat', key: 'root', inode: 'root-inode' },
                            { name: 'Child Cat', key: 'child', inode: 'child-inode' }
                        ]
                    }
                ],
                pagination: { currentPage: 1, perPage: 25, totalEntries: 5 }
            };
            categoriesService.getChildrenPaginated.mockReturnValue(of(mockWithParentList));

            queryParams$.next({ inode: 'child-inode' });
            spectator.flushEffects();

            expect(store.breadcrumbs()).toEqual([
                { label: 'Root Cat', id: 'root-inode' },
                { label: 'Child Cat', id: 'child-inode' }
            ]);
        });

        it('should set empty breadcrumbs at root level', () => {
            expect(store.breadcrumbs()).toEqual([]);
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
        it('should update URL with inode query param', () => {
            store.navigateToChildren(MOCK_CATEGORIES[0]);

            expect(router.navigate).toHaveBeenCalledWith([], {
                relativeTo: expect.anything(),
                queryParams: { inode: 'inode-1' },
                queryParamsHandling: 'merge'
            });
        });
    });

    describe('navigateToBreadcrumb', () => {
        beforeEach(() => {
            const mockWithParentList = {
                entity: [
                    {
                        ...MOCK_CATEGORIES[0],
                        parentList: [
                            { name: 'Category 1', key: 'cat1', inode: 'inode-1' },
                            { name: 'Category 2', key: 'cat2', inode: 'inode-2' }
                        ]
                    }
                ],
                pagination: { currentPage: 1, perPage: 25, totalEntries: 10 }
            };
            categoriesService.getChildrenPaginated.mockReturnValue(of(mockWithParentList));

            queryParams$.next({ inode: 'inode-2' });
            spectator.flushEffects();
            (router.navigate as jest.Mock).mockClear();
        });

        it('should navigate to breadcrumb inode at given index', () => {
            store.navigateToBreadcrumb(0);

            expect(router.navigate).toHaveBeenCalledWith([], {
                relativeTo: expect.anything(),
                queryParams: { inode: 'inode-1' },
                queryParamsHandling: 'merge'
            });
        });

        it('should navigate to root when index is -1', () => {
            store.navigateToBreadcrumb(-1);

            expect(router.navigate).toHaveBeenCalledWith([], {
                relativeTo: expect.anything(),
                queryParams: { inode: null },
                queryParamsHandling: 'merge'
            });
        });
    });

    describe('Query params sync', () => {
        it('should set parentInode when inode query param changes', () => {
            queryParams$.next({ inode: 'some-inode' });

            expect(store.parentInode()).toBe('some-inode');
            expect(store.page()).toBe(1);
            expect(store.filter()).toBe('');
            expect(store.selectedCategories()).toEqual([]);
        });

        it('should set parentInode to null when inode query param is removed', () => {
            queryParams$.next({ inode: 'some-inode' });
            queryParams$.next({});

            expect(store.parentInode()).toBeNull();
        });

        it('should not patch state if inode has not changed', () => {
            queryParams$.next({});
            const currentPage = store.page();

            // Emit same empty params again
            store.setPagination(3, 25);
            queryParams$.next({});

            // page should be reset to 1 because the subscription fires
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
            const mockWithParentList = {
                entity: [
                    {
                        ...MOCK_CATEGORIES[0],
                        parentList: [{ name: 'Category 1', key: 'cat1', inode: 'inode-1' }]
                    }
                ],
                pagination: { currentPage: 1, perPage: 25, totalEntries: 10 }
            };
            categoriesService.getChildrenPaginated.mockReturnValue(of(mockWithParentList));

            queryParams$.next({ inode: 'inode-1' });
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

            expect(categoriesService.deleteCategories).toHaveBeenCalledWith([
                'inode-1',
                'inode-2'
            ]);
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

        it('should trigger loadCategories when parentInode changes via query params', () => {
            categoriesService.getChildrenPaginated.mockClear();
            queryParams$.next({ inode: 'inode-1' });
            spectator.flushEffects();

            expect(categoriesService.getChildrenPaginated).toHaveBeenCalled();
        });
    });
});
