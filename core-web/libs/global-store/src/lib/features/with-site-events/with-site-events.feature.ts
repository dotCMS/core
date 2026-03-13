import { patchState, signalStoreFeature, withHooks, withMethods, withState } from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { DestroyRef, Signal, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { switchMap } from 'rxjs/operators';

import { DotSiteService } from '@dotcms/data-access';
import { DotcmsEventsService } from '@dotcms/dotcms-js';
import { DotSite } from '@dotcms/dotcms-models';

/**
 * Site WebSocket events that trigger a list refresh in the site dropdown.
 * These events indicate that the available sites list has changed
 * (new site created, published, updated, unarchived, or permissions changed).
 */
const LIST_REFRESH_EVENTS = [
    'SAVE_SITE',
    'PUBLISH_SITE',
    'UN_ARCHIVE_SITE',
    'UPDATE_SITE',
    'UPDATE_SITE_PERMISSIONS'
] as const;

/**
 * State for the SiteEvents feature.
 */
export interface SiteEventsState {
    /**
     * Opaque version counter for the sites list.
     * Incremented whenever a WebSocket event signals that the available sites list changed.
     * Consumers (e.g., the dot-site dropdown) react to this changing — the actual value is irrelevant.
     */
    siteListVersion: number;
}

const initialSiteEventsState: SiteEventsState = {
    siteListVersion: 0
};

/**
 * GlobalStore feature that centralizes all site-related WebSocket event handling.
 *
 * Subscribes to backend events via `DotcmsEventsService` and keeps the global site state
 * in sync without portlets needing to manage their own WebSocket subscriptions.
 *
 * ## Events handled
 * - `SWITCH_SITE` — another user/tab switched the site; update `siteDetails`
 * - `ARCHIVE_SITE` — a site was archived; if it's the current site, auto-switch to default
 * - `SAVE_SITE`, `PUBLISH_SITE`, `UN_ARCHIVE_SITE`, `UPDATE_SITE`, `UPDATE_SITE_PERMISSIONS`
 *   — the available sites list changed; increment `siteListVersion` to signal UI refresh
 *
 * ## Portlet usage
 * Portlets that need to react to the active site changing should inject `GlobalStore`
 * and use `globalStore.currentSiteId()` in an `effect()` or `toObservable()`. They do NOT
 * need to subscribe to WebSocket events directly.
 *
 * @param siteDetails - Signal of the current site (from GlobalState)
 * @param setCurrentSite - Callback to update the current site in GlobalState
 */
export function withSiteEvents(
    siteDetails: Signal<DotSite | null>,
    setCurrentSite: (site: DotSite) => void
) {
    return signalStoreFeature(
        withState(initialSiteEventsState),
        withMethods(
            (
                store,
                eventsService = inject(DotcmsEventsService),
                siteService = inject(DotSiteService),
                destroyRef = inject(DestroyRef)
            ) => ({
                setupSiteEventListeners(): void {
                    // SWITCH_SITE: another user/tab switched — update current site directly from payload
                    eventsService
                        .subscribeTo<DotSite>('SWITCH_SITE')
                        .pipe(takeUntilDestroyed(destroyRef))
                        .subscribe((site) => setCurrentSite(site));

                    // ARCHIVE_SITE: if the archived site is the current one, switch to default
                    eventsService
                        .subscribeTo<DotSite>('ARCHIVE_SITE')
                        .pipe(
                            switchMap((data) => {
                                const isCurrent = data.identifier === siteDetails()?.identifier;

                                if (!isCurrent) {
                                    patchState(store, {
                                        siteListVersion: store.siteListVersion() + 1
                                    });
                                    return EMPTY;
                                }

                                return siteService.switchSite(null);
                            }),
                            takeUntilDestroyed(destroyRef)
                        )
                        .subscribe((defaultSite) => {
                            setCurrentSite(defaultSite);
                            patchState(store, {
                                siteListVersion: store.siteListVersion() + 1
                            });
                        });

                    // List-refresh events: sites catalog changed — signal the dropdown to reload
                    eventsService
                        .subscribeToEvents<DotSite>([...LIST_REFRESH_EVENTS])
                        .pipe(takeUntilDestroyed(destroyRef))
                        .subscribe(() => {
                            patchState(store, {
                                siteListVersion: store.siteListVersion() + 1
                            });
                        });
                }
            })
        ),
        withHooks({
            onInit(store) {
                store.setupSiteEventListeners();
            }
        })
    );
}
