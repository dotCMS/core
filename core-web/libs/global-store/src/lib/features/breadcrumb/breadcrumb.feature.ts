import {
    patchState,
    signalStoreFeature,
    withComputed,
    withHooks,
    withMethods,
    withProps,
    withState
} from '@ngrx/signals';

import { computed, effect, inject, Signal } from '@angular/core';
import { Event, NavigationEnd, Router } from '@angular/router';

import { MenuItem } from 'primeng/api';

import { filter, map } from 'rxjs/operators';

import { DotMenuItem } from '@dotcms/dotcms-models';

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
 * Session storage key for persisting breadcrumbs
 */
const BREADCRUMBS_SESSION_KEY = 'breadcrumbs';

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

export function withBreadcrumbs(menuItems: Signal<DotMenuItem[]>) {
    return signalStoreFeature(
        withState(initialBreadcrumbState),
        withProps(() => ({
            router: inject(Router)
        })),
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
                const breadcrumbs = JSON.parse(
                    sessionStorage.getItem(BREADCRUMBS_SESSION_KEY) || '[]'
                );
                patchState(store, { breadcrumbs });
            };

            const clearBreadcrumbs = () => {
                patchState(store, { breadcrumbs: [] });
            };

            const listenToRouterEvents = () => {
                store.router.events
                    .pipe(
                        filter((event: Event) => event instanceof NavigationEnd),
                        map((event: NavigationEnd) => event.urlAfterRedirects)
                    )
                    .subscribe((url) => {
                        const menu = menuItems();
                        const newUrl = `/dotAdmin/#${url}`;
                        const breadcrumbs = store.breadcrumbs();
                        const existingIndex = breadcrumbs.findIndex(
                            (crumb) => crumb.url === newUrl
                        );

                        if (existingIndex > -1) {
                            truncateBreadcrumbs(existingIndex);
                        } else {
                            const item = menu.find((item) => item.menuLink === url);
                            if (item) {
                                setBreadcrumbs([
                                    {
                                        label: 'Home',
                                        disabled: true
                                    },
                                    {
                                        label: item.labelParent,
                                        disabled: true
                                    },
                                    {
                                        label: item.label,
                                        target: '_self',
                                        url: newUrl
                                    }
                                ]);
                            } else {
                                if (url.includes('/content?filter=')) {
                                    const filter = url.split('/content?filter=')[1];
                                    addNewBreadcrumb({
                                        label: filter,
                                        target: '_self',
                                        url: newUrl
                                    });
                                }
                            }
                        }
                    });
            };
            return {
                setBreadcrumbs,
                appendCrumb,
                truncateBreadcrumbs,
                setLastBreadcrumb,
                addNewBreadcrumb,
                loadBreadcrumbs,
                clearBreadcrumbs,
                _listenToRouterEvents: listenToRouterEvents
            };
        }),
        withHooks({
            onInit(store) {
                // Load current site on store initialization
                // System configuration is automatically loaded by withSystem feature

                store._listenToRouterEvents();
                store.loadBreadcrumbs();
                // Persist breadcrumbs to sessionStorage whenever they change
                effect(() => {
                    const breadcrumbs = store.breadcrumbs();
                    sessionStorage.setItem(BREADCRUMBS_SESSION_KEY, JSON.stringify(breadcrumbs));
                });
            }
        })
    );
}
