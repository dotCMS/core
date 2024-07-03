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

import {
    DotCategoryFieldCategory,
    DotCategoryFieldItem,
    DotCategoryFieldKeyValueObj
} from '../models/dot-category-field.models';
import { CategoriesService } from '../services/categories.service';
import {
    addMetadata,
    checkIfClickedIsLastItem,
    clearCategoriesAfterIndex,
    clearParentPathAfterIndex,
    getSelectedCategories
} from '../utils/category-field.utils';

export type CategoryFieldState = {
    field: DotCMSContentTypeField; // field and configurations
    selected: DotCategoryFieldKeyValueObj[]; // selected items from form and UI
    categories: DotCategoryFieldCategory[][]; // list of categories
    parentPath: string[]; // Parent patch of selected UI
    state: ComponentStatus;
};

export const initialState: CategoryFieldState = {
    field: {} as DotCMSContentTypeField,
    selected: [],
    categories: [],
    parentPath: [],
    state: ComponentStatus.IDLE
};

/**
 * A Signal store responsible for managing category fields. It keeps track of the state of
 * different categories, provides access to selected categories, and offers methods for
 * loading, retrieving, and cleaning up categories.
 *
 * @typedef {Object} CategoryFieldStore
 *
 *   @property {Array<string>} selectedCategories - The keys of the currently selected items from the contentlet.
 *   @property {Array<Object>} categoryList - The list of categories ready for rendering, where each category has additional properties.
 *   @property {function} load - Sets a given iNode as the parent and loads selected categories into the store.
 *   @property {function} getCategories - Fetches categories of a given iNode category parent.
 *   @property {function} clean - Clears all categories from the store.
 */
export const CategoryFieldStore = signalStore(
    withState(initialState),
    withComputed(({ field, categories, selected, parentPath }) => ({
        /**
         * Current selected items (key) from the contentlet
         */
        selectedCategoriesValues: computed(() => selected().map((item) => item.key)),
        /**
         * Categories for render with added properties
         */
        categoryList: computed(() =>
            categories().map((column) => addMetadata(column, parentPath()))
        ),

        hasSelectedCategories: computed(() => !!selected().map((item) => item.key).length),

        rootCategoryInode: computed(() => field().values),
        fieldVariableName: computed(() => field().variable)
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

        updateSelected(selected: string[], item: DotCategory): void {
            if (!Array.isArray(selected)) {
                console.error('Expected selected to be an array, but got:', selected);

                return;
            }

            const currentChecked = [...store.selected()];
            if (selected.includes(item.key)) {
                currentChecked.push({ key: item.key, value: item.categoryName });
            } else {
                const index = currentChecked.findIndex((entry) => entry.key === item.key);
                if (index > -1) {
                    currentChecked.splice(index, 1);
                }
            }

            patchState(store, {
                selected: currentChecked
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

        /**
         * Clears all categories from the store, effectively resetting state related to categories and their parent paths.
         */
        clean() {
            patchState(store, { categories: [], parentPath: [] });
        }
    }))
);
