import { patchState, signalStoreFeature, withHooks, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { merge, Observable, pipe } from 'rxjs';

import { inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import {
    DotEventsSocket,
    DotSystemEventType,
    SITE_REFRESH_EVENTS,
    WebSocketStatus
} from '@dotcms/data-access';
import { DotcmsEventsService } from '@dotcms/dotcms-js';
import { DotSite } from '@dotcms/dotcms-models';

export interface WebSocketState {
    wsStatus: WebSocketStatus;
}

const initialWebSocketState: WebSocketState = {
    wsStatus: 'connecting'
};

/**
 * Store feature that manages the WebSocket connection lifecycle and exposes
 * its status as a signal.
 *
 * - Starts the connection automatically in `onInit`
 * - Destroys the connection in `onDestroy`
 * - Keeps `wsStatus` in sync via `status$()`
 *
 * Consumers read `globalStore.wsStatus()` — the UI indicator only shows when not 'connected'.
 */
export function withWebSocket() {
    return signalStoreFeature(
        withState(initialWebSocketState),
        withMethods(
            (
                store,
                eventsSocket = inject(DotEventsSocket),
                dotcmsEventsService: DotcmsEventsService = inject(DotcmsEventsService)
            ) => ({
                startConnection: rxMethod<void>(pipe(switchMap(() => eventsSocket.connect()))),
                trackStatus: rxMethod<void>(
                    pipe(
                        switchMap(() =>
                            eventsSocket
                                .status$()
                                .pipe(tap((wsStatus) => patchState(store, { wsStatus })))
                        )
                    )
                ),
                /** Pipes all raw WS messages into the legacy DotcmsEventsService Subject bus. */
                feedLegacyEventBus: rxMethod<void>(
                    pipe(
                        switchMap(() =>
                            eventsSocket
                                .messages()
                                .pipe(
                                    tap(({ event, payload }) =>
                                        dotcmsEventsService.feedMessage(event, payload?.data)
                                    )
                                )
                        )
                    )
                ),
                destroySocket: () => eventsSocket.destroy(),
                /**
                 * Observable that emits when the backend sends UPDATE_PORTLET_LAYOUTS.
                 * Use this instead of the deprecated DotcmsEventsService.
                 */
                portletLayoutUpdated$: (): Observable<void> =>
                    eventsSocket.on<void>(DotSystemEventType.UPDATE_PORTLET_LAYOUTS),

                /**
                 * Observable that emits whenever a site is created, published,
                 * archived, unarchived, or updated. Use this to refresh site lists.
                 */
                siteEvents$: (): Observable<void> =>
                    merge(...SITE_REFRESH_EVENTS.map((event) => eventsSocket.on<void>(event))),

                /**
                 * Observable that emits the new site when another user/tab switches
                 * the current site (SWITCH_SITE event). The payload contains the full
                 * DotSite object — no extra HTTP call needed.
                 */
                switchSiteEvent$: (): Observable<DotSite> =>
                    eventsSocket.on<DotSite>(DotSystemEventType.SWITCH_SITE)
            })
        ),
        withHooks({
            onInit(store) {
                store.startConnection();
                store.trackStatus();
                store.feedLegacyEventBus();
            },
            onDestroy(store) {
                store.destroySocket();
            }
        })
    );
}
