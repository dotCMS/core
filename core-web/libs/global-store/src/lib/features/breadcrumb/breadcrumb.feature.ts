import {
    patchState,
    signalStoreFeature,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed, effect } from '@angular/core';

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

const urlsRegex = {
    content: {
        regex: /\/content\/.+/,
        url: '/dotAdmin/#/content'
    },
    editPage: {
        regex: /\/content\/.+/,
        url: '/dotAdmin/#/edit-page/content'
    }
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
        withComputed(({ breadcrumbs }) => {
            const breadcrumbCount = computed(() => breadcrumbs().length);
            const hasBreadcrumbs = computed(() => breadcrumbs().length > 0);
            const lastBreadcrumb = computed(() => {
                const crumbs = breadcrumbs();
                const last = crumbs.length ? crumbs[crumbs.length - 1] : null;
                return last;
            });
            const selectLastBreadcrumbLabel = computed(() => lastBreadcrumb()?.label ?? null);
            return {
                /**
                 * Computed signal that returns the number of breadcrumb items.
                 *
                 * @returns The count of breadcrumb items
                 */
                breadcrumbCount,

                /**
                 * Computed signal that indicates if there are any breadcrumbs.
                 *
                 * @returns `true` if there are breadcrumbs, `false` if empty
                 */
                hasBreadcrumbs,

                /**
                 * Computed signal returning the last breadcrumb item.
                 * Returns null if there are no breadcrumbs.
                 */
                lastBreadcrumb,
                /**
                 * Computed signal returning the label of the last breadcrumb item.
                 * Returns null if there are no breadcrumbs.
                 */
                selectLastBreadcrumbLabel
            };
        }),
        withMethods((store) => {
            const setBreadcrumbs = (breadcrumbs: MenuItem[]) => {
                patchState(store, { breadcrumbs });
            };

            const appendCrumb = (crumb: MenuItem) => {
                const currentBreadcrumbs = store.breadcrumbs();
                patchState(store, { breadcrumbs: [...currentBreadcrumbs, crumb] });
            };

            const setLastBreadcrumb = (crumb: MenuItem) => {
                const currentBreadcrumbs = store.breadcrumbs();
                patchState(store, { breadcrumbs: [...currentBreadcrumbs.slice(0, -1), crumb] });
            };

            const truncateBreadcrumbs = (existingIndex: number) => {
                const currentBreadcrumbs = store.breadcrumbs();
                patchState(store, { breadcrumbs: currentBreadcrumbs.slice(0, existingIndex + 1) });
            };

            const addNewBreadcrumb = (item: MenuItem) => {
                const contentEditRegex = /\/content\/.+/;
                const url = item?.url?.replace('/dotAdmin/#', '') || '';
                const lastBreadcrumb = store.lastBreadcrumb();
                const lastBreadcrumbUrl = lastBreadcrumb?.url?.replace('/dotAdmin/#', '') || '';

                const isSameUrl = url === lastBreadcrumbUrl;

                if (isSameUrl) {
                    return;
                }

                if (contentEditRegex.test(url) && contentEditRegex.test(lastBreadcrumbUrl)) {
                    setLastBreadcrumb(item);
                } else {
                    appendCrumb(item);
                }
            };

            const loadBreadcrumbs = () => {
                const breadcrumbs = JSON.parse(sessionStorage.getItem('breadcrumbs') || '[]');
                patchState(store, { breadcrumbs });
            };

            const clearBreadcrumbs = () => {
                patchState(store, { breadcrumbs: [] });
            };

            return {
                setBreadcrumbs,
                appendCrumb,
                truncateBreadcrumbs,
                setLastBreadcrumb,
                addNewBreadcrumb,
                loadBreadcrumbs,
                clearBreadcrumbs
            };
        }),
        withHooks({
            onInit(store) {
                // Load current site on store initialization
                // System configuration is automatically loaded by withSystem feature
                store.loadBreadcrumbs();

                // Persist breadcrumbs to sessionStorage whenever they change
                const BREADCRUMBS_SESSION_KEY = 'breadcrumbs';

                effect(() => {
                    const breadcrumbs = store.breadcrumbs();
                    sessionStorage.setItem(BREADCRUMBS_SESSION_KEY, JSON.stringify(breadcrumbs));
                });
            }
        })
    );
}
