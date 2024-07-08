import { tapResponse } from '@ngrx/component-store';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { delay, filter, switchMap, tap } from 'rxjs/operators';

import {
    ComponentStatus,
    DotCategory,
    DotCMSContentlet,
    DotCMSContentTypeField
} from '@dotcms/dotcms-models';

import { CategoryFieldViewMode } from '../components/dot-category-field-sidebar/dot-category-field-sidebar.component';
import {
    DotCategoryFieldCategory,
    DotCategoryFieldCategorySearchedItems,
    DotCategoryFieldItem,
    DotCategoryFieldKeyValueObj
} from '../models/dot-category-field.models';
import { CategoriesService } from '../services/categories.service';
import {
    addMetadata,
    checkIfClickedIsLastItem,
    clearCategoriesAfterIndex,
    clearParentPathAfterIndex,
    getSelectedCategories,
    transformedData,
    updateChecked,
    updateSelectedFromSearch
} from '../utils/category-field.utils';

export type CategoryFieldState = {
    field: DotCMSContentTypeField;
    selected: DotCategoryFieldKeyValueObj[];
    categories: DotCategoryFieldCategory[][];
    parentPath: string[];
    state: ComponentStatus;
    // search
    mode: CategoryFieldViewMode;
    filter: string;
    searchCategories: DotCategoryFieldCategory[];
};

export const initialState: CategoryFieldState = {
    field: {} as DotCMSContentTypeField,
    selected: [],
    categories: [],
    parentPath: [],
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
            store.categories().map((column) => addMetadata(column, store.parentPath()))
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
        getSearchedCategories: computed(() => transformedData(store.searchCategories())), // remove this one

        isSearchLoading: computed(
            () => store.mode() === 'search' && store.state() === ComponentStatus.LOADING
        ),
        searchCategoriesFound: computed(
            () =>
                store.mode() === 'search' &&
                store.state() === ComponentStatus.LOADED &&
                store.filter()
        )
    })),
    withMethods((store, categoryService = inject(CategoriesService)) => ({
        /**
         * Sets a given iNode as the main parent and loads selected categories into the store.
         */
        load(field: DotCMSContentTypeField, contentlet: DotCMSContentlet): void {
            const selected = getSelectedCategories(field, contentlet);
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
        updateSelected(selected: string[], item: DotCategory): void {
            const currentChecked: DotCategoryFieldKeyValueObj[] = updateChecked(
                store.selected(),
                selected,
                item
            );

            patchState(store, {
                selected: currentChecked
            });
        },

        updateSelectedFromSearch(selectedItems: DotCategoryFieldCategorySearchedItems[]): void {
            const currentChecked: DotCategoryFieldKeyValueObj[] = updateSelectedFromSearch(
                store.selected(),
                selectedItems,
                store.searchCategories()
            );

            patchState(store, {
                selected: [...currentChecked]
            });
        },

        /**
         * Clears all categories from the store, effectively resetting state related to categories and their parent paths.
         */
        clean() {
            patchState(store, { categories: [], parentPath: [], mode: 'list', filter: '' });
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
                                parentPath: [
                                    ...clearParentPathAfterIndex(store.parentPath(), index)
                                ]
                            });
                        }
                    }
                }),
                // Only pass if you click a item with children
                filter(
                    (event) =>
                        !event ||
                        (event.item.childrenCount > 0 &&
                            !store.parentPath().includes(event.item.inode))
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
                                        parentPath: [...store.parentPath(), event.item.inode]
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
                        delay(300),
                        tapResponse({
                            next: (categories) => {
                                patchState(store, {
                                    searchCategories: categories,
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
