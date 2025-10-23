import {
    patchState,
    signalStoreFeature,
    withComputed,
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
                patchState(store, {
                    breadcrumbs: [...currentBreadcrumbs, crumb]
                });
            },

            /**
             * Clears all breadcrumb items, resetting to an empty array.
             */
            clearBreadcrumbs: () => {
                patchState(store, { breadcrumbs: [] });
            }
        })),
        withComputed(({ breadcrumbs }) => ({
            /**
             * Computed signal that returns the current breadcrumb items.
             *
             * @returns Array of MenuItem objects representing the breadcrumbs
             */
            selectBreadcrumbs: computed(() => breadcrumbs()),

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
            hasBreadcrumbs: computed(() => breadcrumbs().length > 0)
        }))
    );
}
