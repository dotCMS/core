import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { EMPTY, Observable } from 'rxjs';

import { effect, inject, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import { MenuItem } from 'primeng/api';

import { catchError, take } from 'rxjs/operators';

import {
    DotCategoriesService,
    DotCategoryForm,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import { DotCategory } from '@dotcms/dotcms-models';

type DotCategoriesListStatus = 'init' | 'loading' | 'loaded' | 'error';

interface DotCategoriesListState {
    categories: DotCategory[];
    selectedCategories: DotCategory[];
    breadcrumbs: MenuItem[];
    parentInode: string | null;
    parentName: string | null;
    totalRecords: number;
    page: number;
    rows: number;
    filter: string;
    sortField: string;
    sortOrder: string;
    status: DotCategoriesListStatus;
}

const initialState: DotCategoriesListState = {
    categories: [],
    selectedCategories: [],
    breadcrumbs: [],
    parentInode: null,
    parentName: null,
    totalRecords: 0,
    page: 1,
    rows: 25,
    filter: '',
    sortField: 'category_name',
    sortOrder: 'ASC',
    status: 'init'
};

export const DotCategoriesListStore = signalStore(
    withState<DotCategoriesListState>(initialState),
    withMethods((store) => {
        const categoriesService = inject(DotCategoriesService);
        const httpErrorManager = inject(DotHttpErrorManagerService);
        const router = inject(Router);
        const route = inject(ActivatedRoute);

        function loadCategories() {
            patchState(store, { status: 'loading' });

            const params = {
                filter: store.filter() || undefined,
                page: store.page(),
                per_page: store.rows(),
                orderby: store.sortField(),
                direction: store.sortOrder()
            };

            const source$ =
                store.parentInode() === null
                    ? categoriesService.getCategoriesPaginated(params)
                    : categoriesService.getChildrenPaginated(store.parentInode()!, params);

            source$
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'error' });

                        return EMPTY;
                    })
                )
                .subscribe((response) => {
                    const categories = response.entity;
                    const firstCategory = categories[0];
                    const parentList = firstCategory?.parentList ?? [];

                    let breadcrumbs: MenuItem[];

                    if (!store.parentInode()) {
                        // Root level — no breadcrumbs
                        breadcrumbs = [];
                    } else if (parentList.length) {
                        // parentList contains ancestors of the children being listed.
                        // It includes the current parent we navigated into.
                        // We need the full trail: all items in parentList that are NOT
                        // the top-level root, plus the current parent from query params.
                        // Actually, parentList already represents the path from
                        // top-level down to the current parent. We just need to use it
                        // and append the current parent if it's not already the last item.
                        const fromApi = parentList.map((p) => ({
                            label: p.name,
                            id: p.inode
                        }));
                        const currentInode = store.parentInode();
                        const lastInApi = fromApi[fromApi.length - 1];

                        if (lastInApi && lastInApi.id === currentInode) {
                            // parentList already ends with the current parent
                            breadcrumbs = fromApi;
                        } else {
                            // Append current parent to complete the trail
                            breadcrumbs = [
                                ...fromApi,
                                { label: store.parentName() ?? '', id: currentInode }
                            ];
                        }
                    } else {
                        // No parentList (empty child list) — keep existing breadcrumbs
                        breadcrumbs = store.breadcrumbs();
                    }

                    const parentName = breadcrumbs.length
                        ? (breadcrumbs[breadcrumbs.length - 1].label as string)
                        : store.parentName();

                    patchState(store, {
                        categories,
                        breadcrumbs,
                        parentName,
                        totalRecords: response.pagination?.totalEntries ?? 0,
                        status: 'loaded'
                    });
                });
        }

        function handleCategoryAction(source$: Observable<unknown>, onSuccess: () => void) {
            patchState(store, { status: 'loading' });
            source$
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'loaded' });

                        return EMPTY;
                    })
                )
                .subscribe(() => onSuccess());
        }

        return {
            loadCategories,

            setFilter(filter: string) {
                patchState(store, { filter, page: 1 });
            },

            setPagination(page: number, rows: number) {
                patchState(store, { page, rows });
            },

            setSort(field: string, order: string) {
                patchState(store, { sortField: field, sortOrder: order });
            },

            setSelectedCategories(categories: DotCategory[]) {
                patchState(store, { selectedCategories: categories });
            },

            navigateToChildren(category: DotCategory) {
                router.navigate([], {
                    relativeTo: route,
                    queryParams: { inode: category.inode, name: category.categoryName },
                    queryParamsHandling: 'merge'
                });
            },

            navigateToBreadcrumb(index: number) {
                if (index < 0) {
                    router.navigate([], {
                        relativeTo: route,
                        queryParams: { inode: null, name: null },
                        queryParamsHandling: 'merge'
                    });
                } else {
                    const breadcrumb = store.breadcrumbs()[index];
                    router.navigate([], {
                        relativeTo: route,
                        queryParams: { inode: breadcrumb?.id, name: breadcrumb?.label },
                        queryParamsHandling: 'merge'
                    });
                }
            },

            createCategory(form: DotCategoryForm) {
                const body: DotCategoryForm = { ...form };
                if (store.parentInode()) {
                    body.parent = store.parentInode()!;
                }

                handleCategoryAction(categoriesService.createCategory(body), () =>
                    loadCategories()
                );
            },

            updateCategory(form: DotCategoryForm) {
                handleCategoryAction(categoriesService.updateCategory(form), () =>
                    loadCategories()
                );
            },

            deleteCategories() {
                const inodes = store.selectedCategories().map((c) => c.inode);
                handleCategoryAction(categoriesService.deleteCategories(inodes), () => {
                    patchState(store, { selectedCategories: [] });
                    loadCategories();
                });
            }
        };
    }),
    withHooks((store) => {
        return {
            onInit() {
                const route = inject(ActivatedRoute);

                // Subscribe to query params to sync inode and parent name
                route.queryParams.pipe(takeUntilDestroyed()).subscribe((params) => {
                    const inode = params['inode'] || null;
                    const name = params['name'] || null;
                    if (inode !== store.parentInode()) {
                        patchState(store, {
                            parentInode: inode,
                            parentName: name,
                            page: 1,
                            filter: '',
                            selectedCategories: []
                        });
                    }
                });

                // Auto-reload when state changes
                effect(() => {
                    store.filter();
                    store.page();
                    store.rows();
                    store.sortField();
                    store.sortOrder();
                    store.parentInode();

                    untracked(() => store.loadCategories());
                });
            }
        };
    })
);
