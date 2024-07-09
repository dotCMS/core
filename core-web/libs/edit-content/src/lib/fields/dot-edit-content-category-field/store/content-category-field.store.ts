import { tapResponse } from '@ngrx/component-store';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { filter, switchMap, tap } from 'rxjs/operators';

import {
    ComponentStatus,
    DotCategory,
    DotCMSContentlet,
    DotCMSContentTypeField
} from '@dotcms/dotcms-models';

import { CategoryFieldViewMode } from '../components/dot-category-field-sidebar/dot-category-field-sidebar.component';
import {
    DotCategoryFieldItem,
    DotCategoryFieldKeyValueObj
} from '../models/dot-category-field.models';
import { CategoriesService } from '../services/categories.service';
import {
    addSelected,
    checkIfClickedIsLastItem,
    clearCategoriesAfterIndex,
    clearParentPathAfterIndex,
    removeItemByKey,
    transformCategories,
    transformSelectedCategories,
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
    withMethods((store, categoryService = inject(CategoriesService)) => ({
        /**
         * Sets a given iNode as the main parent and loads selected categories into the store.
         */
        load(field: DotCMSContentTypeField, contentlet: DotCMSContentlet): void {
            const selected = transformSelectedCategories(field, contentlet);
            patchState(store, {
                field,
                selected
            });
        },

        setMode(mode: 'list' | 'search'): void {
            patchState(store, {
                mode,
                searchCategories: [],
                filter: ''
            });
        },

        /**
         * Updates the selected items based on the provided item.
         */
        updateSelected(selected: string[], item: DotCategoryFieldKeyValueObj): void {
            const currentChecked: DotCategoryFieldKeyValueObj[] = updateChecked(
                store.selected(),
                selected,
                item
            );

            patchState(store, {
                selected: currentChecked
            });
        },

        /**
         * Adds the selected item(s) to the store's selected state.
         *
         * @param {DotCategoryFieldKeyValueObj | DotCategoryFieldKeyValueObj[]} selectedItem - The item(s) to be added.
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
            patchState(store, { categories: [], keyParentPath: [], mode: 'list', filter: '' });
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
                        (event.item.hasChildren && !store.keyParentPath().includes(event.item.key))
                ),
                tap(() => patchState(store, { state: ComponentStatus.LOADING })),
                switchMap((event) => {
                    const rootCategoryInode: string = event
                        ? event.item.inode
                        : store.rootCategoryInode();

                    return categoryService.getChildren(rootCategoryInode).pipe(
                        tapResponse({
                            next: (newCategories) => {
                                if (event) {
                                    patchState(store, {
                                        categories: [...store.categories(), newCategories],
                                        state: ComponentStatus.LOADED,
                                        keyParentPath: [...store.keyParentPath(), event.item.key]
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

        search: rxMethod<string>(
            pipe(
                tap(() => patchState(store, { mode: 'search', state: ComponentStatus.LOADING })),
                switchMap((filter) => {
                    return categoryService.getChildren(store.rootCategoryInode(), { filter }).pipe(
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
    }))
);
