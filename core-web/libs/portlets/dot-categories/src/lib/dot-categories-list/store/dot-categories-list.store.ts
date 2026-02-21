import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { EMPTY, Observable } from 'rxjs';

import { effect, inject, untracked } from '@angular/core';

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
                    patchState(store, {
                        categories: response.entity,
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
                const breadcrumbs = [
                    ...store.breadcrumbs(),
                    { label: category.categoryName, id: category.inode }
                ];
                patchState(store, {
                    breadcrumbs,
                    parentInode: category.inode,
                    page: 1,
                    filter: '',
                    selectedCategories: []
                });
            },

            navigateToBreadcrumb(index: number) {
                if (index < 0) {
                    patchState(store, {
                        breadcrumbs: [],
                        parentInode: null,
                        page: 1,
                        filter: '',
                        selectedCategories: []
                    });
                } else {
                    const breadcrumbs = store.breadcrumbs().slice(0, index + 1);
                    const parentInode = breadcrumbs[breadcrumbs.length - 1]?.id as string;
                    patchState(store, {
                        breadcrumbs,
                        parentInode,
                        page: 1,
                        filter: '',
                        selectedCategories: []
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
            },

            exportCategories() {
                handleCategoryAction(categoriesService.exportCategories(store.parentInode()), () =>
                    patchState(store, { status: 'loaded' })
                );
            },

            importCategories(file: File, exportType: 'replace' | 'merge') {
                handleCategoryAction(
                    categoriesService.importCategories(file, exportType, store.parentInode()),
                    () => loadCategories()
                );
            }
        };
    }),
    withHooks((store) => {
        return {
            onInit() {
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
