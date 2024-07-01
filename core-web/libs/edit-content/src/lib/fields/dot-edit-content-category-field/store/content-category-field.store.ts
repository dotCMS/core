import { tapResponse } from '@ngrx/component-store';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { filter, switchMap, tap } from 'rxjs/operators';

import { ComponentStatus, DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import {
    DotCategoryFieldCategory,
    DotCategoryFieldItem,
    DotKeyValueObj
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
    rootCategoryInode: string;
    categories: DotCategoryFieldCategory[][];
    categoriesValue: DotKeyValueObj[];
    parentPath: string[];
    state: ComponentStatus;
};

export const initialState: CategoryFieldState = {
    rootCategoryInode: '',
    categories: [],
    categoriesValue: [],
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
    withComputed(({ categories, categoriesValue, parentPath }) => ({
        /**
         * Current selected items (key) from the contentlet
         */
        selectedCategories: computed(() => categoriesValue().map((item) => item.key)),
        /**
         * Categories for render with added properties
         */
        categoryList: computed(() =>
            categories().map((column) => addMetadata(column, parentPath()))
        ),

        hasSelectedCategories: computed(() => {
            return !!categoriesValue().map((item) => item.key).length;
        })
    })),
    withMethods((store, categoryService = inject(CategoriesService)) => ({
        /**
         * Sets a given iNode as the main parent and loads selected categories into the store.
         */
        load({ variable, values }: DotCMSContentTypeField, contentlet: DotCMSContentlet): void {
            const categoriesValue = getSelectedCategories(variable, contentlet);
            patchState(store, { rootCategoryInode: values, categoriesValue });
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
