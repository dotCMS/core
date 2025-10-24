import {
    patchState,
    signalStoreFeature,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { MenuItem } from 'primeng/api';

/**
 * State interface for the Breadcrumb feature.
 * Contains the breadcrumb navigation items.
 */
export interface BreadcrumbState {
    /**
     * Array of breadcrumb items to display in the navigation.
     *
     * Each item follows PrimeNG's MenuItem interface. Set to an empty array when no breadcrumbs are available.
     */
    breadcrumbs: MenuItem[];
}

/**
 * Initial state for the Breadcrumb feature.
 */
const initialBreadcrumbState: BreadcrumbState = {
    breadcrumbs: []
};

/**
 * Custom Store Feature for managing breadcrumb navigation state.
 *
 * This feature provides state management for breadcrumb navigation items
 * using PrimeNG's MenuItem interface. It serves as the single source of truth
 * for the DotCrumbtrailComponent.
 *
 * ## Features
 * - Manages breadcrumb navigation items as MenuItem[]
 * - Compatible with PrimeNG Breadcrumb component
 * - Provides methods to set, append, and clear breadcrumbs
 * - Full TypeScript support with strict typing
 *
 */
export function withBreadcrumbs() {
    return signalStoreFeature(
        withState(initialBreadcrumbState),
        withComputed(({ breadcrumbs }) => ({
            /**
             * Computed signal that returns the number of breadcrumb items.
             *
             * @returns The count of breadcrumb items
             */
            breadcrumbCount: computed(() => breadcrumbs().length),

            /**
             * Computed signal that indicates if there are any breadcrumbs.
             *
             * @returns `true` if there are breadcrumbs, `false` if empty
             */
            hasBreadcrumbs: computed(() => breadcrumbs().length > 0),

            /**
             * Computed signal returning the last breadcrumb item.
             * Returns null if there are no breadcrumbs.
             */
            lastBreadcrumb: computed(() => breadcrumbs()[breadcrumbs().length - 1]),
            /**
             * Computed signal returning the label of the last breadcrumb item.
             * Returns null if there are no breadcrumbs.
             */
            selectLastBreadcrumbLabel: computed(() => {
                const crumbs = breadcrumbs();
                const last = crumbs.length ? crumbs[crumbs.length - 1] : null;
                return last?.label ?? null;
            })
        })),
        withMethods((store) => ({
            /**
             * Sets the breadcrumb items, replacing any existing breadcrumbs.
             *
             * @param breadcrumbs - Array of MenuItem objects to set as breadcrumbs
             */
            setBreadcrumbs: (breadcrumbs: MenuItem[]) => {
                patchState(store, { breadcrumbs });
            },

            /**
             * Appends a single breadcrumb item to the end of the current breadcrumbs.
             *
             * @param crumb - MenuItem object to append to the breadcrumbs
             */
            appendCrumb: (crumb: MenuItem) => {
                const currentBreadcrumbs = store.breadcrumbs();
                patchState(store, { breadcrumbs: [...currentBreadcrumbs, crumb] });
            },
            /**
             * Truncates the breadcrumbs to the existing index.
             *
             * @param existingIndex - Index of the existing breadcrumb
             */
            truncateBreadcrumbs: (existingIndex: number) => {
                const currentBreadcrumbs = store.breadcrumbs();
                patchState(store, { breadcrumbs: currentBreadcrumbs.slice(0, existingIndex + 1) });
            },
            /**
             * Sets the last breadcrumb item, replacing the last breadcrumb item.
             *
             * @param crumb - MenuItem object to set as the last breadcrumb
             */
            setLastBreadcrumb: (crumb: MenuItem) => {
                const currentBreadcrumbs = store.breadcrumbs();
                patchState(store, { breadcrumbs: [...currentBreadcrumbs.slice(0, -1), crumb] });
            },
            /**
             * Adds a new breadcrumb item to the breadcrumbs.
             *
             * @param item - MenuItem object to add to the breadcrumbs
             */
            addNewBreadcrumb: (item: MenuItem) => {
                const contentEditRegex = /\/content\/.+/;
                const url = item?.url?.replace('/dotAdmin/#', '') || '';
                const lastBreadcrumb = store.lastBreadcrumb();
                const lastBreadcrumbUrl = lastBreadcrumb?.url?.replace('/dotAdmin/#', '') || '';
                const currentBreadcrumbs = store.breadcrumbs();

                if (contentEditRegex.test(url) && contentEditRegex.test(lastBreadcrumbUrl)) {
                    patchState(store, { breadcrumbs: [...currentBreadcrumbs.slice(0, -1), item] });
                } else {
                    patchState(store, { breadcrumbs: [...currentBreadcrumbs, item] });
                }
            },
            loadBreadcrumbs: () => {
                const breadcrumbs = JSON.parse(sessionStorage.getItem('breadcrumbs') || '[]');
                patchState(store, { breadcrumbs });
            }
        })),
        withHooks({
            onInit(store) {
                // Load current site on store initialization
                // System configuration is automatically loaded by withSystem feature
                store.loadBreadcrumbs();
            }
        })
    );
}
