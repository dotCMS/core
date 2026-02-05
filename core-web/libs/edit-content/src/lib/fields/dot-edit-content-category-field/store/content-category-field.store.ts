import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

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
    checkIfClickedIsLoaded,
    clearCategoriesAfterIndex,
    clearParentPathAfterIndex,
    getMenuItemsFromKeyParentPath,
    getSelectedFromContentlet,
    removeEmptyArrays,
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
    //dialog
    dialog: {
        selected: DotCategoryFieldKeyValueObj[];
        state: 'open' | 'closed';
    };
};

export const initialState: CategoryFieldState = {
    field: {} as DotCMSContentTypeField,
    selected: [],
    categories: [],
    keyParentPath: [],
    state: ComponentStatus.INIT,
    mode: 'list',
    filter: '',
    searchCategories: [],
    dialog: {
        selected: [],
        state: 'closed'
    }
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
         * Categories for render with added properties
         */
        categoryList: computed<DotCategoryFieldKeyValueObj[][]>(() =>
            store.categories().map((column) => transformCategories(column, store.keyParentPath()))
        ),

        /**
         * Get the root category inode.
         */
        rootCategoryInode: computed(() => store.field().values),

        /**
         * Retrieves the value of the field variable.
         */
        fieldVariableName: computed(() => store.field().variable),

        /**
         * Status of the List Component
         */
        listState: computed(() => (store.mode() === 'list' ? store.state() : ComponentStatus.INIT)),

        /**
         * Determines if the list mode is currently loading.
         */
        isListLoading: computed(
            () => store.mode() === 'list' && store.state() === ComponentStatus.LOADING
        ),

        /**
         * Determines if the store state is currently loaded.
         */
        isInitSate: computed(() => store.state() === ComponentStatus.INIT),

        /**
         * Determines if the search mode is currently loading.
         */
        isSearchLoading: computed(
            () => store.mode() === 'search' && store.state() === ComponentStatus.LOADING
        ),

        /**
         * Status of the Search Component
         */
        searchState: computed(() =>
            store.mode() === 'search' ? store.state() : ComponentStatus.INIT
        ),

        /**
         * Categories for render with added properties
         */
        searchCategoryList: computed<DotCategoryFieldKeyValueObj[]>(() =>
            store
                .searchCategories()
                .map((column) => transformCategories(column, store.keyParentPath()))
        ),

        /**
         * Transform the selected categories to a breadcrumb menu
         */
        breadcrumbMenu: computed(() => {
            const categories = store.categories();
            const keyParentPath = store.keyParentPath();

            return getMenuItemsFromKeyParentPath(categories, keyParentPath);
        }),

        // Dialog
        /**
         * Computed property that checks if a dialog is open.
         */
        isDialogOpen: computed(() => store.dialog.state() === 'open'),

        /**
         * A computed property that retrieves the keys of selected dialog items.
         * This function accesses the store's dialog and maps each selected item's key.
         *
         * @returns {Array} An array of keys of selected dialog items.
         */
        dialogSelectedKeys: computed(() => store.dialog.selected().map((item) => item.key))
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
                                        state: ComponentStatus.LOADED
                                    });
                                },
                                error: (error: HttpErrorResponse) => {
                                    patchState(store, {
                                        field,
                                        state: ComponentStatus.ERROR
                                    });

                                    dotHttpErrorManagerService.handle(error);
                                }
                            })
                        );
                    })
                )
            ),

            /**
             * Sets the mode for the CategoryFieldView and resets search categories and filter.
             */
            setMode(mode: CategoryFieldViewMode): void {
                patchState(store, {
                    mode,
                    searchCategories: [],
                    filter: ''
                });
            },

            /**
             * Opens the dialog with the current selected items and sets the state to 'open'.
             */
            openDialog(): void {
                patchState(store, {
                    dialog: {
                        selected: [...store.selected()],
                        state: 'open'
                    }
                });
            },

            /**
             * Closes the dialog and resets the selected items.
             */
            closeDialog(): void {
                patchState(store, {
                    dialog: {
                        selected: [],
                        state: 'closed'
                    }
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
                    store.dialog.selected(),
                    categoryListChecked,
                    item
                );

                patchState(store, {
                    dialog: { ...store.dialog(), selected: [...currentChecked] }
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
                const updatedSelected = addSelected(store.dialog.selected(), selectedItem);
                patchState(store, {
                    dialog: { state: 'open', selected: updatedSelected }
                });
            },

            /**
             * Applies the selected items from the dialog to the store.
             */
            applyDialogSelection(): void {
                const { selected } = store.dialog();
                patchState(store, {
                    selected
                });
            },

            /**
             * Removes the selected at the dialog items with the given key(s).
             *
             * @param {string | string[]} key - The key(s) of the item(s) to be removed.
             * @return {void}
             */
            removeSelected(key: string | string[]): void {
                const selected = removeItemByKey(store.dialog.selected(), key);

                patchState(store, {
                    dialog: { ...store.dialog(), selected }
                });
            },

            /**
             * Removes the selected items with the given key(s).
             *
             * @param {string | string[]} key - The key(s) of the item(s) to be removed.
             * @return {void}
             */
            removeRootSelected(key: string | string[]): void {
                const selected = removeItemByKey(store.selected(), key);

                patchState(store, { selected });
            },

            /**
             * Sets the selected categories from an array of inodes.
             * This method is used by ControlValueAccessor to initialize the component.
             *
             * @param {string[]} inodes - Array of category inodes to set as selected
             */
            setSelectedFromInodes: rxMethod<string[]>(
                pipe(
                    tap(() => patchState(store, { state: ComponentStatus.LOADING })),
                    switchMap((inodes) => {
                        if (!inodes || inodes.length === 0) {
                            patchState(store, {
                                selected: [],
                                state: ComponentStatus.LOADED
                            });

                            return EMPTY;
                        }

                        return categoryService.getSelectedHierarchy(inodes).pipe(
                            tapResponse({
                                next: (categoryWithParentPath) => {
                                    const selected =
                                        transformToSelectedObject(categoryWithParentPath);
                                    patchState(store, {
                                        selected,
                                        state: ComponentStatus.LOADED
                                    });
                                },
                                error: (error: HttpErrorResponse) => {
                                    patchState(store, {
                                        state: ComponentStatus.ERROR
                                    });
                                    dotHttpErrorManagerService.handle(error);
                                }
                            })
                        );
                    })
                )
            ),

            /**
             * Resets the store to its initial state.
             */
            clean() {
                patchState(store, {
                    categories: [],
                    keyParentPath: [],
                    mode: 'list',
                    filter: '',
                    searchCategories: [],
                    state: ComponentStatus.INIT
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
                        const keyParentPath = store.keyParentPath();

                        if (event && event?.item) {
                            if (
                                !checkIfClickedIsLastItem(index, currentCategories) &&
                                checkIfClickedIsLoaded(event, keyParentPath)
                            ) {
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
                            (event?.item?.hasChildren &&
                                !store.keyParentPath().includes(event?.item?.key))
                    ),
                    tap(() => patchState(store, { state: ComponentStatus.LOADING })),
                    switchMap((event) => {
                        const categoryInode: string = event
                            ? event?.item.inode
                            : store.rootCategoryInode();

                        return categoryService.getChildren(categoryInode).pipe(
                            tapResponse({
                                next: (newCategories) => {
                                    const changes: Partial<CategoryFieldState> = {
                                        categories: removeEmptyArrays([
                                            ...store.categories(),
                                            newCategories
                                        ]),
                                        state: ComponentStatus.LOADED
                                    };
                                    if (event) {
                                        changes.keyParentPath = [
                                            ...store.keyParentPath(),
                                            event.item.key
                                        ];
                                    }

                                    patchState(store, changes);
                                },
                                error: () => {
                                    // TODO: Add Error Handler
                                    patchState(store, { state: ComponentStatus.ERROR });
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
                                        patchState(store, {
                                            state: ComponentStatus.ERROR,
                                            searchCategories: []
                                        });
                                    }
                                })
                            );
                    })
                )
            )
        })
    )
);
