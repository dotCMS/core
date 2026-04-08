import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { EMPTY, Observable } from 'rxjs';

import { effect, inject, untracked } from '@angular/core';

import { MenuItem, MessageService } from 'primeng/api';

import { catchError, take } from 'rxjs/operators';

import {
    DotCategoriesService,
    DotCategoryForm,
    DotCategoryUpdateForm,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotCategory } from '@dotcms/dotcms-models';

type DotCategoriesListStatus = 'init' | 'loading' | 'loaded' | 'error';

/**
 * Special identifier used by the push-publishing system to bundle ALL categories at once.
 * This constant mirrors the legacy JSP behavior in `view_categories.jsp`, which called
 * `pushHandler.showAddToBundleDialog('CAT', ...)`. The backend's `RemotePublishAjaxAction`
 * recognizes 'CAT' as a signal to include every category in the generated bundle, handled
 * by `CategoryBundler` / `CategoryFullBundler` on the enterprise publishing pipeline.
 */
export const ALL_CATEGORIES_BUNDLE_IDENTIFIER = 'CAT';

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
    sortOrder: 'ASC' | 'DESC';
    status: DotCategoriesListStatus;
    showAddToBundle: boolean;
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
    status: 'init',
    showAddToBundle: false
};

export const DotCategoriesListStore = signalStore(
    withState<DotCategoriesListState>(initialState),
    withMethods((store) => {
        const categoriesService = inject(DotCategoriesService);
        const httpErrorManager = inject(DotHttpErrorManagerService);
        const messageService = inject(MessageService);
        const dotMessageService = inject(DotMessageService);

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

        function handleCategoryAction<T>(source$: Observable<T>, onSuccess: () => void) {
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

            setSort(field: string, order: 'ASC' | 'DESC') {
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
                    const parentInode = (breadcrumbs[breadcrumbs.length - 1]?.id ?? null) as
                        | string
                        | null;
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

                handleCategoryAction(categoriesService.createCategory(body), () => {
                    messageService.add({
                        severity: 'success',
                        summary: dotMessageService.get('categories.create.success')
                    });
                    loadCategories();
                });
            },

            updateCategory(form: DotCategoryUpdateForm) {
                handleCategoryAction(categoriesService.updateCategory(form), () => {
                    messageService.add({
                        severity: 'success',
                        summary: dotMessageService.get('categories.update.success')
                    });
                    loadCategories();
                });
            },

            deleteCategories() {
                const inodes = store.selectedCategories().map((c) => c.inode);
                handleCategoryAction(categoriesService.deleteCategories(inodes), () => {
                    messageService.add({
                        severity: 'success',
                        summary: dotMessageService.get('categories.delete.success')
                    });
                    patchState(store, { selectedCategories: [] });
                    loadCategories();
                });
            },

            exportCategories() {
                // Export is a background file download — it must NOT set status: 'loading'
                // (which would show the table spinner). We handle errors directly without
                // going through handleCategoryAction to avoid that side effect.
                categoriesService
                    .exportCategories(store.parentInode())
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { status: 'loaded' });

                            return EMPTY;
                        })
                    )
                    .subscribe();
            },

            importCategories(file: File, exportType: 'replace' | 'merge') {
                handleCategoryAction(
                    categoriesService.importCategories(file, exportType, store.parentInode()),
                    () => loadCategories()
                );
            },

            openAddToBundle() {
                patchState(store, { showAddToBundle: true });
            },

            closeAddToBundle() {
                patchState(store, { showAddToBundle: false });
            },

            updateSortOrder(inode: string, sortOrder: number) {
                const previousCategories = store.categories();

                patchState(store, {
                    categories: previousCategories.map((c) =>
                        c.inode === inode ? { ...c, sortOrder } : c
                    )
                });

                categoriesService
                    .updateSortOrder(
                        { [inode]: sortOrder },
                        {
                            parentInode: store.parentInode(),
                            filter: store.filter() || undefined,
                            page: store.page(),
                            per_page: store.rows(),
                            orderby: store.sortField(),
                            direction: store.sortOrder()
                        }
                    )
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { categories: previousCategories });

                            return EMPTY;
                        })
                    )
                    .subscribe(() => {
                        messageService.add({
                            severity: 'success',
                            summary: dotMessageService.get('categories.sort-order.saved')
                        });
                    });
            }
        };
    }),
    withHooks((store) => {
        return {
            onInit() {
                // All six signal reads below are intentional tracking dependencies.
                // Any change to filter, pagination, sort, or navigation triggers a reload.
                // loadCategories() is wrapped in untracked() to prevent the signals it
                // reads internally from registering as additional dependencies.
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
