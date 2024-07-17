import { tapResponse } from '@ngrx/component-store';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import { filter, switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus, DotCategory, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import {
    CategoryFieldViewMode,
    DotCategoryField,
    DotCategoryFieldItem,
    DotCategoryFieldKeyValueObj
} from '../models/dot-category-field.models';
import { CategoriesService } from '../services/categories.service';
import {
    addSelected,
    checkIfClickedIsLastItem,
    clearCategoriesAfterIndex,
    clearParentPathAfterIndex,
    getSelectedFromContentlet,
    removeItemByKey,
    transformCategories,
    transformToSelectedObject,
    updateChecked
} from '../utils/category-field.utils';

export type CategoryFieldState = {
    field: DotCMSContentTypeField;
    selected: DotCategoryFieldKeyValueObj[]; // <- source of selected
    categories: DotCategory[][];
    keyParentPath: string[]; // Delete when we have the endpoint for this
    state: ComponentStatus;
    mode: CategoryFieldViewMode;
    // search
    filter: string;
    searchCategories: DotCategory[];
};

export const initialState: CategoryFieldState = {
    field: {} as DotCMSContentTypeField,
    selected: [],
    categories: [],
    keyParentPath: [],
    state: ComponentStatus.IDLE,
    mode: 'list',
    filter: '',
    searchCategories: []
};

/**
 * A Signal store responsible for managing category fields. It keeps track of the state of
 * different categories, provides access to selected categories, and offers methods for
 * loading, retrieving, and cleaning up categories.
 */
export const CategoryFieldStore = signalStore(
    withState(initialState),
    withComputed((store) => ({
        /**
         * Current selected items (key) from the contentlet
         */
        selectedCategoriesValues: computed(() => store.selected().map((item) => item.key)),

        /**
         * Categories for render with added properties
         */
        categoryList: computed(() =>
            store.categories().map((column) => transformCategories(column, store.keyParentPath()))
        ),

        /**
         * Indicates whether any categories are selected.
         */
        hasSelectedCategories: computed(() => !!store.selected().length),

        /**
         * Get the root category inode.
         */
        rootCategoryInode: computed(() => store.field().values),

        /**
         * Retrieves the value of the field variable.
         */
        fieldVariableName: computed(() => store.field().variable),

        // Search
        isSearchLoading: computed(
            () => store.mode() === 'search' && store.state() === ComponentStatus.LOADING
        ),

        /**
         * Categories for render with added properties
         */
        searchCategoryList: computed(() =>
            store
                .searchCategories()
                .map((column) => transformCategories(column, store.keyParentPath()))
        )
    })),
    withMethods(
        (
            store,
            categoryService = inject(CategoriesService),
            dotHttpErrorManagerService = inject(DotHttpErrorManagerService)
        ) => ({
            /**
             * The method gets `DotCategoryField` as an input, then it performs the following operations:
             * - Initially sets the state in the store to LOADING.
             * - Transforms and collects selected categories from given `DotCategoryField`.
             * - Constructs parents hierarchy for selected categories using service, and loads it into the store.
             */
            load: rxMethod<DotCategoryField>(
                pipe(
                    tap(() => patchState(store, { state: ComponentStatus.LOADING })),
                    switchMap((categoryField) => {
                        const { field, contentlet } = categoryField;
                        const selectedKeys = getSelectedFromContentlet(field, contentlet).map(
                            (item) => item.key
                        );

                        return categoryService.getSelectedHierarchy(selectedKeys).pipe(
                            tapResponse({
                                next: (categoryWithParentPath) => {
                                    const selected =
                                        transformToSelectedObject(categoryWithParentPath);

                                    patchState(store, {
                                        field,
                                        selected,
                                        state: ComponentStatus.IDLE
                                    });
                                },
                                error: (error: HttpErrorResponse) => {
                                    patchState(store, {
                                        field,
                                        state: ComponentStatus.IDLE
                                    });

                                    dotHttpErrorManagerService.handle(error);
                                }
                            })
                        );
                    })
                )
            ),

            setMode(mode: CategoryFieldViewMode): void {
                patchState(store, {
                    mode,
                    searchCategories: [],
                    filter: ''
                });
            },

            /**
             * Updates the selected items based on the items keyed by the provided selected keys.
             * This method receives the selected keys from the list of categories, searches in the category array
             * for the item with all the necessary data and stores it in the state as a selected item.
             *
             * @param {string[]} categoryListChecked - An array of selected keys from the category list
             * @param {DotCategoryFieldKeyValueObj} item - The item containing the data to be stored as a selected item
             */
            updateSelected(categoryListChecked: string[], item: DotCategoryFieldKeyValueObj): void {
                const currentChecked: DotCategoryFieldKeyValueObj[] = updateChecked(
                    store.selected(),
                    categoryListChecked,
                    item
                );

                patchState(store, {
                    selected: currentChecked
                });
            },

            /**
             * Adds the items directly to the 'selected' in the store. These items are already transformed and formatted in the manner 'selected' requires.
             *
             * @param {DotCategoryFieldKeyValueObj | DotCategoryFieldKeyValueObj[]} selectedItem - The fully formed item or items to be added. These item(s) have been transformed from the search results to match the requirements of 'selected'.
             * @returns {void}
             */
            addSelected(
                selectedItem: DotCategoryFieldKeyValueObj | DotCategoryFieldKeyValueObj[]
            ): void {
                const updatedSelected = addSelected(store.selected(), selectedItem);
                patchState(store, {
                    selected: updatedSelected
                });
            },

            /**
             * Removes the selected items with the given key(s).
             *
             * @param {string | string[]} key - The key(s) of the item(s) to be removed.
             * @return {void}
             */
            removeSelected(key: string | string[]): void {
                const newSelected = removeItemByKey(store.selected(), key);

                patchState(store, {
                    selected: newSelected
                });
            },

            /**
             * Clears all categories from the store, effectively resetting state related to categories and their parent paths.
             */
            clean() {
                patchState(store, {
                    categories: [],
                    keyParentPath: [],
                    mode: 'list',
                    filter: '',
                    searchCategories: []
                });
            },

            /**
             * Fetches categories from a given iNode category parent.
             * This method accepts either void to get the parent, or an index and item returned after clicking an item with children.
             */
            getCategories: rxMethod<void | DotCategoryFieldItem>(
                pipe(
                    tap((event) => {
                        const index = event ? event.index : 0;
                        const currentCategories = store.categories();

                        if (event) {
                            if (!checkIfClickedIsLastItem(index, currentCategories)) {
                                patchState(store, {
                                    categories: [
                                        ...clearCategoriesAfterIndex(currentCategories, index)
                                    ],
                                    keyParentPath: [
                                        ...clearParentPathAfterIndex(store.keyParentPath(), index)
                                    ]
                                });
                            }
                        }
                    }),
                    // Only pass if you click a item with children
                    filter(
                        (event) =>
                            !event ||
                            (event.item.hasChildren &&
                                !store.keyParentPath().includes(event.item.key))
                    ),
                    tap(() => patchState(store, { state: ComponentStatus.LOADING })),
                    switchMap((event) => {
                        const categoryInode: string = event
                            ? event.item.inode
                            : store.rootCategoryInode();

                        return categoryService.getChildren(categoryInode).pipe(
                            tapResponse({
                                next: (newCategories) => {
                                    if (event) {
                                        patchState(store, {
                                            categories: [...store.categories(), newCategories],
                                            state: ComponentStatus.LOADED,
                                            keyParentPath: [
                                                ...store.keyParentPath(),
                                                event.item.key
                                            ]
                                        });
                                    } else {
                                        patchState(store, {
                                            categories: [...store.categories(), newCategories],
                                            state: ComponentStatus.LOADED
                                        });
                                    }
                                },
                                error: () => {
                                    // TODO: Add Error Handler
                                    patchState(store, { state: ComponentStatus.IDLE });
                                }
                            })
                        );
                    })
                )
            ),

            /**
             * Searches for children categories based on the specified filter.
             *
             * @param {string} filter - The filter to apply when searching for children categories.
             */
            search: rxMethod<string>(
                pipe(
                    tap(() =>
                        patchState(store, { mode: 'search', state: ComponentStatus.LOADING })
                    ),
                    switchMap((filter) => {
                        return categoryService
                            .getChildren(store.rootCategoryInode(), { filter })
                            .pipe(
                                tapResponse({
                                    next: (categories) => {
                                        patchState(store, {
                                            searchCategories: [...categories],
                                            state: ComponentStatus.LOADED
                                        });
                                    },
                                    error: () => {
                                        // TODO: Add Error Handler
                                        patchState(store, { state: ComponentStatus.IDLE });
                                    }
                                })
                            );
                    })
                )
            )
        })
    )
);
